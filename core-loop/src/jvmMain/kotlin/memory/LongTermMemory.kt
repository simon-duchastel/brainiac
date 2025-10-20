package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.xml.xml

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallLongTermMemory(
    name: String? = null,
    longTermMemoryRepository: LongTermMemoryRepository,
): AIAgentNodeDelegate<LongTermMemoryRequest, LongTermMemory> = node(name) { request ->
    // Generate mind map of LTM directory structure
    val mindMap = longTermMemoryRepository.generateMindMap()

    // If no LTM files exist, return empty memory
    if (mindMap == "<ltm-directory />") {
        return@node LongTermMemory("")
    }

    // Ask LLM which memories would be helpful
    val relevantFiles = llm.writeSession {
        updatePrompt {
            system {
                xml {
                    tag("instruction") {
                        +"""
                        Given the following user request and a mind map of available long-term memory files,
                        identify which memory files would be helpful to retrieve, if any.

                        Respond with a list of file paths (relative to the long-term-memory directory),
                        one per line, with no additional text or formatting. If no files would be helpful,
                        respond with "NONE".
                        """.trimIndent()
                    }
                    tag("user-request") {
                        +request.query
                    }
                    tag("mind-map") {
                        +mindMap
                    }
                }
            }
        }

        val response = requestLLMWithoutTools()
        response.content.trim()
    }

    // Parse file paths from response
    val filePaths = if (relevantFiles == "NONE") {
        emptyList()
    } else {
        relevantFiles.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    // Read and concatenate relevant files
    val memories = filePaths.mapNotNull { filePath ->
        val content = longTermMemoryRepository.read(filePath)
        if (content.isNotEmpty()) {
            "=== $filePath ===\n$content"
        } else {
            null
        }
    }.joinToString("\n\n")

    LongTermMemory(memories)
}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateLongTermMemory(
    name: String? = null,
    tokenThreshold: Int = 10_000,
    longTermMemoryRepository: LongTermMemoryRepository,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentNodeDelegate<Pair<ShortTermMemory, T>, T> = node(name) { (shortTermMemory, input) ->
    // We need to put the short term memory into a fake prompt in order to count its tokens.
    // Currently the tokenizer can only tokenize messages or prompts, not arbitrary strings (even though
    // under the hood it just parses out the raw string).
    val shortTermMemoryForTokenization = prompt("short_term_memory") { shortTermMemory.memory }
    val currentTokens = tokenizer.tokenCountFor(shortTermMemoryForTokenization)

    if (currentTokens > tokenThreshold) {
        llm.writeSession {
            // Ask LLM to identify important memories to promote to LTM
            updatePrompt {
                system {
                    xml {
                        tag("instruction") {
                            +"""
                            Please analyze the short-term memory below and identify important information
                            that should be saved to long-term memory.

                            For each important memory:
                            1. Provide a filename (use descriptive names like "project_goals.txt", "key_facts.txt", etc.)
                            2. Provide the content to save

                            Format your response as follows:
                            FILE: filename.txt
                            CONTENT:
                            [content here]

                            FILE: another_file.txt
                            CONTENT:
                            [content here]

                            If no memories need to be saved, respond with "NONE".
                            """.trimIndent()
                        }
                        tag("short-term-memory") {
                            +shortTermMemory.memory
                        }
                    }
                }
            }

            val promotionResponse = requestLLMWithoutTools()
            val promotionContent = promotionResponse.content.trim()

            // Parse and save memories to LTM
            if (promotionContent != "NONE") {
                val fileBlocks = promotionContent.split("FILE:").filter { it.isNotBlank() }

                for (block in fileBlocks) {
                    val lines = block.lines()
                    val filename = lines.firstOrNull()?.substringBefore("CONTENT:")?.trim()
                    val contentStartIndex = block.indexOf("CONTENT:")

                    if (filename != null && contentStartIndex != -1) {
                        val content = block.substring(contentStartIndex + "CONTENT:".length).trim()
                        if (content.isNotEmpty()) {
                            longTermMemoryRepository.write(filename, content)
                        }
                    }
                }
            }

            // Ask LLM to clean STM by removing promoted/unneeded information
            rewritePrompt {
                prompt("clean_stm_prompt") {
                    system {
                        xml {
                            tag("instruction") {
                                +"""
                                The following short-term memory has been analyzed, and important information
                                has been promoted to long-term memory.

                                Please create a cleaned, condensed version of the short-term memory that:
                                1. Removes information that was promoted to long-term memory
                                2. Removes any unneeded or redundant information
                                3. Retains only recent, actionable context that is still relevant

                                Provide a clear, concise summary.
                                """.trimIndent()
                            }
                            tag("short-term-memory") {
                                +shortTermMemory.memory
                            }
                        }
                    }
                }
            }

            val cleanedResponse = requestLLMWithoutTools()
            val cleanedMemory = cleanedResponse.content.trim()

            // Write cleaned STM back to repository
            shortTermMemoryRepository.write(cleanedMemory)

            // Replace working memory with cleaned STM
            rewritePrompt {
                prompt("cleaned_short_term_memory") {
                    system {
                        +cleanedMemory
                    }
                }
            }
        }
    }

    input
}

data class LongTermMemoryRequest(
    val query: String,
)

data class LongTermMemory(
    val memory: String
)
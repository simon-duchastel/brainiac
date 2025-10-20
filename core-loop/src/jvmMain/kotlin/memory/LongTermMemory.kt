package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.text.TextContentBuilderBase
import ai.koog.prompt.xml.xml

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallLongTermMemory(
    name: String? = null,
    longTermMemoryRepository: LongTermMemoryRepository,
): AIAgentNodeDelegate<LongTermMemoryRequest, LongTermMemory> = node(name) { request ->
    val filePaths = llm.writeSession {
        updatePrompt {
            system {
                xml {
                    tag("instruction") {
                        +"""
                        Given the following user request and a mind map of available long-term memory files,
                        identify which memory files would be helpful to retrieve, if any.

                        Return a list of file paths (relative to the long-term-memory directory) in structured format.
                        If no files would be helpful, return an empty list.
                        
                        Don't return memories just for the sake of it - only return memories that would be genuinely helpful. Be thorough yet selective!
                        """.trimIndent()
                    }
                    longTermMemoryRepository.generateMindMap()
                    tag("user-request") {
                        +request.query
                    }
                }
            }
        }

        requestLLMStructured<List<String>>().getOrNull()!!.structure
    }

    // Read and concatenate relevant files
    val memories = filePaths.map { memoryPath ->
        longTermMemoryRepository.getLongTermMemory(memoryPath)
    }

    LongTermMemory(memories)
}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateLongTermMemory(
    name: String? = null,
    tokenThreshold: Int = 10_000,
    longTermMemoryRepository: LongTermMemoryRepository,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentNodeDelegate<Pair<ShortTermMemory, T>, T> = node(name) { (shortTermMemory, input) ->
    val shortTermMemoryForTokenization = prompt("short_term_memory") {
        system {
            shortTermMemory.asXmlRepresentation()
        }
    }
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

                            If no memories need to be saved, respond with "NONE".
                            
                            Don't save memories just for the sake of it - only save memories that would be genuinely helpful for long-term storage. Consider memories which are either:
                            1. useful pieces of information, or
                            2. episodic
                            """.trimIndent()
                        }
                        tag("short-term-memory") {
                            +shortTermMemory.memory
                        }
                    }
                }
            }

            val promotionResponse = requestLLMWithoutTools()

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

            val paths = requestLLMStructured<List<String>>().getOrNull()!!.structure

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
    val memories: List<String>
) {
    context(textBuilder: TextContentBuilderBase<*>)
    fun asXmlRepresentation(indented: Boolean = true) = textBuilder.xml(indented) {
        tag("long-term-memory") {
            memories.forEach {
                tag("memory") {
                    +it
                }
            }
        }
    }
}
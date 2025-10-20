package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.text.TextContentBuilderBase
import ai.koog.prompt.xml.xml
import com.duchastel.simon.brainiac.core.process.context.BrainiacContext
import com.duchastel.simon.brainiac.core.process.util.withModel
import kotlinx.serialization.Serializable

@AIAgentBuilderDslMarker
context(brainiacContext: BrainiacContext)
fun AIAgentSubgraphBuilderBase<*, *>.recallLongTermMemory(
    name: String? = null,
    longTermMemoryRepository: LongTermMemoryRepository,
): AIAgentNodeDelegate<LongTermMemoryRequest, LongTermMemory> = node(name) { request ->
    val originalPrompt = llm.prompt
    val filePaths = llm.writeSession {
        rewritePrompt {
            prompt("recall_long_term_memory") {
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

                        longTermMemoryRepository.generateXmlMindMap()

                        tag("user-request") {
                            +request.query
                        }
                    }
                }
            }
        }

        val memoryPaths = withModel(brainiacContext.mediumThoughtModel) {
            requestLLMStructured<List<String>>().getOrNull()!!.structure
        }
        prompt = originalPrompt

        memoryPaths
    }

    // Read and concatenate relevant files
    val memories = filePaths.map { memoryPath ->
        longTermMemoryRepository.getLongTermMemory(memoryPath)
    }

    LongTermMemory(memories)
}

@Serializable
data class MemoryPromotion(
    val filename: String,
    val content: String
)

@AIAgentBuilderDslMarker
context(brainiacContext: BrainiacContext)
inline fun <reified T: Any> AIAgentSubgraphBuilderBase<*, *>.updateLongTermMemory(
    name: String? = null,
    tokenThreshold: Int = 10_000,
    longTermMemoryRepository: LongTermMemoryRepository,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentSubgraphDelegate<Pair<ShortTermMemory, T>, T> = subgraph(name) {
    val initialInputKey = createStorageKey<T>("${name}_initial_input")
    val initialPromptKey = createStorageKey<Prompt>("${name}_initial_prompt")
    val shortTermMemoryKey = createStorageKey<ShortTermMemory>("${name}initial_stm")

    val setup by node<Pair<ShortTermMemory, T>, Unit>("${name}_setup") { (shortTermMemory, input) ->
        storage.set(initialInputKey, input)
        storage.set(initialPromptKey, llm.prompt)
        storage.set(shortTermMemoryKey, shortTermMemory)
    }

    val cleanup by node<Unit, T>("${name}_cleanup") {
        storage.getValue(initialInputKey)
    }

    val evaluateNeedForLongTermMemoryUpdate by node<Unit, Boolean>("${name}_evaluate_tokens") {
        val stm = storage.getValue(shortTermMemoryKey)
        val stmPrompt = prompt("tokenize_stm") {
            system {
                stm.asXmlRepresentation()
            }
        }
        tokenizer.tokenCountFor(stmPrompt) >= tokenThreshold
    }

    val identifyPromotionCandidates by node<Unit, List<MemoryPromotion>>("${name}_identify_promotions") {
        val stm = storage.getValue(shortTermMemoryKey)

        llm.writeSession {
            rewritePrompt {
                prompt("identify_promotions") {
                    system {
                        +"""
                        Please analyze the short-term memory below and identify important information
                        that should be saved to long-term memory.

                        Return a list of memory promotions with filenames and content.
                        If no memories need to be saved, return an empty list.

                        Don't save memories just for the sake of it - only save memories that would be genuinely helpful for long-term storage. Consider memories which are either:
                        1. useful pieces of information, or
                        2. episodic (events which happened that were interesting or notable for summarization and storage)
                        """.trimIndent()

                        stm.asXmlRepresentation()
                    }
                }
            }

            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<List<MemoryPromotion>>().getOrNull()!!.structure
            }
        }
    }

    val promoteToLongTermMemory by node<List<MemoryPromotion>, List<MemoryPromotion>>("${name}_promote") { promotions ->
        promotions.forEach { promotion ->
            longTermMemoryRepository.writeLongTermMemory(promotion.filename, promotion.content)
        }
        promotions
    }

    val cleanShortTermMemory by node<List<MemoryPromotion>, ShortTermMemory>("${name}_clean_stm") { promotions ->
        val stm = storage.getValue(shortTermMemoryKey)

        llm.writeSession {
            rewritePrompt {
                prompt("clean_short_term_memory") {
                    system {
                        +"""
                        The following short-term memory has been analyzed, and important information
                        has been promoted to long-term memory.

                        Return a cleaned version of the short-term memory that:
                        1. Removes information that was promoted to long-term memory
                        2. Removes any unneeded or redundant information
                        3. Retains only recent, actionable context that is still relevant
                        """.trimIndent()

                        stm.asXmlRepresentation()
                    }
                }
            }

            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<ShortTermMemory>().getOrNull()!!.structure
            }
        }
    }

    val updateRepositoryAndPrompt by node<ShortTermMemory, Unit>("${name}_update_prompt") { cleanedStm ->
        shortTermMemoryRepository.updateShortTermMemory(cleanedStm)

        val originalPrompt = storage.getValue(initialPromptKey)
        llm.writeSession {
            rewritePrompt { originalPrompt }
            updatePrompt {
                system { summarizeWorkingMemory(cleanedStm) }
            }
            val summaryMessage = requestLLM()
            rewriteWorkingMemory(cleanedStm, LongTermMemory(emptyList()))
            prompt = prompt.withMessages { currentMessages -> currentMessages + summaryMessage }
        }
    }

    edge(nodeStart forwardTo setup)
    edge(setup forwardTo evaluateNeedForLongTermMemoryUpdate)
    edge(
        evaluateNeedForLongTermMemoryUpdate forwardTo identifyPromotionCandidates
        onCondition { it }
        transformed { }
    )
    edge(
        evaluateNeedForLongTermMemoryUpdate forwardTo cleanup
        onCondition { !it }
        transformed { }
    )
    identifyPromotionCandidates then promoteToLongTermMemory then cleanShortTermMemory then updateRepositoryAndPrompt then cleanup
    cleanup then nodeFinish
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
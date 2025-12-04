package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
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
import com.duchastel.simon.brainiac.core.process.prompt.Prompts
import com.duchastel.simon.brainiac.core.process.util.withModel
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@PublishedApi
internal val logger = LoggerFactory.getLogger("LongTermMemory")

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
                            +Prompts.RECALL_LONG_TERM_MEMORY_INSTRUCTION
                        }

                        longTermMemoryRepository.generateXmlMindMap()

                        tag("user-request") {
                            +request.query
                        }
                    }
                }
                user {
                    +Prompts.RECALL_LONG_TERM_MEMORY_USER
                }
            }
        }

        val memoryPaths = withModel(brainiacContext.mediumThoughtModel) {
            requestLLMStructured<MemoryPaths>().fold(
                onSuccess = { response -> response.structure.filePaths },
                onFailure = { error ->
                    logger.warn("Failed to identify relevant memories: {}", error.message)
                    logger.info("Continuing without long-term memory context")
                    emptyList()
                }
            )
        }
        prompt = originalPrompt

        memoryPaths
    }

    // Read and concatenate relevant files
    val memories = filePaths.mapNotNull { memoryPath ->
        try {
            longTermMemoryRepository.getLongTermMemory(memoryPath)
        } catch (e: Exception) {
            logger.warn("Failed to read memory file '{}': {}", memoryPath, e.message)
            null
        }
    }

    LongTermMemory(memories)
}

@Serializable
data class MemoryPromotion(
    val filename: String,
    val content: String,
)

@Serializable
data class MemoryPromotions(
    val promotions: List<MemoryPromotion>,
)

@Serializable
data class MemoryPaths(
    val filePaths: List<String>,
)

@AIAgentBuilderDslMarker
context(brainiacContext: BrainiacContext)
inline fun <reified T: Any> AIAgentSubgraphBuilderBase<*, *>.updateLongTermMemory(
    name: String? = null,
    tokenThreshold: Int = 10_000,
    longTermMemoryRepository: LongTermMemoryRepository,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentSubgraphDelegate<Pair<ShortTermMemory, T>, T> = subgraph(
    name = name,
    toolSelectionStrategy = ToolSelectionStrategy.NONE,
) {
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
                        +Prompts.IDENTIFY_MEMORY_PROMOTIONS

                        stm.asXmlRepresentation()
                    }
                    user {
                        +Prompts.IDENTIFY_MEMORY_PROMOTIONS_USER
                    }
                }
            }

            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<MemoryPromotions>().fold(
                    onSuccess = { response -> response.structure.promotions },
                    onFailure = { error ->
                        logger.warn("Failed to identify memory promotions: {}", error.message)
                        logger.info("Skipping LTM promotion this cycle")
                        emptyList()
                    }
                )
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
                        +Prompts.CLEAN_SHORT_TERM_MEMORY

                        stm.asXmlRepresentation()
                    }
                    user {
                        +Prompts.CLEAN_SHORT_TERM_MEMORY_USER
                    }
                }
            }

            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<ShortTermMemory>().fold(
                    onSuccess = { response -> response.structure },
                    onFailure = { error ->
                        logger.warn("Failed to clean short-term memory: {}", error.message)
                        logger.info("Keeping original short-term memory contents")
                        // Return original STM unchanged
                        stm
                    }
                )
            }
        }
    }

    val updateRepositoryAndPrompt by node<ShortTermMemory, Unit>("${name}_update_prompt") { cleanedStm ->
        shortTermMemoryRepository.updateShortTermMemory(cleanedStm)

        val originalPrompt = storage.getValue(initialPromptKey)
        llm.writeSession {
            rewritePrompt { originalPrompt }
            updatePrompt {
                system { with(Prompts) { summarizeWorkingMemory(cleanedStm) } }
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
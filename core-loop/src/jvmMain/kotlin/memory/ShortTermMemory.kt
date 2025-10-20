package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt
import kotlinx.serialization.Serializable

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallShortTermMemory(
    name: String? = null,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentNodeDelegate<Unit, ShortTermMemory> = node(name) {
    shortTermMemoryRepository.getShortTermMemory()
}

@AIAgentBuilderDslMarker
inline fun <reified T: Any> AIAgentSubgraphBuilderBase<*, *>.updateShortTermMemory(
    name: String? = null,
    tokenThreshold: Int = 50_000,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentSubgraphDelegate<T, T> = subgraph(name) {
    val initialInputKey = createStorageKey<T>("${name}_initial_input")

    val storeIntitalInput by node<T, Unit>("${name}_store_initial_input") { input ->
        storage.set(initialInputKey, input)
    }
    val cleanup by node<Unit, T>("${name}_cleanup") {
        storage.getValue(initialInputKey)
    }
    val evaluateNeedForShortTermMemoryUpdate by node<Unit, Boolean>("${name}_evalute_tokens") {
        tokenizer.tokenCountFor(llm.prompt) >= tokenThreshold
    }

    val updatePrompt by node<Unit, Unit>("${name}_update_prompt") {
        llm.writeSession {
            updatePrompt {
                system {
                    +"""
                    Please synthesize the current working memory into a concise, actionable short-term memory.
                    Focus on:
                    - Key facts and information from the conversation
                    - Current tasks or goals
                    - Important context that should be retained
                    - Action items or next steps

                    Provide a clear, structured summary that will be useful for future interactions.
                    """.trimIndent()
                }
            }

            // Request LLM to synthesize the memory
            val response = requestLLMWithoutTools()
            val synthesizedMemory = response.content

            // Write the synthesized memory to disk as a thought
            val currentMemory = shortTermMemoryRepository.getShortTermMemory()
            val updatedMemory = ShortTermMemory(
                thoughts = listOf(synthesizedMemory),
                goals = currentMemory.goals,
                events = currentMemory.events
            )
            shortTermMemoryRepository.updateShortTermMemory(updatedMemory)

            // Replace working memory with just the summary
            rewritePrompt {
                prompt("short_term_memory_summary") {
                    system {
                        +synthesizedMemory
                    }
                }
            }
        }
    }

    edge(nodeStart forwardTo storeIntitalInput)
    edge(storeIntitalInput forwardTo evaluateNeedForShortTermMemoryUpdate)
    edge(
        evaluateNeedForShortTermMemoryUpdate forwardTo updatePrompt
        onCondition { it }
        transformed { }
    )
    edge(
        evaluateNeedForShortTermMemoryUpdate forwardTo cleanup
                onCondition { !it }
                transformed { }
    )
    edge(updatePrompt forwardTo cleanup)
    edge(cleanup forwardTo nodeFinish)
}

@Serializable
data class ShortTermMemory(
    val thoughts: List<String> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val events: List<String> = emptyList(),
)

@Serializable
data class Goal(
    val description: String,
    val completed: Boolean = false,
)
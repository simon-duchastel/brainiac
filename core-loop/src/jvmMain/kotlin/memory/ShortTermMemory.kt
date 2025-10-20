package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallShortTermMemory(
    name: String? = null,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentNodeDelegate<Unit, ShortTermMemory> = node(name) {
    shortTermMemoryRepository.read()
}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateShortTermMemory(
    name: String? = null,
    tokenThreshold: Long = 50_000,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentNodeDelegate<T, T> = node(name) { input ->
    val currentTokens = tokenizer.tokenCountFor(llm.prompt)

    if (currentTokens >= tokenThreshold) {
        llm.writeSession {
            updatePrompt {
                system {
                    +"""
                    Please synthesize the current context into a concise, actionable short-term memory.
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

            // Write the synthesized memory to disk
            shortTermMemoryRepository.write(synthesizedMemory)

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

    input
}

data class ShortTermMemory(
    val memory: String
)
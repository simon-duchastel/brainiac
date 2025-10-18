@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.duchastel.simon.brainiac.core.process

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.tokenizer.SimpleRegexBasedTokenizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Core agent for the Brainiac AI Memory System.
 *
 * This class wraps the CoreLoop strategy and provides a simple interface
 * for processing user queries through the memory system.
 */
class CoreAgent(
    private val googleApiKey: String,
) {
    /**
     * Runs the agent with the given user query.
     *
     * @param userQuery The user's input query
     * @return The agent's response (currently returns empty string as strategy outputs Unit)
     */
    fun run(userQuery: String): Flow<String> {
        val coreLoopStrategy = CoreLoop.strategy("core-loop")

        return callbackFlow {
            val agent = AIAgent(
                promptExecutor = simpleGoogleAIExecutor(googleApiKey),
                toolRegistry = ToolRegistry.EMPTY,
                strategy = coreLoopStrategy,
                agentConfig = AIAgentConfig(
                    prompt = Prompt.build("brainiac-core") {
                        system(
                            """
                        You are Brainiac, an AI assistant with advanced memory capabilities.
                        You have access to both short-term and long-term memory systems.

                        Your goal is to provide helpful, accurate responses while maintaining
                        and organizing your memory system.
                        """.trimIndent()
                        )
                    },
                    model = GoogleModels.Gemini2_5Pro,
                    maxAgentIterations = Int.MAX_VALUE,
                ),
                installFeatures = {
                    install(MessageTokenizer) {
                        tokenizer = SimpleRegexBasedTokenizer()
                    }
                    install(EventHandler) {
                        onLLMCallCompleted { ctx ->
                            ctx.responses.forEach {
                                when (it) {
                                    is Message.Assistant -> {
                                        trySend("------ Assistant")
                                        trySend(it.content)
                                    }

                                    is Message.Tool.Call -> {
                                        trySend("Calling ${it.tool}")
                                    }
                                }
                            }
                        }
                    }
                }
            )

            agent.run(userQuery)
            close()
        }
    }
}

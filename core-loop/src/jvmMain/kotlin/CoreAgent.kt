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
import com.duchastel.simon.brainiac.core.process.callbacks.AgentEvent
import com.duchastel.simon.brainiac.core.process.callbacks.ToolUse
import kotlinx.coroutines.runBlocking

/**
 * Core agent for the Brainiac AI Memory System.
 *
 * This class wraps the CoreLoop strategy and provides a simple interface
 * for processing user queries through the memory system.
 */
class CoreAgent(
    private val googleApiKey: String,
    private val onEvent: (AgentEvent) -> Unit,
) {
    /**
     * Runs the agent with the given user query.
     *
     * @param userQuery The user's input query
     */
    fun run(userQuery: String) = runBlocking {
        val coreLoopStrategy = CoreLoop.strategy("core-loop")

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
                                    onEvent(AgentEvent.AssistantMessage(it.content))
                                }

                                is Message.Tool.Call -> {
                                    val toolUse = when (it.tool) {
                                        "store_short_term_memory" -> ToolUse.StoreShortTermMemory
                                        "store_long_term_memory" -> ToolUse.StoreLongTermMemory
                                        else -> return@forEach // Skip unknown tools
                                    }
                                    onEvent(AgentEvent.ToolCall(toolUse))
                                }
                            }
                        }
                    }
                }
            }
        )

        agent.run(userQuery)
    }
}

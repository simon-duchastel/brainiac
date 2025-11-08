package com.duchastel.simon.brainiac.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.Executors.promptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.tokenizer.SimpleRegexBasedTokenizer
import kotlinx.coroutines.runBlocking

/**
 * Core agent scaffolding for the Brainiac AI system.
 *
 * This is the main entry point that connects together:
 * - Agent strategies (how the agent behaves)
 * - Tools (what the agent can do)
 * - LLM execution (how the agent thinks)
 * - Event handling (how we observe the agent)
 *
 * This module provides the scaffolding but doesn't depend on specific
 * implementations like core-loop. Strategies and tools are injected from outside.
 */
class CoreAgent(
    private val config: CoreAgentConfig,
) {
    /**
     * Runs the agent with the given user query.
     *
     * @param userQuery The user's input query
     */
    @Suppress("UnstableApiUsage")
    fun run(userQuery: String) = runBlocking {
        val agent = AIAgent(
            promptExecutor = promptExecutor(config.executionClients),
            toolRegistry = config.toolRegistry,
            strategy = config.strategy,
            agentConfig = AIAgentConfig(
                prompt = config.systemPrompt,
                model = config.model,
                maxAgentIterations = config.maxIterations,
            ),
            installFeatures = {
                install(MessageTokenizer) {
                    tokenizer = SimpleRegexBasedTokenizer()
                }
                config.onEventHandler?.let { handler ->
                    install(EventHandler) {
                        onLLMCallCompleted { ctx ->
                            handler(ctx.responses)
                        }
                    }
                }
            }
        )

        agent.run(userQuery)
    }
}

/**
 * Configuration for CoreAgent.
 *
 * @property strategy The agent strategy that defines behavior (e.g., CoreLoop)
 * @property toolRegistry Tools available to the agent
 * @property model The primary LLM model to use
 * @property executionClients Map of LLM providers to their clients
 * @property systemPrompt The system prompt to initialize the agent with
 * @property maxIterations Maximum number of agent iterations (default: no limit)
 * @property onEventHandler Optional handler for agent events (e.g., messages, tool calls)
 */
data class CoreAgentConfig(
    val strategy: AIAgentGraphStrategy<String, Unit>,
    val toolRegistry: ToolRegistry,
    val model: LLModel,
    val executionClients: Map<LLMProvider, LLMClient>,
    val systemPrompt: Prompt,
    val maxIterations: Int = Int.MAX_VALUE,
    val onEventHandler: ((List<Message>) -> Unit)? = null,
)

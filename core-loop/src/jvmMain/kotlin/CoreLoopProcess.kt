@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.duchastel.simon.brainiac.core.process

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.feature.handler.agent.AgentCompletedContext
import ai.koog.agents.core.feature.handler.agent.AgentStartingContext
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.duchastel.simon.brainiac.core.process.graphs.ShortTermMemory
import com.duchastel.simon.brainiac.core.process.graphs.subgraphPromotionProcess

/**
 * Core loop process that handles user prompts with memory retrieval and assembly
 *
 * 1) fetch short term memory file
 * 2) fetch relevant long-term memories
 * 3) create a prompt that incorporates system prompt, user prompt, STM, and LTM
 * 4) run the LLM with thinking, tool-use, and multi-step calls, allowing it to update its STM via
 *    a tool use
 * 8) Once the task is complete, run Reflection, then Promotion, then return the final result to
 *    the user
 * 6) When context gets too large by token count, run Reflection
 * 7) When STM gets too large by token count, run Promotion
 * 9) Periodically (~1/day) run Organization
 *
 * Reflection:
 * 1) Ask sub-model to synthesize all events and key-facts and append them to the STM file
 * 2) Summarize the existing context extremely concisely
 * 3) Overwrite existing context with summary and re-generate prompt with new STM
 *
 * Promotion:
 * 1) Ask sub-model to read through STM and identify any insights or events worthy of being added
 *    to long-term memory - mark these as promotion candidates
 * 2) For each promotion candidate, ask sub-model to search existing LTM to see if anything might
 *    be related and to either append to an existing LTM memory or add a new LTM memory
 *
 * Organization:
 * 1) Ask sub-model to read through all memory accesses and reinforce or de-prioritize memories
 *    based on most important accesses. Condenses and distills information
 */
class CoreLoopProcess() {
    suspend fun processUserPrompt(userPrompt: String): String {
        val agentStrategy = strategy<String, Unit>("core-loop-strategy") {
            val promotionProcess by subgraphPromotionProcess()

            edge(nodeStart forwardTo promotionProcess
                    transformed { input -> ShortTermMemory(userPrompt) }
            )
            edge(promotionProcess forwardTo nodeFinish transformed { })
        }


        val agent = AIAgent(
            promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
            toolRegistry = ToolRegistry.EMPTY,
            strategy = agentStrategy,
            agentConfig = AIAgentConfig(
                prompt = Prompt.build("simple-calculator") {
                    system(
                        """
                       TODO - add a core loop strategy prompt
                        """.trimIndent()
                    )
                },
                model = OpenAIModels.Chat.GPT4o,
                maxAgentIterations = 10
            ),
            installFeatures = {
                install(EventHandler) {
                    onAgentStarting { eventContext: AgentStartingContext<*> ->
                        println("Starting agent: ${eventContext.agent.id}")
                    }
                    onAgentCompleted { eventContext: AgentCompletedContext ->
                        println("Result: ${eventContext.result}")
                    }
                }
            }
        )

        return agent.run(userPrompt).let { "Success" }
    }
}

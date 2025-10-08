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
 * Core loop process that handles user prompts with memory retrieval and assembly.
 *
 * This class implements the main interaction process for the Brainiac AI Memory System,
 * managing the flow of information between working memory, short-term memory (STM),
 * and long-term memory (LTM).
 *
 * ## Core Loop Process
 * 1. Fetch short term memory file
 * 2. Fetch relevant long-term memories
 * 3. Create a prompt that incorporates system prompt, user prompt, STM, and LTM
 * 4. Run the LLM with thinking, tool-use, and multi-step calls, allowing it to update its STM via tool use
 * 5. Once the task is complete, run Reflection, then Promotion, then return the final result to the user
 * 6. When context gets too large by token count, run Reflection
 * 7. When STM gets too large by token count, run Promotion
 * 8. Periodically (~1/day) run Organization
 *
 * ## Reflection Process
 * Captures key information from the Core Loop into Short-Term Memory:
 * 1. Ask sub-model to synthesize all events and key-facts and append them to the STM file
 * 2. Summarize the existing context extremely concisely
 * 3. Overwrite existing context with summary and re-generate prompt with new STM
 *
 * ## Promotion Process
 * Consolidates memories from STM into Long-Term Memory:
 * 1. Ask sub-model to read through STM and identify any insights or events worthy of being added
 *    to long-term memory - mark these as promotion candidates
 * 2. For each promotion candidate, ask sub-model to search existing LTM to see if anything might
 *    be related and to either append to an existing LTM memory or add a new LTM memory
 *
 * ## Organization Process
 * Refactors and evolves the structure of LTM:
 * 1. Ask sub-model to read through all memory accesses and reinforce or de-prioritize memories
 *    based on most important accesses. Condenses and distills information
 *
 * @see com.duchastel.simon.brainiac.core.process.graphs.subgraphPromotionProcess
 */
class CoreLoopProcess() {
    /**
     * Processes a user prompt through the complete memory retrieval and response generation pipeline.
     *
     * This function orchestrates the core loop by:
     * 1. Constructing an AI agent with the promotion process strategy
     * 2. Processing the user prompt through short-term and long-term memory
     * 3. Generating and returning a response
     *
     * @param userPrompt The input prompt from the user to process
     * @return A string containing the agent's response
     */
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

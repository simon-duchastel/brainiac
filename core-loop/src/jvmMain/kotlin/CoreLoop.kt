@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.duchastel.simon.brainiac.core.process

import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentEdgeBuilderIntermediate
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.EdgeTransformationDslMarker
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemory
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRequest
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemory
import com.duchastel.simon.brainiac.core.process.memory.loadInitialWorkingMemory
import com.duchastel.simon.brainiac.core.process.memory.recallLongTermMemory
import com.duchastel.simon.brainiac.core.process.memory.recallShortTermMemory
import com.duchastel.simon.brainiac.core.process.memory.updateShortTermMemory
import kotlin.reflect.typeOf

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
 */
object CoreLoop {
    fun strategy(
        name: String,
    ): AIAgentGraphStrategy<String, Unit> {
        return strategy<String, Unit>(name) {
            val userQueryKey = createStorageKey<String>("${name}_user_prompt")
            val shortTermMemoryKey = createStorageKey<ShortTermMemory>("${name}_short_term_memory")

            val recallShortTermMemory by recallShortTermMemory("recall_short_term_memory")
            val recallLongTermMemory by recallLongTermMemory("recall_short_term_memory")
            val prepareWorkingMemoryInputs by node<LongTermMemory, Pair<ShortTermMemory, LongTermMemory>>("prepare_working_memory_inputs") { longTermMemory ->
                val shortTermMemory = storage.getValue(shortTermMemoryKey)
                shortTermMemory to longTermMemory
            }
            val loadInitialWorkingMemory by loadInitialWorkingMemory(
                name = "load_initial_working_memory",
            )
            val appendUserQuery by node<Unit, Unit>("append_user_query") {
                val userQuery = storage.getValue(userQueryKey)
                llm.writeSession {
                    updatePrompt {
                        user {
                            +userQuery
                        }
                    }
                }
            }
            val requestLlm by node<Unit, Message.Response>("request_llm") {
                llm.writeSession {
                    requestLLM()
                }
            }
            val updateShortTermMemory by updateShortTermMemory<Message.Response>("update_short_term_memory")
            val updateLongTermMemory by updateShortTermMemory<Message.Response>("update_long_term_memory")

            val executeTool by nodeExecuteTool()
            val sendToolResult by nodeLLMSendToolResult()

            edge(
                nodeStart forwardTo recallShortTermMemory
                transformed { userQuery -> storage.set(userQueryKey, userQuery) }
            )
            edge(
                recallShortTermMemory forwardTo recallLongTermMemory
                transformed { shortTermMemory ->
                    storage.set(shortTermMemoryKey, shortTermMemory)
                    val userQuery = storage.get(userQueryKey)
                    LongTermMemoryRequest("Look up relevant long-term memory for the following user query: $userQuery")
                }
            )
            recallLongTermMemory then prepareWorkingMemoryInputs then loadInitialWorkingMemory then
                    appendUserQuery then requestLlm then updateShortTermMemory then updateLongTermMemory

            edge(
                updateLongTermMemory forwardTo executeTool
                onToolCall { true }
            )
            edge(
                updateLongTermMemory forwardTo nodeFinish
                    onAssistantMessage { true }
                    transformed { }
            )

            executeTool then sendToolResult then updateShortTermMemory
        }
    }
}

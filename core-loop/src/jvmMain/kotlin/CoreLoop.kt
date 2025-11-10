@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.duchastel.simon.brainiac.core.process

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.context.BrainiacContext
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemory
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRequest
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemory
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.loadInitialWorkingMemory
import com.duchastel.simon.brainiac.core.process.memory.recallLongTermMemory
import com.duchastel.simon.brainiac.core.process.memory.recallShortTermMemory
import com.duchastel.simon.brainiac.core.process.memory.updateLongTermMemory
import com.duchastel.simon.brainiac.core.process.memory.updateShortTermMemory
import kotlinx.coroutines.CompletableDeferred

/**
 * Instruction for the core loop resting state.
 */
sealed class CoreLoopInstruction {
    data object Stop : CoreLoopInstruction()
    data class Query(val userQuery: String) : CoreLoopInstruction()
}

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
class CoreLoop(
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val brainiacContext: BrainiacContext,
    private val awaitNextInstruction: suspend () -> CoreLoopInstruction,
    private val onThinking: (String) -> Unit = {},
    private val thinkingToolName: String? = null
) {
    fun strategy(
        name: String,
    ): AIAgentGraphStrategy<Unit, Unit> = with(brainiacContext) {
        strategy<Unit, Unit>(name) {
            val userQueryKey = createStorageKey<String>("${name}_user_prompt")
            val shortTermMemoryKey =
                createStorageKey<ShortTermMemory>("${name}_short_term_memory")

            val recallShortTermMemory by recallShortTermMemory(
                name = "recall_short_term_memory",
                shortTermMemoryRepository = shortTermMemoryRepository
            )
            val recallLongTermMemory by recallLongTermMemory(
                name = "recall_long_term_memory",
                longTermMemoryRepository = longTermMemoryRepository
            )
            val prepareWorkingMemoryInputs by node<LongTermMemory, Pair<ShortTermMemory, LongTermMemory>>(
                "prepare_working_memory_inputs"
            ) { longTermMemory ->
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
            val updateShortTermMemory by updateShortTermMemory<Message.Response>(
                name = "update_short_term_memory",
                shortTermMemoryRepository = shortTermMemoryRepository
            )
            val updateLongTermMemory by updateLongTermMemory<Message.Response>(
                name = "update_long_term_memory",
                longTermMemoryRepository = longTermMemoryRepository,
                shortTermMemoryRepository = shortTermMemoryRepository
            )

            val checkAndExecuteTool by node<Message.Tool.Call, Message.Tool.Response>("check_and_execute_tool") { toolCall ->
                // Check if this is the special ThinkingTool
                if (thinkingToolName != null && toolCall.tool == thinkingToolName) {
                    // Extract the thought from the tool call arguments
                    val thoughtPattern = """"thought"\s*:\s*"([^"]*)"""".toRegex()
                    val match = thoughtPattern.find(toolCall.content)
                    val thought = match?.groupValues?.get(1) ?: toolCall.content

                    // Route to the callback instead of normal execution
                    onThinking(thought)

                    // Return a synthetic tool response
                    Message.Tool.Response(
                        id = toolCall.id,
                        tool = toolCall.tool,
                        content = "Thought recorded"
                    )
                } else {
                    // Normal tool execution
                    tools.executeTool(toolCall)
                }
            }
            val sendToolResult by nodeLLMSendToolResult()

            val restingNode by node<Unit, CoreLoopInstruction>("resting_node") {
                awaitNextInstruction()
            }

            edge(nodeStart forwardTo restingNode)
            edge(
                recallShortTermMemory forwardTo recallLongTermMemory
                transformed { shortTermMemory ->
                    storage.set(shortTermMemoryKey, shortTermMemory)
                    val userQuery = storage.get(userQueryKey)
                    LongTermMemoryRequest("Look up relevant long-term memory for the following user query: $userQuery")
                }
            )
            recallLongTermMemory then prepareWorkingMemoryInputs then loadInitialWorkingMemory then
                    appendUserQuery then requestLlm then updateShortTermMemory

            edge(
                updateShortTermMemory forwardTo updateLongTermMemory
                transformed { llmResponse ->
                    storage.getValue(shortTermMemoryKey) to llmResponse
                }
            )

            edge(
                updateLongTermMemory forwardTo checkAndExecuteTool
                onToolCall { true }
            )
            edge(
                updateLongTermMemory forwardTo restingNode
                    onAssistantMessage { true }
                    transformed { }
            )

            edge(
                restingNode forwardTo nodeFinish
                onCondition { instruction -> instruction is CoreLoopInstruction.Stop }
                transformed { }
            )

            edge(
                restingNode forwardTo recallShortTermMemory
                onCondition { instruction -> instruction is CoreLoopInstruction.Query }
                transformed { instruction ->
                    val query = (instruction as CoreLoopInstruction.Query).userQuery
                    storage.set(userQueryKey, query)
                }
            )

            checkAndExecuteTool then sendToolResult then updateShortTermMemory
        }
    }
}

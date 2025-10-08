package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.nodes.nodeCallLLM
import com.duchastel.simon.brainiac.core.process.nodes.nodeCallTool
import com.duchastel.simon.brainiac.core.process.nodes.nodeReflection

/**
 * Creates a subgraph that implements the Thinking Loop process.
 *
 * This subgraph orchestrates the main LLM thinking loop with tool calls and reflection.
 * It handles:
 * 1. Calling the LLM
 * 2. Routing tool calls (including STM updates)
 * 3. Triggering reflection when context gets too large
 * 4. Looping back for multi-step reasoning
 *
 * The actual looping logic and conditional routing will be implemented based on
 * the Message.Response type and context size.
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes a String (prompt) as input and produces [Message.Response]
 */
fun AIAgentSubgraphBuilderBase<*, *>.subgraphThinkingLoop(
    name: String? = null,
): AIAgentSubgraphDelegate<String, Message.Response> = subgraph(name) {
    val nodeCallLLMNode by nodeCallLLM()
    val nodeCallToolNode by nodeCallTool()
    val nodeReflectionNode by nodeReflection()
    val nodeUpdateSTMSubgraph by subgraphUpdateSTM()

    // TODO: This is a simplified structure. The actual implementation needs:
    // 1. Loop detection and management
    // 2. Conditional routing based on Message.Response type
    // 3. Context size checking for reflection triggering
    // 4. Tool type detection for STM update routing

    edge(nodeStart forwardTo nodeCallLLMNode)

    // Route to Update STM if tool call is for STM update
    // TODO: Add conditional check for tool type
    edge(
        nodeCallLLMNode forwardTo nodeUpdateSTMSubgraph
            transformed { response ->
                // TODO: Extract UpdateSTMRequest from response
                UpdateSTMRequest("event", "TODO")
            }
    )

    // Update STM can loop back to CallLLM or finish
    // TODO: Add conditional logic
    edge(
        nodeUpdateSTMSubgraph forwardTo nodeCallLLMNode
            transformed { prompt ->
                // TODO: Convert FinalPrompt to String
                prompt.prompt
            }
    )

    edge(
        nodeUpdateSTMSubgraph forwardTo nodeFinish
            transformed { prompt ->
                // TODO: Convert to Message.Response
                Message.Response.Assistant("TODO")
            }
    )

    // Route to reflection if context is too large
    // TODO: Add context size check
    edge(
        nodeCallLLMNode forwardTo nodeReflectionNode
            transformed { response ->
                // TODO: Build ReflectionContext
                ReflectionContext(listOf(), ShortTermMemory(""))
            }
    )

    // Reflection can route to tool call or finish
    edge(
        nodeReflectionNode forwardTo nodeCallToolNode
            transformed { stm ->
                // TODO: Convert to Message.Response
                Message.Response.Assistant("TODO")
            }
    )

    edge(
        nodeReflectionNode forwardTo nodeFinish
            transformed { stm ->
                // TODO: Convert to Message.Response
                Message.Response.Assistant("TODO")
            }
    )

    // Tool call loops back to LLM
    edge(nodeCallToolNode forwardTo nodeCallLLMNode transformed { response -> "TODO" })

    // Direct finish if Assistant response
    edge(nodeCallLLMNode forwardTo nodeFinish)
}

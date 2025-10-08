package com.duchastel.simon.brainiac.core.process.nodes
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.graphs.*

/**
 * Creates an AI agent node that calls the LLM for the thinking loop.
 *
 * This node makes an LLM request and returns the response, which may be
 * an assistant message, a tool call, or other response types.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes a String (prompt) as input and produces [Message.Response]
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeCallLLM(
    name: String? = null,
): AIAgentNodeDelegate<String, Message.Response> = node(name) { input ->
    llm.readSession {
        requestLLM()
    }
}

/**
 * Creates an AI agent node that executes a tool call.
 *
 * This node handles tool execution requests from the LLM and returns the result.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes [Message.Response] as input and produces [Message.Response]
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeCallTool(
    name: String? = null,
): AIAgentNodeDelegate<Message.Response, Message.Response> = node(name) { input ->
    // TODO: Implement tool calling logic
    llm.readSession {
        requestLLM()
    }
}

/**
 * Creates an AI agent node that performs reflection on the conversation.
 *
 * This node analyzes the conversation history and updates short-term memory
 * with key facts, events, and insights.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes [ReflectionContext] as input and produces [ShortTermMemory]
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeReflection(
    name: String? = null,
): AIAgentNodeDelegate<ReflectionContext, ShortTermMemory> = node(name) { input ->
    // TODO: Implement reflection logic
    ShortTermMemory("TODO: perform reflection")
}

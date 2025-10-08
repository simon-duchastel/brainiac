package com.duchastel.simon.brainiac.core.process.nodes
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.graphs.LongTermMemory

/**
 * Creates an AI agent node that fetches the current short-term memory state.
 *
 * This node temporarily rewrites the prompt to fetch short-term memory content,
 * makes an LLM request without tools, and then restores the original prompt.
 * It's a generic function that can accept any input type and returns the LLM's
 * response message.
 *
 * @param T The type of input this node accepts
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes input of type T and produces a [Message.Response]
 */
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.nodeFetchShortTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<T, Message.Response> = node(name) { input ->
    llm.writeSession {
        val initialPrompt = prompt.copy()
        val longTermHistoryPrompt = Prompt.build("short-term-memory-fetch") {
            system(
                """
                TODO - Add a system prompt for short term memory fetch.
                """.trimIndent()
            )
        }
        rewritePrompt { longTermHistoryPrompt }

        val response = requestLLMWithoutTools()

        rewritePrompt {
            initialPrompt
        }

        response
    }
}

/**
 * Creates an AI agent node that resets the short-term memory after promotion.
 *
 * This node is responsible for clearing the short-term memory state after memories
 * have been successfully promoted to long-term storage. It maintains system hygiene
 * by ensuring STM doesn't grow unbounded and starts fresh for the next cycle.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes a List of [LongTermMemory] objects as input and produces Unit
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeResetShortTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<List<LongTermMemory>, Unit> = node(name) { input ->
    input
}
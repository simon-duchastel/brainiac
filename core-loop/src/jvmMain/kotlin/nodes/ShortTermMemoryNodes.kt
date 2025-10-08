package com.duchastel.simon.brainiac.core.process.nodes
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.graphs.LongTermMemory

/**
 *
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
 *
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeResetShortTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<List<LongTermMemory>, Unit> = node(name) { input ->
    input
}
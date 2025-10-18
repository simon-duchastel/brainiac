package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallShortTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<Unit, ShortTermMemory> = node(name) {
    ShortTermMemory("TODO")
}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateShortTermMemory(
    name: String? = null,
    tokenThreshold: Int = 50_000,
): AIAgentNodeDelegate<T, T> = node(name) { input ->
    val currentTokens = tokenizer.tokenCountFor(llm.prompt)

    if (currentTokens > tokenThreshold) {

    }

    input
}

data class ShortTermMemory(
    val memory: String
)
package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.features.tokenizer.feature.tokenizer

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallShortTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<Unit, ShortTermMemory> = node(name) {
    ShortTermMemory("")
}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateShortTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<T, T> = node(name) { input ->
    val currentTokens = tokenizer.tokenCountFor(llm.prompt)

    input
}

data class ShortTermMemory(
    val memory: String
)
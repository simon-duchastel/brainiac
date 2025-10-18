package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.agents.features.tokenizer.feature.tokenizer

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallLongTermMemory(
    name: String? = null,
): AIAgentSubgraphDelegate<LongTermMemoryRequest, LongTermMemory> = subgraph(name) {

}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateLongTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<T, T> = node(name) { input ->
    val currentTokens = tokenizer.tokenCountFor(llm.prompt)

    input
}

data class LongTermMemoryRequest(
    val query: String,
)

data class LongTermMemory(
    val memory: String
)
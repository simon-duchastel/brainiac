package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallLongTermMemory(
    name: String? = null,
): AIAgentNodeDelegate<LongTermMemoryRequest, LongTermMemory> = node(name) {
    LongTermMemory("TODO")
}

@AIAgentBuilderDslMarker
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.updateLongTermMemory(
    name: String? = null,
    tokenThreshold: Int = 10_000,
): AIAgentNodeDelegate<Pair<ShortTermMemory, T>, T> = node(name) { (shortTermMemory, input) ->
    // We need to put the short term memory into a fake prompt in order to count its tokens.
    // Currently the tokenizer can only tokenize messages or prompts, not arbitrary strings (even though
    // under the hood it just parses out the raw string).
    val shortTermMemoryForTokenization = prompt("short_term_memory") { shortTermMemory.memory }
    val currentTokens = tokenizer.tokenCountFor(shortTermMemoryForTokenization)

    if (currentTokens > tokenThreshold) {

    }

    input
}

data class LongTermMemoryRequest(
    val query: String,
)

data class LongTermMemory(
    val memory: String
)
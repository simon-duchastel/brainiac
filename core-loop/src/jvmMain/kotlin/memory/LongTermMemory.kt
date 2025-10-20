package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.text.TextContentBuilderBase
import ai.koog.prompt.xml.xml

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
    val shortTermMemoryForTokenization = prompt("short_term_memory") {
        system {
            shortTermMemory.asXmlRepresentation()
        }
    }
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
) {
    context(textBuilder: TextContentBuilderBase<*>)
    fun asXmlRepresentation(indented: Boolean = true) = textBuilder.xml(indented) {
        tag("long-term-memory") {
            +memory
        }
    }
}
package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.agent.session.AIAgentLLMWriteSession
import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.dsl.prompt

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.loadInitialWorkingMemory(
    name: String? = null,
): AIAgentNodeDelegate<Pair<ShortTermMemory, LongTermMemory>, Unit> {
    return node(name) { (shortTermMemory, longTermMemory) ->
        llm.writeSession {
            rewriteWorkingMemory(shortTermMemory, longTermMemory)
        }
    }
}

fun AIAgentLLMWriteSession.rewriteWorkingMemory(
    shortTermMemory: ShortTermMemory,
    longTermMemory: LongTermMemory,
) {
    rewritePrompt { originalPrompt ->
        originalPrompt.withMessages {
            prompt("initial_working_memory") {
                system {
                    longTermMemory.asXmlRepresentation()
                    shortTermMemory.asXmlRepresentation()
                }
            }.messages
        }
    }
}
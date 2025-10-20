package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.xml.xml

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.loadInitialWorkingMemory(
    name: String? = null,
): AIAgentNodeDelegate<Pair<ShortTermMemory, LongTermMemory>, Unit> {
    return node(name) { (shortTermMemory, longTermMemory) ->
        llm.writeSession {
            rewritePrompt { originalPrompt ->
                originalPrompt.withMessages {
                    prompt("initial_working_memory") {
                        system {
                            xml {
                                tag("long-term-memory") {
                                    +longTermMemory.memory
                                }
                                tag("short-term-memory") {
                                    tag("thoughts") {
                                        shortTermMemory.thoughts.forEach {
                                            tag("thought") {
                                                +it
                                            }
                                        }
                                    }
                                    tag("goals") {
                                        shortTermMemory.goals.forEach {
                                            tag(
                                                name = "goal",
                                                attributes = linkedMapOf("completed" to it.completed.toString())
                                            ) {
                                                +it.description
                                            }
                                        }
                                    }
                                    tag("events") {
                                        shortTermMemory.events.forEach {
                                            tag("event") {
                                                +it
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }.messages
                }
            }
        }
    }
}
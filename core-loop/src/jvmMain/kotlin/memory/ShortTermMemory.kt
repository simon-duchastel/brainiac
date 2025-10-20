package com.duchastel.simon.brainiac.core.process.memory

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentBuilderDslMarker
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.features.tokenizer.feature.tokenizer
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.text.TextContentBuilderBase
import ai.koog.prompt.xml.xml
import com.duchastel.simon.brainiac.core.process.context.BrainiacContext
import com.duchastel.simon.brainiac.core.process.util.withModel
import kotlinx.serialization.Serializable

@AIAgentBuilderDslMarker
fun AIAgentSubgraphBuilderBase<*, *>.recallShortTermMemory(
    name: String? = null,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentNodeDelegate<Unit, ShortTermMemory> = node(name) {
    shortTermMemoryRepository.getShortTermMemory()
}

@AIAgentBuilderDslMarker
context(brainiacContext: BrainiacContext)
inline fun <reified T: Any> AIAgentSubgraphBuilderBase<*, *>.updateShortTermMemory(
    name: String? = null,
    tokenThreshold: Int = 50_000,
    shortTermMemoryRepository: ShortTermMemoryRepository,
): AIAgentSubgraphDelegate<T, T> = subgraph(name) {
    val initialInputKey = createStorageKey<T>("${name}_initial_input")
    val initialPromptKey = createStorageKey<Prompt>("${name}_initial_prompt")

    val setup by node<T, Unit>("${name}_setup") { input ->
        storage.set(initialInputKey, input)
        storage.set(initialPromptKey, llm.prompt)
    }
    val cleanup by node<Unit, T>("${name}_cleanup") {
        storage.getValue(initialInputKey)
    }
    val evaluateNeedForShortTermMemoryUpdate by node<Unit, Boolean>("${name}_evalute_tokens") {
        tokenizer.tokenCountFor(llm.prompt) >= tokenThreshold
    }

    val updateEvents by node<Unit, List<String>>("${name}_update_events") {
        val originalPrompt = storage.getValue(initialPromptKey)
        llm.writeSession {
            rewritePrompt { originalPrompt }
            updatePrompt {
                system {
                    +"""
                    Review the conversation and the current short-term memory.
                    Identify any events that should be added to the events list based on the current context.

                    Return all new events to be added (not including current events), in structured format.
                    """.trimIndent()
                }
            }
            withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<List<String>>().getOrNull()!!.structure
            }
        }
    }
    val updateGoals by node<List<String>, Pair<List<String>, List<Goal>>>("${name}_update_goals") { events ->
        val originalPrompt = storage.getValue(initialPromptKey)
        llm.writeSession {
            rewritePrompt { originalPrompt }
            updatePrompt {
                system {
                    +"""
                    Review the conversation and the current short-term memory.
                    Identify any goals that should be added, removed, or have their completion status changed.

                    Return the updated complete list of goals, including all goals which should be kept, in structured format.
                    """.trimIndent()
                }
            }

            val goals = withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<List<Goal>>().getOrNull()!!.structure
            }
            events to goals
        }
    }
    val updateThoughts by node<Pair<List<String>, List<Goal>>, Triple<List<String>, List<Goal>, List<String>>>("${name}_update_thoughts") { (events, goals) ->
        val originalPrompt = storage.getValue(initialPromptKey)
        llm.writeSession {
            rewritePrompt { originalPrompt }
            updatePrompt {
                system {
                    +"""
                    Review the conversation and the current short-term memory.
                    Identify any thoughts that should be distilled and added to the thoughts list
                    
                    Return all new thoughts to be added (not including current thoughts), in structured format.
                    """.trimIndent()
                }
            }
            val thoughts = withModel(brainiacContext.mediumThoughtModel) {
                requestLLMStructured<List<String>>().getOrNull()!!.structure
            }
            Triple(events, goals, thoughts)
        }
    }

    val updatePrompt by node<Triple<List<String>, List<Goal>, List<String>>, Unit>("${name}_update_prompt") { (events, goals, thoughts) ->
        shortTermMemoryRepository.updateThoughts(thoughts)
        shortTermMemoryRepository.updateGoals(goals)
        events.forEach { shortTermMemoryRepository.addEvent(it) }
        val updatedShortTermMemory = shortTermMemoryRepository.getShortTermMemory()

        val originalPrompt = storage.getValue(initialPromptKey)
        llm.writeSession {
            rewritePrompt { originalPrompt }
            updatePrompt {
                system { summarizeWorkingMemory(updatedShortTermMemory) }
            }
            val summaryMessage = requestLLM()
            rewriteWorkingMemory(
                updatedShortTermMemory,
                LongTermMemory(listOf())
            )
            prompt = prompt.withMessages { currentMessages -> currentMessages + summaryMessage }
        }
    }

    edge(nodeStart forwardTo setup)
    edge(setup forwardTo evaluateNeedForShortTermMemoryUpdate)
    edge(
        evaluateNeedForShortTermMemoryUpdate forwardTo updateEvents
        onCondition { it }
        transformed { }
    )
    updateEvents then updateGoals then updateThoughts then updatePrompt then cleanup
    edge(
        evaluateNeedForShortTermMemoryUpdate forwardTo cleanup
                onCondition { !it }
                transformed { }
    )
    cleanup then nodeFinish
}

@Serializable
data class ShortTermMemory(
    val thoughts: List<String> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val events: List<String> = emptyList(),
) {
    context(textBuilder: TextContentBuilderBase<*>)
    fun asXmlRepresentation(indented: Boolean = true) = textBuilder.xml(indented) {
        tag("short-term-memory") {
            tag("thoughts") {
                thoughts.forEach {
                    tag("thought") {
                        +it
                    }
                }
            }
            tag("goals") {
                goals.forEach {
                    tag(
                        name = "goal",
                        attributes = linkedMapOf("completed" to it.completed.toString())
                    ) {
                        +it.description
                    }
                }
            }
            tag("events") {
                events.forEach {
                    tag("event") {
                        +it
                    }
                }
            }
        }
    }
}

@Serializable
data class Goal(
    val description: String,
    val completed: Boolean = false,
)

fun TextContentBuilderBase<*>.summarizeWorkingMemory(
    updatedShortTermMemory: ShortTermMemory,
) = markdown {
    +"Summarize the current context based on the updated short-term memory below:"

    updatedShortTermMemory.asXmlRepresentation()

    +"Remove any irrelevant or redundant information as well as any information already included in the updated short-term memory"
    +"For example, discard any information pertaining to completed goals which you feel will not be relevant moving forward"
    +"Keep any information that will be useful in the future for working memory, ie. information immediately pertinent to what the user is asking you to do."
}
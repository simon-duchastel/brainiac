package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.prompt.message.Message
import com.duchastel.simon.brainiac.core.process.nodes.nodeFetchShortTermMemory

/**
 * Creates the main Core Process Graph that orchestrates the entire memory system flow.
 *
 * This is the top-level graph that implements the complete Brainiac memory system pipeline:
 * 1. Fetch short-term memory file
 * 2. Fetch relevant long-term memories based on user prompt
 * 3. Build final prompt incorporating system prompt, user prompt, STM, and LTM
 * 4. Run the thinking loop (LLM with tool use, multi-step reasoning)
 * 5. Perform reflection to capture key information into STM
 * 6. Run promotion to consolidate STM into LTM (if needed)
 * 7. Return final result to user
 *
 * Based on the Setup (Overall Flow) diagram from specs/graph-spec.md
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes a String (user prompt) as input and produces a String (final response)
 */
fun AIAgentSubgraphBuilderBase<*, *>.coreProcessGraph(
    name: String? = null,
): AIAgentSubgraphDelegate<String, String> = subgraph(name) {
    val nodeFetchSTM by nodeFetchShortTermMemory<String>()
    val subgraphFetchLTMProcess by subgraphFetchLTM()
    val subgraphBuildPrompt by subgraphBuildFinalPrompt()
    val subgraphThinkingLoopProcess by subgraphThinkingLoop()
    val subgraphReflectionProcess by subgraphReflection()

    // Start: User prompt comes in
    edge(nodeStart forwardTo nodeFetchSTM)

    // Fetch STM → Fetch LTM
    // Need to pass the user prompt to FetchLTM, not the Message.Response from FetchSTM
    edge(
        nodeFetchSTM forwardTo subgraphFetchLTMProcess
            transformed { response ->
                // TODO: Extract user prompt from context
                "TODO: user prompt"
            }
    )

    // Fetch LTM → Build Final Prompt
    // Need to combine user prompt, STM, and LTM files
    edge(
        subgraphFetchLTMProcess forwardTo subgraphBuildPrompt
            transformed { ltmFiles ->
                // TODO: Build FinalPromptInput with actual user prompt and STM
                FinalPromptInput(
                    userPrompt = "TODO: user prompt",
                    shortTermMemory = ShortTermMemory("TODO: STM content"),
                    longTermMemories = ltmFiles
                )
            }
    )

    // Build Final Prompt → Thinking Loop
    edge(
        subgraphBuildPrompt forwardTo subgraphThinkingLoopProcess
            transformed { finalPrompt ->
                finalPrompt.prompt
            }
    )

    // Thinking Loop → Reflection
    edge(
        subgraphThinkingLoopProcess forwardTo subgraphReflectionProcess
            transformed { response ->
                // TODO: Build ReflectionContext with conversation history
                ReflectionContext(
                    conversationHistory = listOf("TODO: conversation history"),
                    currentSTM = ShortTermMemory("TODO: current STM")
                )
            }
    )

    // Reflection → Finish
    // Reflection produces FinalPrompt, but we need to return String
    edge(
        subgraphReflectionProcess forwardTo nodeFinish
            transformed { finalPrompt ->
                // TODO: Extract final response from the process
                "TODO: final response to user"
            }
    )
}

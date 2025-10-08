package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import com.duchastel.simon.brainiac.core.process.nodes.nodeBuildFinalPrompt

/**
 * Creates a subgraph that implements the Build Final Prompt process.
 *
 * This subgraph takes the user prompt, short-term memory, and relevant long-term
 * memories and combines them into a complete prompt ready for the LLM.
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes [FinalPromptInput] as input and produces [FinalPrompt]
 */
fun AIAgentSubgraphBuilderBase<*, *>.subgraphBuildFinalPrompt(
    name: String? = null,
): AIAgentSubgraphDelegate<FinalPromptInput, FinalPrompt> = subgraph(name) {
    val nodeBuild by nodeBuildFinalPrompt()

    edge(nodeStart forwardTo nodeBuild)
    edge(nodeBuild forwardTo nodeFinish)
}

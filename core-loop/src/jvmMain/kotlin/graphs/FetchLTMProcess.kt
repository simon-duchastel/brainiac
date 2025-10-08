package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import com.duchastel.simon.brainiac.core.process.nodes.nodeAskLLMForRelevantFiles
import com.duchastel.simon.brainiac.core.process.nodes.nodeFetchLTMFiles
import com.duchastel.simon.brainiac.core.process.nodes.nodeGetMindMapFile

/**
 * Creates a subgraph that implements the Fetch LTM process.
 *
 * This subgraph retrieves relevant long-term memories based on the user prompt by:
 * 1. Fetching the mind map index file
 * 2. Using the LLM to identify relevant files in the mind map
 * 3. Fetching the actual LTM files
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes a String (user prompt) as input and produces a List of [LongTermMemory]
 */
fun AIAgentSubgraphBuilderBase<*, *>.subgraphFetchLTM(
    name: String? = null,
): AIAgentSubgraphDelegate<String, List<LongTermMemory>> = subgraph(name) {
    val nodeFetchMindMap by nodeGetMindMapFile()
    val nodeAskLLM by nodeAskLLMForRelevantFiles()
    val nodeFetchFiles by nodeFetchLTMFiles()

    edge(nodeStart forwardTo nodeFetchMindMap)
    edge(nodeFetchMindMap forwardTo nodeAskLLM)
    edge(nodeAskLLM forwardTo nodeFetchFiles)
    edge(nodeFetchFiles forwardTo nodeFinish)
}

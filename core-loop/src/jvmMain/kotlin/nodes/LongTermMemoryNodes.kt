package com.duchastel.simon.brainiac.core.process.nodes
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import com.duchastel.simon.brainiac.core.process.graphs.LongTermMemory
import com.duchastel.simon.brainiac.core.process.graphs.LongTermMemoryCandidate
import com.duchastel.simon.brainiac.core.process.graphs.ShortTermMemory

/**
 *
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeFetchLongTermMemories(
    name: String? = null,
): AIAgentNodeDelegate<String, List<LongTermMemory>> = node(name) { input ->
    llm.readSession {
        requestLLMStructured<List<LongTermMemory>>().getOrNull()!!.structure
    }
}

/**
 *
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeDistillLongTermMemoryCandidates(
    name: String? = null,
): AIAgentNodeDelegate<ShortTermMemory, List<LongTermMemoryCandidate>> = node(name) { input ->
    llm.readSession {
        requestLLMStructured<List<LongTermMemoryCandidate>>().getOrNull()!!.structure
    }
}

/**
 *
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeWriteLongTermMemoryCandidates(
    name: String? = null,
): AIAgentNodeDelegate<List<LongTermMemoryCandidate>, List<LongTermMemory>> = node(name) { input ->
    listOf()
}


/**
 *
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeUpdateMindMapIndex(
    name: String? = null,
): AIAgentNodeDelegate<List<LongTermMemory>, Unit> = node(name) { input ->

}
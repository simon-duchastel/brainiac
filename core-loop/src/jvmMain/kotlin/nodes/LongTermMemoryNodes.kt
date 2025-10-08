package com.duchastel.simon.brainiac.core.process.nodes
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import com.duchastel.simon.brainiac.core.process.graphs.LongTermMemory
import com.duchastel.simon.brainiac.core.process.graphs.LongTermMemoryCandidate
import com.duchastel.simon.brainiac.core.process.graphs.ShortTermMemory

/**
 * Creates an AI agent node that fetches relevant long-term memories based on input context.
 *
 * This node queries the LLM to retrieve structured long-term memory entries that are
 * relevant to the current processing context. It uses the LLM's structured output
 * capabilities to return a typed list of [LongTermMemory] objects.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes a String input and produces a List of [LongTermMemory] objects
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeFetchLongTermMemories(
    name: String? = null,
): AIAgentNodeDelegate<String, List<LongTermMemory>> = node(name) { input ->
    llm.readSession {
        requestLLMStructured<List<LongTermMemory>>().getOrNull()!!.structure
    }
}

/**
 * Creates an AI agent node that analyzes short-term memory to identify promotion candidates.
 *
 * This node processes the short-term memory content and uses the LLM to distill
 * important insights, events, or information that should be promoted to long-term memory.
 * It returns a list of candidates that warrant permanent storage.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes [ShortTermMemory] input and produces a List of [LongTermMemoryCandidate] objects
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeDistillLongTermMemoryCandidates(
    name: String? = null,
): AIAgentNodeDelegate<ShortTermMemory, List<LongTermMemoryCandidate>> = node(name) { input ->
    llm.readSession {
        requestLLMStructured<List<LongTermMemoryCandidate>>().getOrNull()!!.structure
    }
}

/**
 * Creates an AI agent node that writes long-term memory candidates to permanent storage.
 *
 * This node takes a list of memory candidates and persists them as actual long-term
 * memory entries. It handles the conversion from candidate descriptions to structured
 * memory objects that can be stored and retrieved later.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes a List of [LongTermMemoryCandidate] objects and produces a List of [LongTermMemory] objects
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeWriteLongTermMemoryCandidates(
    name: String? = null,
): AIAgentNodeDelegate<List<LongTermMemoryCandidate>, List<LongTermMemory>> = node(name) { input ->
    listOf()
}


/**
 * Creates an AI agent node that updates the mind map index with new long-term memories.
 *
 * This node maintains the semantic relationships and indexing structure for long-term
 * memories. It updates the `_index.md` files in the LTM directory structure to ensure
 * new memories are properly linked and discoverable through the semantic web.
 *
 * @param name Optional name for the node. If null, a default name will be assigned.
 * @return A node delegate that takes a List of [LongTermMemory] objects as input and produces Unit
 */
fun AIAgentSubgraphBuilderBase<*, *>.nodeUpdateMindMapIndex(
    name: String? = null,
): AIAgentNodeDelegate<List<LongTermMemory>, Unit> = node(name) { input ->

}
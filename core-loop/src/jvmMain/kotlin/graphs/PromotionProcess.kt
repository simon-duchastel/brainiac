package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import com.duchastel.simon.brainiac.core.process.nodes.nodeDistillLongTermMemoryCandidates
import com.duchastel.simon.brainiac.core.process.nodes.nodeResetShortTermMemory
import com.duchastel.simon.brainiac.core.process.nodes.nodeUpdateMindMapIndex
import com.duchastel.simon.brainiac.core.process.nodes.nodeWriteLongTermMemoryCandidates
import com.duchastel.simon.brainiac.core.process.nodes.util.passthrough

/**
 * Creates a subgraph that implements the memory promotion process.
 *
 * This subgraph orchestrates the promotion of information from short-term memory to
 * long-term memory. It follows these steps:
 * 1. Distill LTM candidates from short-term memory
 * 2. Write the candidates as actual long-term memories
 * 3. Update the mind map index and reset short-term memory in parallel
 *
 * The process ensures that important information is permanently stored while maintaining
 * the semantic relationships in the memory system.
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes [ShortTermMemory] as input and produces a List of [LongTermMemory] objects
 */
fun AIAgentSubgraphBuilderBase<*, *>.subgraphPromotionProcess(
    name: String? = null,
): AIAgentSubgraphDelegate<ShortTermMemory, List<LongTermMemory>> = subgraph(name) {
    val nodeDistillLtmCandidates by nodeDistillLongTermMemoryCandidates()
    val nodeWriteLtmCandidates by nodeWriteLongTermMemoryCandidates()
    val nodeUpdateMindMap by passthrough(nodeUpdateMindMapIndex())
    val nodeResetShortTermMemory by passthrough(nodeResetShortTermMemory())
    val updateState by parallel(
        nodeUpdateMindMap,
            nodeResetShortTermMemory,
    ) {
        selectBy { true } // both outputs are passthroughs so pick any of them
    }

    edge(nodeStart forwardTo nodeDistillLtmCandidates)
    edge(nodeDistillLtmCandidates forwardTo nodeWriteLtmCandidates)
    edge(nodeWriteLtmCandidates forwardTo updateState)
    edge(updateState forwardTo nodeFinish)
}

/**
 * Represents a persistent memory entry in the long-term memory system.
 *
 * Long-term memories are stored in the `memory/long_term/` directory as Markdown files
 * with YAML frontmatter. They form the permanent knowledge base of the AI system.
 *
 * @property fileName The name of the file where this memory is stored
 * @property memory The content of the memory entry
 */
data class LongTermMemory(
    val fileName: String,
    val memory: String,
)

/**
 * Represents a potential memory entry that may be promoted to long-term storage.
 *
 * During the promotion process, the system analyzes short-term memory to identify
 * important information, events, or insights that should be permanently stored.
 * These are first identified as candidates before being written to LTM.
 *
 * @property candidateDescription A description of the information to be promoted
 */
data class LongTermMemoryCandidate(
    val candidateDescription: String,
)

/**
 * Represents the current state of short-term memory.
 *
 * Short-term memory serves as a staging area for recent interactions and context.
 * It's stored in `memory/short_term.md` and contains a summary, structured data
 * (goals, facts, tasks), and an event log of recent interactions. STM is periodically
 * pruned and promoted to long-term memory.
 *
 * @property memory The current content of short-term memory
 */
data class ShortTermMemory(
    val memory: String,
)
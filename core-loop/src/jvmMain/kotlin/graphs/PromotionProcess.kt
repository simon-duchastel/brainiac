package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import com.duchastel.simon.brainiac.core.process.nodes.nodeDistillLongTermMemoryCandidates
import com.duchastel.simon.brainiac.core.process.nodes.nodeResetShortTermMemory
import com.duchastel.simon.brainiac.core.process.nodes.nodeUpdateMindMapIndex
import com.duchastel.simon.brainiac.core.process.nodes.nodeWriteLongTermMemoryCandidates
import com.duchastel.simon.brainiac.core.process.nodes.util.passthroughWithInput

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
    val nodeUpdateMindMap by passthroughWithInput(nodeUpdateMindMapIndex())
    val nodeResetShortTermMemory by passthroughWithInput(nodeResetShortTermMemory())
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

/**
 * Represents a request to update short-term memory.
 *
 * Updates can be either new events (episodic) or new insights (semantic).
 *
 * @property type The type of update: "event" or "insight"
 * @property content The content to add to short-term memory
 */
data class UpdateSTMRequest(
    val type: String, // "event" or "insight"
    val content: String,
)

/**
 * Represents the mind map index file content.
 *
 * The mind map is stored in `memory/long_term/_index.md` and contains a semantic
 * graph of all long-term memories with their relationships and metadata.
 *
 * @property content The content of the mind map index file
 */
data class MindMapFile(
    val content: String,
)

/**
 * Represents the input required to build the final prompt.
 *
 * Combines the user's original prompt with context from short-term and long-term memory.
 *
 * @property userPrompt The original user prompt
 * @property shortTermMemory Current short-term memory content
 * @property longTermMemories Relevant long-term memory entries
 */
data class FinalPromptInput(
    val userPrompt: String,
    val shortTermMemory: ShortTermMemory,
    val longTermMemories: List<LongTermMemory>,
)

/**
 * Represents a fully assembled prompt ready for the LLM.
 *
 * @property prompt The complete prompt with system instructions, memory context, and user query
 */
data class FinalPrompt(
    val prompt: String,
)

/**
 * Represents the context needed for the reflection process.
 *
 * Contains the conversation history and current state to extract key information.
 *
 * @property conversationHistory The history of messages in the current conversation
 * @property currentSTM Current short-term memory state
 */
data class ReflectionContext(
    val conversationHistory: List<String>,
    val currentSTM: ShortTermMemory,
)
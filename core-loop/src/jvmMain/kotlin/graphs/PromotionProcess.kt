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
 *
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
 *
 */
data class LongTermMemory(
    val fileName: String,
    val memory: String,
)

/**
 *
 */
data class LongTermMemoryCandidate(
    val candidateDescription: String,
)

/**
 *
 */
data class ShortTermMemory(
    val memory: String,
)
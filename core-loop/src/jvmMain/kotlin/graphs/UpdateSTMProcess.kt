package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import com.duchastel.simon.brainiac.core.process.nodes.nodeAppendEvent
import com.duchastel.simon.brainiac.core.process.nodes.nodeAppendInsight
import com.duchastel.simon.brainiac.core.process.nodes.nodeBuildFinalPrompt
import com.duchastel.simon.brainiac.core.process.nodes.nodeProcessSTMRequest

/**
 * Creates a subgraph that implements the Update STM process.
 *
 * This subgraph handles updates to short-term memory by routing requests
 * to either event or insight appending nodes based on the request type.
 *
 * Flow:
 * 1. Process the STM update request
 * 2. Route to either event or insight appending based on type
 * 3. Build final prompt with updated STM
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes [UpdateSTMRequest] as input and produces [FinalPrompt]
 */
fun AIAgentSubgraphBuilderBase<*, *>.subgraphUpdateSTM(
    name: String? = null,
): AIAgentSubgraphDelegate<UpdateSTMRequest, FinalPrompt> = subgraph(name) {
    val nodeProcessRequest by nodeProcessSTMRequest()
    val nodeAppendEventNode by nodeAppendEvent()
    val nodeAppendInsightNode by nodeAppendInsight()
    val nodeBuildPrompt by nodeBuildFinalPrompt()

    // TODO: This needs proper conditional routing based on request type
    // For now, using a simple conditional that checks the type field
    edge(nodeStart forwardTo nodeProcessRequest)

    edge(
        nodeProcessRequest forwardTo nodeAppendEventNode
            onCondition  { request -> request.type == "event"}
    )

    edge(
        nodeProcessRequest forwardTo nodeAppendInsightNode
            onCondition { request -> request.type == "insight" }
    )

    // Both event and insight nodes produce ShortTermMemory, which needs to be
    // transformed into FinalPromptInput for the build prompt node
    edge(
        nodeAppendEventNode forwardTo nodeBuildPrompt
            transformed { stm ->
                // TODO: This needs actual user prompt and LTM data
                FinalPromptInput("", stm, listOf())
            }
    )

    edge(
        nodeAppendInsightNode forwardTo nodeBuildPrompt
            transformed { stm ->
                // TODO: This needs actual user prompt and LTM data
                FinalPromptInput("", stm, listOf())
            }
    )

    edge(nodeBuildPrompt forwardTo nodeFinish)
}

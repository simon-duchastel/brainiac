package com.duchastel.simon.brainiac.core.process.graphs

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import com.duchastel.simon.brainiac.core.process.nodes.nodeReflection

/**
 * Creates a subgraph that implements the Reflection process.
 *
 * This subgraph handles the reflection process when context gets too large:
 * 1. Distill key events and insights from conversation context
 * 2. Append insights and events to STM, distill old context and replace
 * 3. Check if new STM size exceeds max_size
 * 4. If STM too large, route to Promotion subgraph
 * 5. Otherwise, route to Build Final Prompt
 *
 * @param name Optional name for the subgraph. If null, a default name will be assigned.
 * @return A subgraph delegate that takes [ReflectionContext] as input and produces [FinalPrompt]
 */
fun AIAgentSubgraphBuilderBase<*, *>.subgraphReflection(
    name: String? = null,
): AIAgentSubgraphDelegate<ReflectionContext, FinalPrompt> = subgraph(name) {
    val nodeDistillInsights by nodeReflection()
    val nodePromotionSubgraph by subgraphPromotionProcess()
    val nodeBuildPromptSubgraph by subgraphBuildFinalPrompt()

    edge(nodeStart forwardTo nodeDistillInsights)

    // Route to Promotion if STM is too large
    edge(
        nodeDistillInsights forwardTo nodePromotionSubgraph
            transformed { stm ->
                // TODO: Add actual STM size check
                // For now, always route based on size threshold
                if (stm.memory.length > 10000) { // TODO: Replace with actual max_size check
                    stm
                } else {
                    null
                }
            }
    )

    // Promotion produces List<LongTermMemory>, needs to build final prompt
    edge(
        nodePromotionSubgraph forwardTo nodeBuildPromptSubgraph
            transformed { ltmList ->
                // TODO: Build FinalPromptInput with actual data
                FinalPromptInput("", ShortTermMemory(""), ltmList)
            }
    )

    // Route to Build Final Prompt if STM is not too large
    edge(
        nodeDistillInsights forwardTo nodeBuildPromptSubgraph
            transformed { stm ->
                // TODO: Add actual STM size check (inverse of above)
                if (stm.memory.length <= 10000) {
                    // TODO: Build FinalPromptInput with actual data
                    FinalPromptInput("", stm, listOf())
                } else {
                    null
                }
            }
    )

    edge(nodeBuildPromptSubgraph forwardTo nodeFinish)
}

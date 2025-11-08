package com.duchastel.simon.brainiac.tools

import ai.koog.agents.core.tools.ToolRegistry

/**
 * Creates a ToolRegistry from a list of BrainiacTools.
 *
 * @param tools The list of Brainiac tools to include in the registry
 * @return A ToolRegistry containing all the converted Koog tools
 */
fun createToolRegistry(vararg tools: BrainiacTool): ToolRegistry {
    if (tools.isEmpty()) {
        return ToolRegistry.EMPTY
    }

    return ToolRegistry {
        tools(tools.map(BrainiacTool::toKoogTool))
    }
}

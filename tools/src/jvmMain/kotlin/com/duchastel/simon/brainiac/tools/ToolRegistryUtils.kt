package com.duchastel.simon.brainiac.tools

import ai.koog.agents.core.tools.Tool
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

    // Convert BrainiacTools to Koog Tools
    val koogTools = tools.map { it.toKoogTool() }

    // Build a ToolRegistry by adding tools one at a time
    return koogTools.fold(ToolRegistry.EMPTY) { registry, tool ->
        registry + tool
    }
}

/**
 * Operator function to add a Tool to a ToolRegistry.
 * This provides a convenient way to add tools to an existing registry.
 */
private operator fun ToolRegistry.plus(tool: Tool<*, *>): ToolRegistry {
    // Get tools from current registry and add the new tool
    val currentTools = this.tools
    val newTools = currentTools + tool

    // Use reflection to access the private constructor
    val constructor = ToolRegistry::class.java.declaredConstructors.first()
    constructor.isAccessible = true
    return constructor.newInstance(newTools) as ToolRegistry
}

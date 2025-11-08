package com.duchastel.simon.brainiac.tools

import ai.koog.agents.core.tools.Tool

/**
 * Base interface for all Brainiac tools.
 */
interface BrainiacTool {
    /**
     * Converts this Brainiac tool to a Koog Tool that can be used
     * by the AI agent system.
     *
     * @return A Koog Tool instance with type parameters for args and result
     */
    fun toKoogTool(): Tool<*, *>
}

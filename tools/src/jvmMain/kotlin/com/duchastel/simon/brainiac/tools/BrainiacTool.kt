package com.duchastel.simon.brainiac.tools

import ai.koog.agents.core.tools.Tool

/**
 * Base interface for all Brainiac tools.
 *
 * This interface provides an abstraction layer over Koog's Tool interface,
 * making it easier to configure and manage tools in the Brainiac system.
 *
 * Each tool implementation should:
 * 1. Define its configuration (API keys, parameters, etc.)
 * 2. Implement the conversion to Koog's Tool interface
 * 3. Handle all implementation details internally
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

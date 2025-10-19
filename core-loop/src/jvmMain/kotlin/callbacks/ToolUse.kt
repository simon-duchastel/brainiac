package com.duchastel.simon.brainiac.core.process.callbacks

/**
 * Sealed interface representing tool use actions available to the AI agent.
 *
 * Currently supports memory storage operations only.
 */
sealed interface ToolUse {
    /**
     * Store information in short-term memory.
     */
    data object StoreShortTermMemory : ToolUse

    /**
     * Store information in long-term memory.
     */
    data object StoreLongTermMemory : ToolUse
}
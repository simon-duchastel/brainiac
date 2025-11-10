package com.duchastel.simon.brainiac.core.process.callbacks

sealed interface ToolUse {
    data object StoreShortTermMemory : ToolUse

    data object StoreLongTermMemory : ToolUse

    data class Talk(val message: String) : ToolUse
}
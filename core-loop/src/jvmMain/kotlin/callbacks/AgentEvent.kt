package com.duchastel.simon.brainiac.core.process.callbacks

import com.duchastel.simon.brainiac.core.process.callbacks.ToolUse

/**
 * Sealed interface representing events that occur during agent execution.
 *
 * These events are emitted via callbacks to notify consumers about
 * AI messages and tool usage.
 */
sealed interface AgentEvent {
    data class Thought(val content: String) : AgentEvent

    data class Talk(val content: String) : AgentEvent

    data class AssistantMessage(val content: String) : AgentEvent

    data class ToolCall(val tool: ToolUse) : AgentEvent

    data class ToolResult(val tool: ToolUse, val success: Boolean) : AgentEvent
}

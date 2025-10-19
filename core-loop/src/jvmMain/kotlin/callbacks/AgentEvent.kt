package com.duchastel.simon.brainiac.core.process.callbacks

import com.duchastel.simon.brainiac.core.process.tools.ToolUse

/**
 * Sealed interface representing events that occur during agent execution.
 *
 * These events are emitted via callbacks to notify consumers about
 * AI messages and tool usage.
 */
sealed interface AgentEvent {
    /**
     * Emitted when the AI assistant sends a message to the user.
     *
     * @property content The text content of the assistant's message
     */
    data class AssistantMessage(val content: String) : AgentEvent

    /**
     * Emitted when the AI calls a tool.
     *
     * @property tool The tool being invoked
     */
    data class ToolCall(val tool: ToolUse) : AgentEvent

    /**
     * Emitted when a tool execution completes.
     *
     * @property tool The tool that was executed
     * @property success Whether the tool execution succeeded
     */
    data class ToolResult(val tool: ToolUse, val success: Boolean) : AgentEvent
}

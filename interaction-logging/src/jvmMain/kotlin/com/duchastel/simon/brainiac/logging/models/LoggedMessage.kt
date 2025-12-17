package com.duchastel.simon.brainiac.logging.models

import kotlinx.serialization.Serializable

/**
 * A unique message stored once per content hash.
 * Messages are deduplicated by content - identical content = same ID.
 */
@Serializable
data class LoggedMessage(
    /** SHA-256 hash of content, used as unique identifier */
    val id: String,
    /** ISO-8601 timestamp of first occurrence */
    val timestamp: String,
    /** Role of the message sender */
    val role: MessageRole,
    /** The actual message content */
    val content: String,
    /** Optional metadata about the message */
    val metadata: MessageMetadata? = null
)

@Serializable
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL_CALL,
    TOOL_RESULT
}

@Serializable
data class MessageMetadata(
    /** LLM model used (for assistant messages) */
    val model: String? = null,
    /** Tool name (for tool calls/results) */
    val toolName: String? = null,
    /** Estimated token count */
    val tokenCount: Int? = null,
    /** Extended thinking content (if separate from main content) */
    val thinkingContent: String? = null
)

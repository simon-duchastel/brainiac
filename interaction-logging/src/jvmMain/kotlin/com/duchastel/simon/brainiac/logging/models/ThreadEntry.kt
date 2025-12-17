package com.duchastel.simon.brainiac.logging.models

import kotlinx.serialization.Serializable

/**
 * An entry in a thread log, representing an event that occurred.
 * Thread entries reference messages by ID and track position in context.
 */
@Serializable
data class ThreadEntry(
    /** ISO-8601 timestamp of when this entry was recorded */
    val timestamp: String,
    /** Reference to LoggedMessage.id (null for memory operations) */
    val messageId: String? = null,
    /** 0-indexed position in LLM context (null for memory operations) */
    val positionInContext: Int? = null,
    /** Type of event that occurred */
    val eventType: ThreadEventType,
    /** Optional snapshot of context state */
    val contextSnapshot: ContextSnapshot? = null,
    /** Present for MEMORY_* event types */
    val memoryOperation: MemoryOperation? = null
)

@Serializable
enum class ThreadEventType {
    /** New message added to context */
    MESSAGE_ADDED,
    /** Existing message replayed in new request */
    CONTEXT_REPLAYED,
    /** Context was summarized/compacted */
    CONTEXT_COMPACTED,
    /** An LLM request was made */
    LLM_REQUEST,
    /** LLM responded */
    LLM_RESPONSE,
    /** Memory file was read (STM/LTM recall) */
    MEMORY_READ,
    /** Memory file was written (STM/LTM update) */
    MEMORY_WRITE,
    /** Semantic search/query on memory */
    MEMORY_QUERY
}

@Serializable
data class ContextSnapshot(
    /** Total number of messages in context */
    val totalMessages: Int,
    /** Estimated token count */
    val estimatedTokens: Int? = null,
    /** Model used for this request */
    val modelUsed: String? = null
)

@Serializable
data class MemoryOperation(
    /** Type of memory accessed */
    val memoryType: MemoryType,
    /** Operation performed */
    val operation: MemoryOpType,
    /** File path accessed (for LTM file operations) */
    val filePath: String? = null,
    /** Search query (for QUERY operations) */
    val query: String? = null,
    /** Number of results returned */
    val resultCount: Int? = null,
    /** Preview of content (first ~100 chars) */
    val contentPreview: String? = null
)

@Serializable
enum class MemoryType {
    /** Short-term memory */
    STM,
    /** Long-term memory */
    LTM
}

@Serializable
enum class MemoryOpType {
    READ,
    WRITE,
    QUERY
}

package com.duchastel.simon.brainiac.logging

import com.duchastel.simon.brainiac.logging.models.*

/**
 * Interface for logging AI interactions with message deduplication and thread tracking.
 *
 * Messages are stored once per unique content (deduplicated by hash).
 * Threads are sequences of message references with positional metadata.
 *
 * Usage:
 * 1. Call startThread() at the beginning of a session
 * 2. Call logMessage() for each unique message content
 * 3. Call logThreadEntry() to record events in the thread
 * 4. Call logMemoryOperation() to record memory access
 */
interface InteractionLogger {

    // === Thread Lifecycle ===

    /**
     * Start a new thread for logging.
     * @param sessionId Optional custom session ID. If null, generates one.
     * @return The thread ID
     */
    fun startThread(sessionId: String? = null): String

    /**
     * Get the current active thread ID, if any.
     */
    fun currentThreadId(): String?

    // === Message Operations ===

    /**
     * Store a message and return its ID.
     * If an identical message exists, returns existing ID without re-storing.
     */
    fun logMessage(
        content: String,
        role: MessageRole,
        metadata: MessageMetadata? = null
    ): String

    /**
     * Retrieve a message by ID.
     */
    fun getMessage(id: String): LoggedMessage?

    // === Thread Entry Operations ===

    /**
     * Log a thread entry (event in the conversation).
     */
    fun logThreadEntry(entry: ThreadEntry)

    /**
     * Convenience: Log a message being added to context.
     */
    fun logMessageAdded(
        messageId: String,
        positionInContext: Int,
        contextSnapshot: ContextSnapshot? = null
    )

    /**
     * Convenience: Log context being replayed to LLM.
     * @param messageIds List of message IDs in context order
     */
    fun logContextReplayed(
        messageIds: List<String>,
        contextSnapshot: ContextSnapshot? = null
    )

    /**
     * Convenience: Log an LLM request being made.
     */
    fun logLLMRequest(contextSnapshot: ContextSnapshot? = null)

    /**
     * Convenience: Log an LLM response received.
     */
    fun logLLMResponse(
        responseMessageId: String,
        contextSnapshot: ContextSnapshot? = null
    )

    // === Memory Operations ===

    /**
     * Log a memory operation (read/write/query on STM or LTM).
     */
    fun logMemoryOperation(
        memoryType: MemoryType,
        operation: MemoryOpType,
        filePath: String? = null,
        query: String? = null,
        resultCount: Int? = null,
        contentPreview: String? = null
    )

    // === Query Operations ===

    /**
     * Get all entries in the current thread.
     */
    fun getCurrentThreadEntries(): List<ThreadEntry>

    /**
     * Get all entries in a specific thread.
     */
    fun getThreadEntries(threadId: String): List<ThreadEntry>

    /**
     * List available threads.
     */
    fun listThreads(limit: Int = 100): List<ThreadSummary>

    /**
     * Search for messages containing text.
     */
    fun searchMessages(query: String): List<LoggedMessage>

    companion object {
        /**
         * Create a no-op logger that doesn't actually log anything.
         * Useful for testing or when logging is disabled.
         */
        fun noOp(): InteractionLogger = NoOpInteractionLogger
    }
}

/**
 * No-op implementation for when logging is disabled.
 */
private object NoOpInteractionLogger : InteractionLogger {
    override fun startThread(sessionId: String?): String = "no-op"
    override fun currentThreadId(): String? = null
    override fun logMessage(content: String, role: MessageRole, metadata: MessageMetadata?): String = "no-op"
    override fun getMessage(id: String): LoggedMessage? = null
    override fun logThreadEntry(entry: ThreadEntry) {}
    override fun logMessageAdded(messageId: String, positionInContext: Int, contextSnapshot: ContextSnapshot?) {}
    override fun logContextReplayed(messageIds: List<String>, contextSnapshot: ContextSnapshot?) {}
    override fun logLLMRequest(contextSnapshot: ContextSnapshot?) {}
    override fun logLLMResponse(responseMessageId: String, contextSnapshot: ContextSnapshot?) {}
    override fun logMemoryOperation(
        memoryType: MemoryType,
        operation: MemoryOpType,
        filePath: String?,
        query: String?,
        resultCount: Int?,
        contentPreview: String?
    ) {}
    override fun getCurrentThreadEntries(): List<ThreadEntry> = emptyList()
    override fun getThreadEntries(threadId: String): List<ThreadEntry> = emptyList()
    override fun listThreads(limit: Int): List<ThreadSummary> = emptyList()
    override fun searchMessages(query: String): List<LoggedMessage> = emptyList()
}

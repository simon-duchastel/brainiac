package com.duchastel.simon.brainiac.logging

import com.duchastel.simon.brainiac.logging.config.InteractionLoggingConfig
import com.duchastel.simon.brainiac.logging.models.*
import com.duchastel.simon.brainiac.logging.store.MessageStore
import com.duchastel.simon.brainiac.logging.store.ThreadStore
import okio.FileSystem
import java.time.Instant

/**
 * Default implementation of InteractionLogger.
 * Uses MessageStore for deduplicated message storage and ThreadStore for thread tracking.
 */
class InteractionLoggerImpl(
    private val config: InteractionLoggingConfig = InteractionLoggingConfig(),
    fileSystem: FileSystem = FileSystem.SYSTEM
) : InteractionLogger {

    private val messageStore = MessageStore(config, fileSystem)
    private val threadStore = ThreadStore(config, fileSystem)

    private var _currentThreadId: String? = null

    // === Thread Lifecycle ===

    override fun startThread(sessionId: String?): String {
        val threadId = sessionId ?: threadStore.generateThreadId()
        threadStore.startThread(threadId)
        _currentThreadId = threadId
        return threadId
    }

    override fun currentThreadId(): String? = _currentThreadId

    // === Message Operations ===

    override fun logMessage(
        content: String,
        role: MessageRole,
        metadata: MessageMetadata?
    ): String {
        if (!config.enabled) return messageStore.computeMessageId(content, role)
        return messageStore.storeMessage(content, role, metadata)
    }

    override fun getMessage(id: String): LoggedMessage? {
        return messageStore.getMessage(id)
    }

    // === Thread Entry Operations ===

    override fun logThreadEntry(entry: ThreadEntry) {
        if (!config.enabled) return
        val threadId = _currentThreadId ?: return
        threadStore.appendEntry(threadId, entry)
    }

    override fun logMessageAdded(
        messageId: String,
        positionInContext: Int,
        contextSnapshot: ContextSnapshot?
    ) {
        logThreadEntry(
            ThreadEntry(
                timestamp = Instant.now().toString(),
                messageId = messageId,
                positionInContext = positionInContext,
                eventType = ThreadEventType.MESSAGE_ADDED,
                contextSnapshot = contextSnapshot
            )
        )
    }

    override fun logContextReplayed(
        messageIds: List<String>,
        contextSnapshot: ContextSnapshot?
    ) {
        messageIds.forEachIndexed { index, messageId ->
            logThreadEntry(
                ThreadEntry(
                    timestamp = Instant.now().toString(),
                    messageId = messageId,
                    positionInContext = index,
                    eventType = ThreadEventType.CONTEXT_REPLAYED,
                    contextSnapshot = if (index == 0) contextSnapshot else null // Only include snapshot once
                )
            )
        }
    }

    override fun logLLMRequest(contextSnapshot: ContextSnapshot?) {
        logThreadEntry(
            ThreadEntry(
                timestamp = Instant.now().toString(),
                eventType = ThreadEventType.LLM_REQUEST,
                contextSnapshot = contextSnapshot
            )
        )
    }

    override fun logLLMResponse(
        responseMessageId: String,
        contextSnapshot: ContextSnapshot?
    ) {
        logThreadEntry(
            ThreadEntry(
                timestamp = Instant.now().toString(),
                messageId = responseMessageId,
                eventType = ThreadEventType.LLM_RESPONSE,
                contextSnapshot = contextSnapshot
            )
        )
    }

    // === Memory Operations ===

    override fun logMemoryOperation(
        memoryType: MemoryType,
        operation: MemoryOpType,
        filePath: String?,
        query: String?,
        resultCount: Int?,
        contentPreview: String?
    ) {
        if (!config.enabled) return

        val eventType = when (operation) {
            MemoryOpType.READ -> ThreadEventType.MEMORY_READ
            MemoryOpType.WRITE -> ThreadEventType.MEMORY_WRITE
            MemoryOpType.QUERY -> ThreadEventType.MEMORY_QUERY
        }

        logThreadEntry(
            ThreadEntry(
                timestamp = Instant.now().toString(),
                eventType = eventType,
                memoryOperation = MemoryOperation(
                    memoryType = memoryType,
                    operation = operation,
                    filePath = filePath,
                    query = query,
                    resultCount = resultCount,
                    contentPreview = contentPreview?.take(100) // Limit preview length
                )
            )
        )
    }

    // === Query Operations ===

    override fun getCurrentThreadEntries(): List<ThreadEntry> {
        val threadId = _currentThreadId ?: return emptyList()
        return threadStore.readThread(threadId)
    }

    override fun getThreadEntries(threadId: String): List<ThreadEntry> {
        return threadStore.readThread(threadId)
    }

    override fun listThreads(limit: Int): List<ThreadSummary> {
        return threadStore.listThreadSummaries(limit)
    }

    override fun searchMessages(query: String): List<LoggedMessage> {
        // Simple implementation: iterate through all messages and filter
        // For production, would want full-text search indexing
        val queryLower = query.lowercase()
        return messageStore.listMessageIds()
            .mapNotNull { messageStore.getMessage(it) }
            .filter { it.content.lowercase().contains(queryLower) }
    }

    /**
     * Run cleanup based on retention policy.
     */
    fun runCleanup() {
        config.retentionDays?.let { days ->
            threadStore.cleanupOldThreads(days)
        }
    }
}

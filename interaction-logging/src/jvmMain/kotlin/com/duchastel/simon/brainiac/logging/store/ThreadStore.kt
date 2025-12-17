package com.duchastel.simon.brainiac.logging.store

import com.duchastel.simon.brainiac.logging.config.InteractionLoggingConfig
import com.duchastel.simon.brainiac.logging.models.ThreadEntry
import com.duchastel.simon.brainiac.logging.models.ThreadSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Store for thread logs.
 * Each thread is a JSONL file with one ThreadEntry per line.
 * Append-only for performance.
 */
class ThreadStore(
    private val config: InteractionLoggingConfig,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val json = Json {
        encodeDefaults = false
    }

    private val threadNameFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH-mm-ss")
        .withZone(ZoneOffset.UTC)

    init {
        ensureDirectoriesExist()
    }

    private fun ensureDirectoriesExist() {
        if (!fileSystem.exists(config.threadsDirectory)) {
            fileSystem.createDirectories(config.threadsDirectory)
        }
    }

    /**
     * Generate a thread ID based on current timestamp.
     */
    fun generateThreadId(): String {
        return "session-${threadNameFormatter.format(Instant.now())}"
    }

    /**
     * Get the file path for a thread.
     */
    private fun getThreadPath(threadId: String): Path {
        return config.threadsDirectory / "$threadId.jsonl"
    }

    /**
     * Start a new thread (creates the file).
     */
    fun startThread(threadId: String = generateThreadId()): String {
        val threadPath = getThreadPath(threadId)

        // Create empty file if it doesn't exist
        if (!fileSystem.exists(threadPath)) {
            fileSystem.write(threadPath) {
                // Empty file
            }
        }

        return threadId
    }

    /**
     * Append an entry to a thread.
     * Thread-safe via file append.
     */
    fun appendEntry(threadId: String, entry: ThreadEntry) {
        val threadPath = getThreadPath(threadId)

        // Ensure thread exists
        if (!fileSystem.exists(threadPath)) {
            startThread(threadId)
        }

        val line = json.encodeToString(entry) + "\n"

        fileSystem.appendingSink(threadPath).use { sink ->
            okio.Buffer().use { buffer ->
                buffer.writeUtf8(line)
                sink.write(buffer, buffer.size)
            }
        }
    }

    /**
     * Read all entries from a thread.
     */
    fun readThread(threadId: String): List<ThreadEntry> {
        val threadPath = getThreadPath(threadId)

        if (!fileSystem.exists(threadPath)) {
            return emptyList()
        }

        return fileSystem.read(threadPath) {
            val entries = mutableListOf<ThreadEntry>()
            while (true) {
                val line = readUtf8Line() ?: break
                if (line.isNotBlank()) {
                    try {
                        entries.add(json.decodeFromString<ThreadEntry>(line))
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
            entries
        }
    }

    /**
     * List all thread IDs.
     */
    fun listThreadIds(): List<String> {
        if (!fileSystem.exists(config.threadsDirectory)) {
            return emptyList()
        }

        return fileSystem.list(config.threadsDirectory)
            .filter { it.name.endsWith(".jsonl") }
            .map { it.name.removeSuffix(".jsonl") }
            .sortedDescending() // Most recent first
    }

    /**
     * Get summary information about a thread.
     */
    fun getThreadSummary(threadId: String): ThreadSummary? {
        val entries = readThread(threadId)
        if (entries.isEmpty()) {
            return null
        }

        val uniqueMessageIds = entries
            .mapNotNull { it.messageId }
            .toSet()

        return ThreadSummary(
            threadId = threadId,
            startTime = entries.first().timestamp,
            lastActivityTime = entries.last().timestamp,
            entryCount = entries.size,
            uniqueMessageCount = uniqueMessageIds.size
        )
    }

    /**
     * List thread summaries.
     */
    fun listThreadSummaries(limit: Int = 100): List<ThreadSummary> {
        return listThreadIds()
            .take(limit)
            .mapNotNull { getThreadSummary(it) }
    }

    /**
     * Check if a thread exists.
     */
    fun threadExists(threadId: String): Boolean {
        return fileSystem.exists(getThreadPath(threadId))
    }

    /**
     * Delete old threads based on retention policy.
     */
    fun cleanupOldThreads(retentionDays: Int) {
        val cutoff = Instant.now().minusSeconds(retentionDays.toLong() * 24 * 60 * 60)

        listThreadIds().forEach { threadId ->
            val summary = getThreadSummary(threadId)
            if (summary != null) {
                try {
                    val lastActivity = Instant.parse(summary.lastActivityTime)
                    if (lastActivity.isBefore(cutoff)) {
                        fileSystem.delete(getThreadPath(threadId))
                    }
                } catch (e: Exception) {
                    // Skip if timestamp can't be parsed
                }
            }
        }
    }
}

package com.duchastel.simon.brainiac.core.process.memory

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Actions that can be logged for memory access tracking.
 */
enum class AccessAction {
    READ,
    WRITE,
    MODIFY
}

/**
 * Represents a single access log entry.
 */
data class AccessEntry(
    val timestamp: Instant,
    val action: AccessAction,
    val filePath: String,
) {
    /**
     * Formats the entry as a log line.
     * Format: \[ISO_8601_TIMESTAMP] | [AccessAction] | ABSOLUTE_FILE_PATH
     */
    fun toLogLine(): String = "[$timestamp] | $action | $filePath"

    companion object {
        /**
         * Parses a log line into an AccessEntry.
         */
        fun fromLogLine(line: String): AccessEntry? {
            val parts = line.split(" | ")
            if (parts.size != 3) return null

            val timestamp = parts[0].trim('[', ']')
            val action = parts[1].trim()
            val filePath = parts[2].trim()

            return try {
                AccessEntry(
                    timestamp = Instant.parse(timestamp),
                    action = AccessAction.valueOf(action),
                    filePath = filePath
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Represents the complete access log.
 */
data class AccessLog(
    val entries: List<AccessEntry> = emptyList()
) {
    /**
     * Groups entries by file path.
     */
    fun groupByFile(): Map<String, List<AccessEntry>> {
        return entries.groupBy { it.filePath }
    }

    /**
     * Filters entries by action type.
     */
    fun filterByAction(action: AccessAction): List<AccessEntry> {
        return entries.filter { it.action == action }
    }

    /**
     * Gets entries within a time range.
     */
    fun filterByTimeRange(start: Instant, end: Instant): List<AccessEntry> {
        return entries.filter { it.timestamp >= start && it.timestamp <= end }
    }
}

/**
 * Repository for managing access log persistence.
 *
 * Handles logging of memory access operations (READ, WRITE, MODIFY) to track
 * usage patterns for the Organization process.
 */
class AccessLogRepository(
    brainiacRootDirectory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val logFilePath: Path = defaultLogPath(brainiacRootDirectory)
    private val archiveDirectory: Path = defaultArchiveDirectory(brainiacRootDirectory)

    /**
     * Logs a memory access operation.
     *
     * @param action The type of access (READ, WRITE, or MODIFY)
     * @param filePath The absolute path to the file being accessed
     */
    fun logAccess(action: AccessAction, filePath: String) {
        val entry = AccessEntry(
            timestamp = Clock.System.now(),
            action = action,
            filePath = filePath
        )

        synchronized(this) {
            fileSystem.createDirectories(logFilePath.parent!!)

            // Read existing content if file exists
            val existingContent = if (fileSystem.exists(logFilePath)) {
                fileSystem.read(logFilePath) { readUtf8() }
            } else {
                ""
            }

            // Append new entry
            fileSystem.write(logFilePath) {
                writeUtf8(existingContent)
                writeUtf8(entry.toLogLine())
                writeUtf8("\n")
            }
        }
    }

    /**
     * Reads and parses the entire access log.
     *
     * @return AccessLog containing all valid entries
     */
    fun readAccessLog(): AccessLog {
        if (!fileSystem.exists(logFilePath)) {
            return AccessLog(emptyList())
        }

        val entries = fileSystem.read(logFilePath) {
            generateSequence { readUtf8Line() }
                .mapNotNull { AccessEntry.fromLogLine(it) }
                .toList()
        }

        return AccessLog(entries)
    }

    /**
     * Archives the current log file and clears it.
     *
     * Moves the log to logs/archive/access-{timestamp}.log and creates
     * a new empty log file.
     */
    fun archiveAndClear() {
        if (!fileSystem.exists(logFilePath)) {
            return
        }

        synchronized(this) {
            val timestamp = Clock.System.now().toString().replace(':', '-')
            val archivePath = archiveDirectory / "access-$timestamp.log"

            fileSystem.createDirectories(archiveDirectory)
            fileSystem.atomicMove(logFilePath, archivePath)
        }
    }

    /**
     * Checks if the access log is empty.
     */
    fun isEmpty(): Boolean {
        return !fileSystem.exists(logFilePath) ||
                fileSystem.metadata(logFilePath).size == 0L
    }

    companion object {
        /**
         * Returns the default path for the access log file.
         *
         * @return Path to brainiacRootDirectory/logs/access.log
         */
        private fun defaultLogPath(brainiacRootDirectory: Path): Path {
            return brainiacRootDirectory / "logs" / "access.log"
        }

        /**
         * Returns the default path for the log archive directory.
         *
         * @return Path to brainiacRootDirectory/logs/archive/
         */
        private fun defaultArchiveDirectory(brainiacRootDirectory: Path): Path {
            return brainiacRootDirectory / "logs" / "archive"
        }
    }
}

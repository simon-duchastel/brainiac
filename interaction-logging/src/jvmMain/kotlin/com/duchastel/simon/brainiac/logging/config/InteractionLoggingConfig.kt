package com.duchastel.simon.brainiac.logging.config

import okio.Path
import okio.Path.Companion.toPath

/**
 * Configuration for the interaction logging system.
 */
data class InteractionLoggingConfig(
    /** Whether logging is enabled */
    val enabled: Boolean = true,
    /** Base directory for all interaction logs */
    val logDirectory: Path = "logs/interactions".toPath(),
    /** Retention period in days (null = keep forever) */
    val retentionDays: Int? = null,
    /** Whether to log full context on every LLM request */
    val logContextOnEveryRequest: Boolean = true,
    /** Whether to include token count estimates */
    val includeTokenEstimates: Boolean = true,
    /** Whether to shard message files into subdirectories (for scale) */
    val shardMessages: Boolean = false
) {
    /** Directory for individual message files */
    val messagesDirectory: Path get() = logDirectory / "messages"

    /** Directory for thread JSONL files */
    val threadsDirectory: Path get() = logDirectory / "threads"
}

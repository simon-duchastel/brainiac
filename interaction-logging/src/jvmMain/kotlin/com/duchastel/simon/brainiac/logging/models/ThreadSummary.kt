package com.duchastel.simon.brainiac.logging.models

import kotlinx.serialization.Serializable

/**
 * Summary information about a thread for listing purposes.
 */
@Serializable
data class ThreadSummary(
    /** Thread identifier (typically session timestamp) */
    val threadId: String,
    /** When the thread started */
    val startTime: String,
    /** When the last entry was added */
    val lastActivityTime: String,
    /** Total number of entries in the thread */
    val entryCount: Int,
    /** Number of unique messages referenced */
    val uniqueMessageCount: Int
)

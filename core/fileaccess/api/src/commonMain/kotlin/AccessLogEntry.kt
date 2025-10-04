@file:OptIn(ExperimentalTime::class)

package com.brainiac.core.fileaccess

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class AccessLogEntry(
    @Contextual val timestamp: Instant,
    val action: String,
    val path: String,
    val details: String? = null
)

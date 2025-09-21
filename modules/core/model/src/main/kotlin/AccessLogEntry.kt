package com.brainiac.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant

@Serializable
data class AccessLogEntry(
    @Contextual val timestamp: Instant,
    val action: String,
    val path: String,
    val details: String? = null
)
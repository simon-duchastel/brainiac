package com.braniac.core.model

import java.time.Instant

data class AccessLogEntry(
    val timestamp: Instant,
    val action: String,
    val path: String,
    val details: String? = null
)
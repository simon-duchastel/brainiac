package com.duchastel.simon.brainiac.core.fileaccess

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.datetime.Instant

@Serializable
data class AccessLogEntry(
    @Contextual val timestamp: Instant,
    val action: String,
    val path: String,
    val details: String? = null
)

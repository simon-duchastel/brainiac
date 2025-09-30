package com.brainiac.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CoreIdentity(
    val content: String,
)
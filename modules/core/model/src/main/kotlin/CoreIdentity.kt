package com.brainiac.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CoreIdentity(
    val name: String,
    val role: String,
    val personality: String,
    val capabilities: List<String>,
    val limitations: List<String>
)
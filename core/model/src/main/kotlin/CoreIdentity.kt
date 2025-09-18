package com.braniac.core.model

data class CoreIdentity(
    val name: String,
    val role: String,
    val personality: String,
    val capabilities: List<String>,
    val limitations: List<String>
)
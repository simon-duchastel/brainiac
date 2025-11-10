package com.duchastel.simon.brainiac.cli.models

data class ToolActivity(
    val toolName: String,
    val summary: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

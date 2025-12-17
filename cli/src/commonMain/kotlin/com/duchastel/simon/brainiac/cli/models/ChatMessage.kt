package com.duchastel.simon.brainiac.cli.models

enum class MessageSender { USER, BRAINIAC }

data class ChatMessage(
    val content: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis()
)

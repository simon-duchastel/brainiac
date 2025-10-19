package com.duchastel.simon.brainiac.cli

import com.duchastel.simon.brainiac.core.process.CoreAgent
import com.duchastel.simon.brainiac.core.process.callbacks.AgentEvent
import com.duchastel.simon.brainiac.core.process.tools.ToolUse

fun main() {
    println("=== Brainiac AI Memory System ===")
    println("Type 'exit' or 'quit' to exit")
    println()

    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")

    val coreAgent = CoreAgent(apiKey) { event ->
        when (event) {
            is AgentEvent.AssistantMessage -> {
                println("Assistant: ${event.content}")
            }
            is AgentEvent.ToolCall -> {
                val toolName = when (event.tool) {
                    ToolUse.StoreShortTermMemory -> "store_short_term_memory"
                    ToolUse.StoreLongTermMemory -> "store_long_term_memory"
                }
                println("[Tool Call: $toolName]")
            }
            is AgentEvent.ToolResult -> {
                val toolName = when (event.tool) {
                    ToolUse.StoreShortTermMemory -> "store_short_term_memory"
                    ToolUse.StoreLongTermMemory -> "store_long_term_memory"
                }
                val status = if (event.success) "SUCCESS" else "FAILED"
                println("[Tool Result: $toolName - $status]")
            }
        }
    }

    print("> ")
    val input = readlnOrNull()?.trim()

    if (input.isNullOrBlank()) {
        println("No input received, exiting...")
        return
    }

    if (input.lowercase() in listOf("exit", "quit")) {
        println("Goodbye!")
        return
    }

    try {
        coreAgent.run(input)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}

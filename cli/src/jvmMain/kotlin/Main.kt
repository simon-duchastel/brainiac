package com.duchastel.simon.brainiac.cli

import com.duchastel.simon.brainiac.core.process.CoreAgent
import com.duchastel.simon.brainiac.core.process.callbacks.AgentEvent
import com.duchastel.simon.brainiac.core.process.callbacks.ToolUse
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import okio.Path.Companion.toPath

fun main() {
    println("===== Brainiac AI =====")
    println("Type 'exit' or 'quit' to exit")
    println()

    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")

    val brainiacRootDirectory = "~/.brainiac/".toPath()

    val shortTermMemoryRepository = ShortTermMemoryRepository(
        brainiacRootDirectory = brainiacRootDirectory
    )
    val longTermMemoryRepository = LongTermMemoryRepository(
        brainiacRootDirectory = brainiacRootDirectory
    )
    val coreAgent = CoreAgent(
        googleApiKey = apiKey,
        shortTermMemoryRepository = shortTermMemoryRepository,
        longTermMemoryRepository = longTermMemoryRepository
    ) { event ->
        when (event) {
            is AgentEvent.AssistantMessage -> {
                println(event.content)
            }
            is AgentEvent.ToolCall -> {
                val toolUseMessage = when (event.tool) {
                    ToolUse.StoreShortTermMemory -> "Updating short term memory..."
                    ToolUse.StoreLongTermMemory -> "Updating long term memory..."
                }
                println(toolUseMessage)
            }
            is AgentEvent.ToolResult -> {
                val toolResultMessage = when (event.tool) {
                    ToolUse.StoreShortTermMemory -> "Done updating short term memory!"
                    ToolUse.StoreLongTermMemory -> "Done updating long term memory!"
                }
                val status = if (event.success) "SUCCESS" else "FAILED"
                println("$toolResultMessage ($status)")
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

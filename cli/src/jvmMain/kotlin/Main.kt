package com.duchastel.simon.brainiac.cli

import com.duchastel.simon.brainiac.core.process.CoreAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("=== Brainiac AI Memory System ===")
    println("Type 'exit' or 'quit' to exit")
    println()

    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")

    val coreAgent = CoreAgent(apiKey)

    print("> ")
    val input = readlnOrNull()?.trim()

    if (input.isNullOrBlank()) {
        println("No input received, exiting...")
        return@runBlocking
    }

    if (input.lowercase() in listOf("exit", "quit")) {
        println("Goodbye!")
        return@runBlocking
    }

    try {
        coreAgent.run(input)
            .flowOn(Dispatchers.Default)
            .collect {
                println(it)
            }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}

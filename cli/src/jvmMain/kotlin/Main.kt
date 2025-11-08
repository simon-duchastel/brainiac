package com.duchastel.simon.brainiac.cli

import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.duchastel.simon.brainiac.core.process.CoreAgent
import com.duchastel.simon.brainiac.core.process.CoreAgentContext
import com.duchastel.simon.brainiac.core.process.callbacks.AgentEvent
import com.duchastel.simon.brainiac.core.process.callbacks.ToolUse
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import okio.Path.Companion.toPath

fun main() {
    println("===== Brainiac AI =====")
    println("Type 'exit' or 'quit' to exit")
    println()

    val googleApiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")
    val openRouterApiKey = System.getenv("OPEN_ROUTER_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")

    val brainiacRootDirectory = "~/.brainiac/".toPath()

    val shortTermMemoryRepository = ShortTermMemoryRepository(
        brainiacRootDirectory = brainiacRootDirectory
    )
    val longTermMemoryRepository = LongTermMemoryRepository(
        brainiacRootDirectory = brainiacRootDirectory
    )

    val stealthModel = LLModel(
        provider = LLMProvider.OpenRouter,
        id = "openrouter/polaris-alpha",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.Completion
        ),
        contextLength = 256_000,
    )

    val coreAgentContext = CoreAgentContext(
        highThoughtModel = stealthModel,
        mediumThoughtModel =  stealthModel,
        lowThoughtModel = stealthModel,
        executionClients = mapOf(
            LLMProvider.Google to GoogleLLMClient(googleApiKey),
            LLMProvider.OpenRouter to OpenRouterLLMClient(openRouterApiKey),
        ),
    )
    val coreAgent = CoreAgent(
        coreAgentContext = coreAgentContext,
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

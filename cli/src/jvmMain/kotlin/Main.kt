package com.duchastel.simon.brainiac.cli

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.ext.tool.file.WriteFileTool
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.rag.base.files.JVMFileSystemProvider
import com.duchastel.simon.brainiac.agent.CoreAgent
import com.duchastel.simon.brainiac.agent.CoreAgentConfig
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.tools.bash.BashTool
import com.duchastel.simon.brainiac.tools.websearch.WebSearchTool
import okio.Path.Companion.toPath

fun main() {
    println("===== Brainiac AI =====")
    println("Type 'exit' or 'quit' to exit")
    println()

    val googleApiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("GOOGLE_API_KEY environment variable not set")
    val openRouterApiKey = System.getenv("OPEN_ROUTER_API_KEY")
        ?: error("OPEN_ROUTER_API_KEY environment variable not set")
    val tavilyApiKey = System.getenv("TAVILY_API_KEY")

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

    val toolRegistry = ToolRegistry {
        if (tavilyApiKey != null) {
            println("Web search enabled via Tavily API")
            tool(WebSearchTool(apiKey = tavilyApiKey, maxResults = 5))
        } else {
            println("Web search disabled (TAVILY_API_KEY not set)")
        }
        tool(BashTool())

        tool(ListDirectoryTool(JVMFileSystemProvider.ReadWrite))
        tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
        tool(ReadFileTool(JVMFileSystemProvider.ReadWrite))
        tool(WriteFileTool(JVMFileSystemProvider.ReadWrite))
    }
    val coreAgent = CoreAgent(
        config = CoreAgentConfig(
            highThoughtModel = stealthModel,
            mediumThoughtModel = GoogleModels.Gemini2_5Flash,
            lowThoughtModel = GoogleModels.Gemini2_5FlashLite,
            executionClients = mapOf(
                LLMProvider.Google to GoogleLLMClient(googleApiKey),
                LLMProvider.OpenRouter to OpenRouterLLMClient(openRouterApiKey),
            ),
            toolRegistry = toolRegistry,
            onEventHandler = { messages ->
                messages.forEach { message ->
                    when (message) {
                        is Message.Assistant -> {
                            println(message.content)
                        }

                        is Message.Tool.Call -> {
                            val toolUseMessage = when (message.tool) {
                                "store_short_term_memory" -> "Updating short term memory..."
                                "store_long_term_memory" -> "Updating long term memory..."
                                else -> return@forEach
                            }
                            println(toolUseMessage)
                        }

                        else -> {} // Ignore other message types
                    }
                }
            }
        ),
        shortTermMemoryRepository = shortTermMemoryRepository,
        longTermMemoryRepository = longTermMemoryRepository
    )

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

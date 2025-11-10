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
import com.duchastel.simon.brainiac.agent.UserMessage
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.tools.bash.BashTool
import com.duchastel.simon.brainiac.tools.talk.TalkTool
import com.duchastel.simon.brainiac.tools.websearch.WebSearchTool
import okio.Path.Companion.toPath

fun main(args: Array<String>) {
    // Configure logging before any other code runs
    val enableLogging = args.contains("--logging")
    configureLogging(enableLogging)

    println("===== Brainiac AI =====")
    if (enableLogging) {
        println("Verbose logging enabled")
    }
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
        tool(TalkTool { message ->
            println("Brainiac: $message")
        })

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
                            println("**Thinking**... ${message.content}")
                        }
                        is Message.Tool.Call -> {
                            when (message.tool) {
                                BashTool().name -> {
                                    println("Running on cmd: ${message.content}")
                                }
                                ListDirectoryTool(JVMFileSystemProvider.ReadWrite).name -> {
                                    println("Listing directory: ${message.content}")
                                }
                                EditFileTool(JVMFileSystemProvider.ReadWrite).name -> {
                                    println("Editing file: ${message.content}")
                                }
                                ReadFileTool(JVMFileSystemProvider.ReadWrite).name -> {
                                    println("Reading file: ${message.content}")
                                }
                                WriteFileTool(JVMFileSystemProvider.ReadWrite).name -> {
                                    println("Writing File: ${message.content}")
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        ),
        shortTermMemoryRepository = shortTermMemoryRepository,
        longTermMemoryRepository = longTermMemoryRepository,
        awaitUserMessage = {
            print("> ")
            val input = readlnOrNull()?.trim()

            val userMessage = when {
                input.isNullOrBlank() -> {
                    println("No input received, exiting...")
                    UserMessage.Stop
                }
                input.lowercase() in listOf("exit", "quit") -> {
                    println("Goodbye!")
                    UserMessage.Stop
                }
                else -> {
                    UserMessage.Message(input)
                }
            }

            userMessage
        }
    )

    try {
        coreAgent.run()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Configure logging levels for the application.
 * By default, Koog logging is set to WARN to reduce verbosity.
 * When enableLogging is true, all logging is set to INFO.
 */
private fun configureLogging(enableLogging: Boolean) {
    if (enableLogging) {
        // Enable verbose logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    } else {
        // Suppress verbose Koog logging by default
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")
        System.setProperty("org.slf4j.simpleLogger.log.ai.koog", "WARN")
    }
}

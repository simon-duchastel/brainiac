package com.duchastel.simon.brainiac.server.service

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
import com.duchastel.simon.brainiac.server.model.QueryEvent
import com.duchastel.simon.brainiac.tools.bash.BashTool
import com.duchastel.simon.brainiac.tools.websearch.WebSearchTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

/**
 * Configuration for the AgentRunner.
 */
data class AgentRunnerConfig(
    val googleApiKey: String,
    val openRouterApiKey: String,
    val tavilyApiKey: String?,
    val brainiacRootDirectory: String = "~/.brainiac/"
)

/**
 * Wraps CoreAgent execution and integrates with QueryExecutionService.
 */
class AgentRunner(
    private val config: AgentRunnerConfig,
    private val queryExecutionService: QueryExecutionService
) {
    private val executionScope = CoroutineScope(Dispatchers.Default)

    private val shortTermMemoryRepository = ShortTermMemoryRepository(
        brainiacRootDirectory = config.brainiacRootDirectory.toPath()
    )

    private val longTermMemoryRepository = LongTermMemoryRepository(
        brainiacRootDirectory = config.brainiacRootDirectory.toPath()
    )

    private val stealthModel = LLModel(
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

    private val toolRegistry = ToolRegistry {
        config.tavilyApiKey?.let { apiKey ->
            tool(WebSearchTool(apiKey = apiKey, maxResults = 5))
        }
        tool(BashTool())
        tool(ListDirectoryTool(JVMFileSystemProvider.ReadWrite))
        tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
        tool(ReadFileTool(JVMFileSystemProvider.ReadWrite))
        tool(WriteFileTool(JVMFileSystemProvider.ReadWrite))
    }

    /**
     * Executes a query asynchronously and streams events through the QueryExecutionService.
     */
    fun executeQuery(queryId: String, query: String) {
        executionScope.launch {
            try {
                queryExecutionService.publishEvent(QueryEvent.Started(queryId, query))

                val coreAgent = CoreAgent(
                    config = CoreAgentConfig(
                        highThoughtModel = stealthModel,
                        mediumThoughtModel = GoogleModels.Gemini2_5Flash,
                        lowThoughtModel = GoogleModels.Gemini2_5FlashLite,
                        executionClients = mapOf(
                            LLMProvider.Google to GoogleLLMClient(config.googleApiKey),
                            LLMProvider.OpenRouter to OpenRouterLLMClient(config.openRouterApiKey),
                        ),
                        toolRegistry = toolRegistry,
                        onEventHandler = { messages ->
                            executionScope.launch {
                                messages.forEach { message ->
                                    when (message) {
                                        is Message.Assistant -> {
                                            queryExecutionService.publishEvent(
                                                QueryEvent.AgentMessage(message.content)
                                            )
                                        }

                                        is Message.Tool.Call -> {
                                            queryExecutionService.publishEvent(
                                                QueryEvent.ToolCall(
                                                    tool = message.tool,
                                                    arguments = "{}" // TODO: Extract actual arguments
                                                )
                                            )
                                        }

                                        else -> {} // Ignore other message types
                                    }
                                }
                            }
                        }
                    ),
                    shortTermMemoryRepository = shortTermMemoryRepository,
                    longTermMemoryRepository = longTermMemoryRepository
                )

                coreAgent.run(query)
                queryExecutionService.completeQuery("Query completed successfully")

            } catch (e: Exception) {
                queryExecutionService.failQuery(e.message ?: "Unknown error occurred")
            }
        }
    }
}

package com.duchastel.simon.brainiac.cli.circuit

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.ext.tool.file.WriteFileTool
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.rag.base.files.JVMFileSystemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.duchastel.simon.brainiac.agent.CoreAgent
import com.duchastel.simon.brainiac.agent.CoreAgentConfig
import com.duchastel.simon.brainiac.agent.UserMessage
import com.duchastel.simon.brainiac.cli.models.ChatMessage
import com.duchastel.simon.brainiac.cli.models.MessageSender
import com.duchastel.simon.brainiac.cli.models.ToolActivity
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.tools.bash.BashTool
import com.duchastel.simon.brainiac.tools.talk.TalkTool
import com.duchastel.simon.brainiac.tools.websearch.WebSearchTool
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class BrainiacPresenter(
    private val openRouterApiKey: String,
    private val tavilyApiKey: String?,
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository
) : Presenter<BrainiacState> {

    val mainModel = LLModel(
        provider = LLMProvider.OpenRouter,
        id = "x-ai/grok-4.1-fast:free",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.Completion
        ),
        contextLength = 256_000,
    )

    @Composable
    override fun present(): BrainiacState {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var thinking by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isThinkingExpanded by remember { mutableStateOf(false) }
    var toolActivities by remember { mutableStateOf<List<ToolActivity>>(emptyList()) }
    var showToolDetails by remember { mutableStateOf(false) }
    var isWaitingForResponse by remember { mutableStateOf(false) }
    var loadingDots by remember { mutableStateOf(0) }
    var inputBuffer by remember { mutableStateOf("") }
    var shouldExit by remember { mutableStateOf(false) }

    val userInputChannel = remember { Channel<String>(Channel.UNLIMITED) }

    // Animation for loading dots
    LaunchedEffect(isThinking, isWaitingForResponse) {
        while (isThinking || isWaitingForResponse) {
            delay(500)
            loadingDots = (loadingDots + 1) % 4
        }
    }

    // Setup CoreAgent
    LaunchedEffect(Unit) {
        val toolRegistry = ToolRegistry {
            tool(TalkTool { message ->
                messages = messages + ChatMessage(
                    content = message,
                    sender = MessageSender.BRAINIAC
                )
                isWaitingForResponse = false
                isThinking = false
            })

            if (tavilyApiKey != null) {
                tool(WebSearchTool(apiKey = tavilyApiKey, maxResults = 5))
            }
            tool(BashTool())
            tool(ListDirectoryTool(JVMFileSystemProvider.ReadWrite))
            tool(EditFileTool(JVMFileSystemProvider.ReadWrite))
            tool(ReadFileTool(JVMFileSystemProvider.ReadWrite))
            tool(WriteFileTool(JVMFileSystemProvider.ReadWrite))
        }

        val coreAgent = CoreAgent(
            config = CoreAgentConfig(
                highThoughtModel = mainModel,
                mediumThoughtModel = mainModel,
                lowThoughtModel = mainModel,
                executionClients = mapOf(
                    LLMProvider.OpenRouter to OpenRouterLLMClient(openRouterApiKey),
                ),
                toolRegistry = toolRegistry,
                onEventHandler = { messagesFromAgent ->
                    messagesFromAgent.forEach { message ->
                        when (message) {
                            is Message.Assistant -> {
                                thinking = message.content
                                isThinking = true
                            }
                            is Message.Tool.Call -> {
                                val toolName = message.tool
                                val content = message.content
                                val activity = when (toolName) {
                                    BashTool().name -> ToolActivity(
                                        toolName = "Bash",
                                        summary = "Running command",
                                        details = content
                                    )
                                    ListDirectoryTool(JVMFileSystemProvider.ReadWrite).name -> ToolActivity(
                                        toolName = "ListDirectory",
                                        summary = "Listing directory",
                                        details = content
                                    )
                                    EditFileTool(JVMFileSystemProvider.ReadWrite).name -> ToolActivity(
                                        toolName = "EditFile",
                                        summary = "Editing file",
                                        details = content
                                    )
                                    ReadFileTool(JVMFileSystemProvider.ReadWrite).name -> ToolActivity(
                                        toolName = "ReadFile",
                                        summary = "Reading file",
                                        details = content
                                    )
                                    WriteFileTool(JVMFileSystemProvider.ReadWrite).name -> ToolActivity(
                                        toolName = "WriteFile",
                                        summary = "Writing file",
                                        details = content
                                    )
                                    else -> ToolActivity(
                                        toolName = toolName,
                                        summary = "Using tool",
                                        details = content
                                    )
                                }
                                toolActivities = (toolActivities + activity).takeLast(10)
                            }
                            else -> {}
                        }
                    }
                }
            ),
            shortTermMemoryRepository = shortTermMemoryRepository,
            longTermMemoryRepository = longTermMemoryRepository,
            awaitUserMessage = {
                val input = userInputChannel.receive()
                when {
                    input.lowercase() in listOf("exit", "quit") -> {
                        shouldExit = true
                        UserMessage.Stop
                    }
                    else -> {
                        messages = messages + ChatMessage(
                            content = input,
                            sender = MessageSender.USER
                        )
                        isWaitingForResponse = true
                        UserMessage.Message(input)
                    }
                }
            }
        )

        try {
            coreAgent.run()
        } catch (e: Exception) {
            messages = messages + ChatMessage(
                content = "Error: ${e.message}",
                sender = MessageSender.BRAINIAC
            )
        }
    }

    return BrainiacState(
        messages = messages,
        thinking = thinking,
        isThinking = isThinking,
        isThinkingExpanded = isThinkingExpanded,
        toolActivities = toolActivities,
        showToolDetails = showToolDetails,
        isWaitingForResponse = isWaitingForResponse,
        loadingDots = loadingDots,
        inputBuffer = inputBuffer,
        shouldExit = shouldExit,
        eventSink = { event ->
            when (event) {
                is BrainiacEvent.SendMessage -> {
                    if (!isWaitingForResponse && event.message.isNotBlank()) {
                        userInputChannel.trySend(event.message)
                        inputBuffer = ""
                    }
                }
                BrainiacEvent.ToggleThinking -> {
                    isThinkingExpanded = !isThinkingExpanded
                }
                BrainiacEvent.ToggleToolDetails -> {
                    showToolDetails = !showToolDetails
                }
                is BrainiacEvent.AppendToInput -> {
                    if (!isWaitingForResponse) {
                        inputBuffer += event.char
                    }
                }
                BrainiacEvent.BackspaceInput -> {
                    if (!isWaitingForResponse && inputBuffer.isNotEmpty()) {
                        inputBuffer = inputBuffer.dropLast(1)
                    }
                }
                BrainiacEvent.SubmitInput -> {
                    if (!isWaitingForResponse && inputBuffer.isNotBlank()) {
                        val trimmed = inputBuffer.trim()
                        if (trimmed.lowercase() in listOf("exit", "quit")) {
                            shouldExit = true
                            userInputChannel.trySend(trimmed)
                        } else {
                            userInputChannel.trySend(trimmed)
                        }
                        inputBuffer = ""
                    }
                }
                BrainiacEvent.ExitApp -> {
                    shouldExit = true
                    userInputChannel.trySend("exit")
                }
            }
        }
    )
}
}

package com.duchastel.simon.brainiac.cli

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.ext.tool.file.WriteFileTool
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.rag.base.files.JVMFileSystemProvider
import androidx.compose.runtime.*
import com.duchastel.simon.brainiac.agent.CoreAgent
import com.duchastel.simon.brainiac.agent.CoreAgentConfig
import com.duchastel.simon.brainiac.agent.UserMessage
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.tools.bash.BashTool
import com.duchastel.simon.brainiac.tools.talk.TalkTool
import com.duchastel.simon.brainiac.tools.websearch.WebSearchTool
import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okio.Path.Companion.toPath
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// Data Models
// ============================================================================

enum class MessageSender { USER, BRAINIAC }

data class ChatMessage(
    val content: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToolActivity(
    val toolName: String,
    val summary: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class UIState(
    val messages: List<ChatMessage> = emptyList(),
    val thinking: String = "",
    val isThinking: Boolean = false,
    val isThinkingExpanded: Boolean = false,
    val toolActivities: List<ToolActivity> = emptyList(),
    val showToolDetails: Boolean = false,
    val inputText: String = "",
    val isWaitingForResponse: Boolean = false,
    val loadingDots: Int = 0
)

// ============================================================================
// Main Entry Point
// ============================================================================

suspend fun main() {
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

    runMosaic {
        BrainiacTUI(
            googleApiKey = googleApiKey,
            openRouterApiKey = openRouterApiKey,
            tavilyApiKey = tavilyApiKey,
            stealthModel = stealthModel,
            shortTermMemoryRepository = shortTermMemoryRepository,
            longTermMemoryRepository = longTermMemoryRepository
        )
    }
}

// ============================================================================
// Main TUI Component
// ============================================================================

@Composable
fun BrainiacTUI(
    googleApiKey: String,
    openRouterApiKey: String,
    tavilyApiKey: String?,
    stealthModel: LLModel,
    shortTermMemoryRepository: ShortTermMemoryRepository,
    longTermMemoryRepository: LongTermMemoryRepository
) {
    var uiState by remember { mutableStateOf(UIState()) }
    val userInputChannel = remember { Channel<String>(Channel.UNLIMITED) }
    val scope = rememberCoroutineScope()

    // Animation for loading dots
    LaunchedEffect(uiState.isThinking, uiState.isWaitingForResponse) {
        while (uiState.isThinking || uiState.isWaitingForResponse) {
            delay(500)
            uiState = uiState.copy(loadingDots = (uiState.loadingDots + 1) % 4)
        }
    }

    // Setup CoreAgent
    LaunchedEffect(Unit) {
        val toolRegistry = ToolRegistry {
            tool(TalkTool { message ->
                uiState = uiState.copy(
                    messages = uiState.messages + ChatMessage(
                        content = message,
                        sender = MessageSender.BRAINIAC
                    ),
                    isWaitingForResponse = false,
                    isThinking = false
                )
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
                highThoughtModel = stealthModel,
                mediumThoughtModel = stealthModel,
                lowThoughtModel = stealthModel,
                executionClients = mapOf(
                    LLMProvider.Google to GoogleLLMClient(googleApiKey),
                    LLMProvider.OpenRouter to OpenRouterLLMClient(openRouterApiKey),
                ),
                toolRegistry = toolRegistry,
                onEventHandler = { messages ->
                    messages.forEach { message ->
                        when (message) {
                            is Message.Assistant -> {
                                uiState = uiState.copy(
                                    thinking = message.content.firstOrNull()?.text ?: "",
                                    isThinking = true
                                )
                            }
                            is Message.Tool.Call -> {
                                val toolName = message.tool
                                val content = message.content.firstOrNull()?.text ?: ""
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
                                uiState = uiState.copy(
                                    toolActivities = (uiState.toolActivities + activity).takeLast(10)
                                )
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
                    input.lowercase() in listOf("exit", "quit") -> UserMessage.Stop
                    else -> {
                        uiState = uiState.copy(
                            messages = uiState.messages + ChatMessage(
                                content = input,
                                sender = MessageSender.USER
                            ),
                            inputText = "",
                            isWaitingForResponse = true
                        )
                        UserMessage.Message(input)
                    }
                }
            }
        )

        try {
            coreAgent.run()
        } catch (e: Exception) {
            uiState = uiState.copy(
                messages = uiState.messages + ChatMessage(
                    content = "Error: ${e.message}",
                    sender = MessageSender.BRAINIAC
                )
            )
        }
    }

    // Terminal input handling in background
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            while (true) {
                try {
                    val line = readlnOrNull() ?: break
                    val trimmed = line.trim()

                    when {
                        trimmed.equals("t", ignoreCase = true) -> {
                            uiState = uiState.copy(isThinkingExpanded = !uiState.isThinkingExpanded)
                        }
                        trimmed.equals("a", ignoreCase = true) -> {
                            uiState = uiState.copy(showToolDetails = !uiState.showToolDetails)
                        }
                        trimmed.isNotEmpty() && !uiState.isWaitingForResponse -> {
                            userInputChannel.send(trimmed)
                        }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    // Render UI
    Column {
        HeaderPanel()
        Spacer()
        ThinkingPanel(uiState.thinking, uiState.isThinking, uiState.isThinkingExpanded, uiState.loadingDots)
        Spacer()
        ToolActivityPanel(uiState.toolActivities, uiState.showToolDetails)
        Spacer()
        ConversationPanel(uiState.messages.takeLast(10))
        Spacer()
        StatusPanel(uiState.isWaitingForResponse, uiState.loadingDots)
        Spacer()
        InputPanel(uiState.inputText, uiState.isWaitingForResponse)
        FooterPanel()
    }
}

// ============================================================================
// UI Components
// ============================================================================

@Composable
fun HeaderPanel() {
    Box(
        style = Style(
            foreground = Color.BrightCyan,
            bold = true
        )
    ) {
        Text("╔════════════════════════════════════════════════════════════════════════╗")
    }
    Box(
        style = Style(
            foreground = Color.BrightCyan,
            bold = true
        )
    ) {
        Text("║                          🧠 BRAINIAC AI                                ║")
    }
    Box(
        style = Style(
            foreground = Color.BrightCyan,
            bold = true
        )
    ) {
        Text("╚════════════════════════════════════════════════════════════════════════╝")
    }
}

@Composable
fun ThinkingPanel(thinking: String, isThinking: Boolean, isExpanded: Boolean, dots: Int) {
    if (!isThinking && thinking.isEmpty()) return

    val dotsText = ".".repeat(dots)
    Box(
        style = Style(
            foreground = Color.Yellow
        )
    ) {
        Text("💭 Thinking${if (!isExpanded) " [Press 't' to expand]" else " [Press 't' to collapse]"}$dotsText")
    }

    if (isExpanded && thinking.isNotEmpty()) {
        Box(
            style = Style(
                foreground = Color.BrightYellow
            )
        ) {
            Text("   ${thinking.take(500)}")
        }
    }
}

@Composable
fun ToolActivityPanel(activities: List<ToolActivity>, showDetails: Boolean) {
    if (activities.isEmpty()) return

    Box(
        style = Style(
            foreground = Color.Green
        )
    ) {
        Text("🔧 Tool Activity${if (!showDetails) " [Press 'a' to show details]" else " [Press 'a' to hide details]"}")
    }

    activities.takeLast(5).forEach { activity ->
        val icon = when (activity.toolName) {
            "Bash" -> "▶"
            "ReadFile" -> "📖"
            "WriteFile" -> "✍"
            "EditFile" -> "✏"
            "ListDirectory" -> "📁"
            else -> "⚙"
        }

        if (showDetails) {
            Box(
                style = Style(
                    foreground = Color.BrightGreen
                )
            ) {
                Text("   $icon ${activity.toolName}: ${activity.details.take(60)}")
            }
        } else {
            Box(
                style = Style(
                    foreground = Color.BrightGreen
                )
            ) {
                Text("   $icon ${activity.toolName}: ${activity.summary}")
            }
        }
    }
}

@Composable
fun ConversationPanel(messages: List<ChatMessage>) {
    Box(
        style = Style(
            foreground = Color.BrightBlue,
            bold = true
        )
    ) {
        Text("━━━━━━━━━━━━━━━━━━━━━━━━ Conversation ━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    if (messages.isEmpty()) {
        Box(
            style = Style(
                foreground = Color.Gray
            )
        ) {
            Text("   Welcome! Type your message below to start chatting with Brainiac.")
        }
    }

    messages.forEach { message ->
        val timeFormat = SimpleDateFormat("HH:mm:ss")
        val time = timeFormat.format(Date(message.timestamp))

        when (message.sender) {
            MessageSender.USER -> {
                Box(
                    style = Style(
                        foreground = Color.BrightWhite
                    )
                ) {
                    Text("   [$time] You: ${message.content}")
                }
            }
            MessageSender.BRAINIAC -> {
                Box(
                    style = Style(
                        foreground = Color.Cyan
                    )
                ) {
                    Text("   [$time] 🧠 Brainiac: ${message.content}")
                }
            }
        }
    }
}

@Composable
fun StatusPanel(isWaiting: Boolean, dots: Int) {
    if (isWaiting) {
        val dotsText = ".".repeat(dots)
        Box(
            style = Style(
                foreground = Color.Magenta
            )
        ) {
            Text("   ⏳ Waiting for Brainiac's response$dotsText")
        }
    }
}

@Composable
fun InputPanel(inputText: String, isDisabled: Boolean) {
    Box(
        style = Style(
            foreground = Color.BrightBlue
        )
    ) {
        Text("━━━━━━━━━━━━━━━━━━━━━━━━━━ Input ━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    val prompt = if (isDisabled) {
        "   ⏸  Waiting for response... (input disabled)"
    } else {
        "   > Type your message and press Enter█"
    }

    Box(
        style = Style(
            foreground = if (isDisabled) Color.Gray else Color.BrightWhite
        )
    ) {
        Text(prompt)
    }
}

@Composable
fun FooterPanel() {
    Box(
        style = Style(
            foreground = Color.Gray
        )
    ) {
        Text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    Box(
        style = Style(
            foreground = Color.Gray
        )
    ) {
        Text("   Shortcuts: [t] Toggle Thinking | [a] Toggle Tool Details | Type 'exit' or 'quit' to quit")
    }
}

@Composable
fun Spacer() {
    Text("")
}

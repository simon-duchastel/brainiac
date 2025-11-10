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
                                    thinking = message.content,
                                    isThinking = true
                                )
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
        InputPanel(uiState.isWaitingForResponse)
        FooterPanel()
    }
}

// ============================================================================
// UI Components
// ============================================================================

@Composable
fun HeaderPanel() {
    Text("╔════════════════════════════════════════════════════════════════════════╗", color = Color.BrightCyan, style = TextStyle.Bold)
    Text("║                          🧠 BRAINIAC AI                                ║", color = Color.BrightCyan, style = TextStyle.Bold)
    Text("╚════════════════════════════════════════════════════════════════════════╝", color = Color.BrightCyan, style = TextStyle.Bold)
}

@Composable
fun ThinkingPanel(thinking: String, isThinking: Boolean, isExpanded: Boolean, dots: Int) {
    if (!isThinking && thinking.isEmpty()) return

    val dotsText = ".".repeat(dots)
    Text("💭 Thinking${if (!isExpanded) " [Press 't' to expand]" else " [Press 't' to collapse]"}$dotsText", color = Color.Yellow)

    if (isExpanded && thinking.isNotEmpty()) {
        Text("   ${thinking.take(500)}", color = Color.BrightYellow)
    }
}

@Composable
fun ToolActivityPanel(activities: List<ToolActivity>, showDetails: Boolean) {
    if (activities.isEmpty()) return

    Text("🔧 Tool Activity${if (!showDetails) " [Press 'a' to show details]" else " [Press 'a' to hide details]"}", color = Color.Green)

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
            Text("   $icon ${activity.toolName}: ${activity.details.take(60)}", color = Color.BrightGreen)
        } else {
            Text("   $icon ${activity.toolName}: ${activity.summary}", color = Color.BrightGreen)
        }
    }
}

@Composable
fun ConversationPanel(messages: List<ChatMessage>) {
    Text("━━━━━━━━━━━━━━━━━━━━━━━━ Conversation ━━━━━━━━━━━━━━━━━━━━━━━━", color = Color.BrightBlue, style = TextStyle.Bold)

    if (messages.isEmpty()) {
        Text("   Welcome! Type your message below to start chatting with Brainiac.", color = Color.White)
    }

    messages.forEach { message ->
        val timeFormat = SimpleDateFormat("HH:mm:ss")
        val time = timeFormat.format(Date(message.timestamp))

        when (message.sender) {
            MessageSender.USER -> {
                Text("   [$time] You: ${message.content}", color = Color.BrightWhite)
            }
            MessageSender.BRAINIAC -> {
                Text("   [$time] 🧠 Brainiac: ${message.content}", color = Color.Cyan)
            }
        }
    }
}

@Composable
fun StatusPanel(isWaiting: Boolean, dots: Int) {
    if (isWaiting) {
        val dotsText = ".".repeat(dots)
        Text("   ⏳ Waiting for Brainiac's response$dotsText", color = Color.Magenta)
    }
}

@Composable
fun InputPanel(isDisabled: Boolean) {
    Text("━━━━━━━━━━━━━━━━━━━━━━━━━━ Input ━━━━━━━━━━━━━━━━━━━━━━━━━━━", color = Color.BrightBlue)

    val prompt = if (isDisabled) {
        "   ⏸  Waiting for response... (input disabled)"
    } else {
        "   > Type your message and press Enter█"
    }

    Text(prompt, color = if (isDisabled) Color.White else Color.BrightWhite)
}

@Composable
fun FooterPanel() {
    Text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", color = Color.White)
    Text("   Shortcuts: [t] Toggle Thinking | [a] Toggle Tool Details | Type 'exit' or 'quit' to quit", color = Color.White)
}

@Composable
fun Spacer() {
    Text("")
}

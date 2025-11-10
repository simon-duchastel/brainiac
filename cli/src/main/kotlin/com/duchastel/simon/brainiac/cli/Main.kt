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
import androidx.compose.runtime.*
import com.duchastel.simon.brainiac.agent.CoreAgent
import com.duchastel.simon.brainiac.agent.CoreAgentConfig
import com.duchastel.simon.brainiac.agent.UserMessage
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.tools.bash.BashTool
import com.duchastel.simon.brainiac.tools.talk.TalkTool
import com.duchastel.simon.brainiac.tools.websearch.WebSearchTool
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.*
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okio.Path.Companion.toPath
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// Circuit Screen
// ============================================================================

data object BrainiacScreen : Screen

// ============================================================================
// Circuit State
// ============================================================================

data class BrainiacState(
    val messages: List<ChatMessage> = emptyList(),
    val thinking: String = "",
    val isThinking: Boolean = false,
    val isThinkingExpanded: Boolean = false,
    val toolActivities: List<ToolActivity> = emptyList(),
    val showToolDetails: Boolean = false,
    val isWaitingForResponse: Boolean = false,
    val loadingDots: Int = 0,
    val eventSink: (BrainiacEvent) -> Unit = {}
) : CircuitUiState

// ============================================================================
// Circuit Events
// ============================================================================

sealed interface BrainiacEvent : CircuitUiEvent {
    data class SendMessage(val message: String) : BrainiacEvent
    data object ToggleThinking : BrainiacEvent
    data object ToggleToolDetails : BrainiacEvent
}

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

// ============================================================================
// Main Entry Point
// ============================================================================

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

    kotlinx.coroutines.runBlocking {
        runBrainiacTUI()
    }
}

suspend fun runBrainiacTUI() {
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
        val state = BrainiacPresenter(
            googleApiKey = googleApiKey,
            openRouterApiKey = openRouterApiKey,
            tavilyApiKey = tavilyApiKey,
            stealthModel = stealthModel,
            shortTermMemoryRepository = shortTermMemoryRepository,
            longTermMemoryRepository = longTermMemoryRepository
        )
        BrainiacUi(state = state)
    }
}

// ============================================================================
// Circuit Presenter (Business Logic)
// ============================================================================

@Composable
fun BrainiacPresenter(
    googleApiKey: String,
    openRouterApiKey: String,
    tavilyApiKey: String?,
    stealthModel: LLModel,
    shortTermMemoryRepository: ShortTermMemoryRepository,
    longTermMemoryRepository: LongTermMemoryRepository
): BrainiacState {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var thinking by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isThinkingExpanded by remember { mutableStateOf(false) }
    var toolActivities by remember { mutableStateOf<List<ToolActivity>>(emptyList()) }
    var showToolDetails by remember { mutableStateOf(false) }
    var isWaitingForResponse by remember { mutableStateOf(false) }
    var loadingDots by remember { mutableStateOf(0) }

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
                highThoughtModel = stealthModel,
                mediumThoughtModel = GoogleModels.Gemini2_5Flash,
                lowThoughtModel = GoogleModels.Gemini2_5FlashLite,
                executionClients = mapOf(
                    LLMProvider.Google to GoogleLLMClient(googleApiKey),
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
                    input.lowercase() in listOf("exit", "quit") -> UserMessage.Stop
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

    // Terminal input handling in background
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            while (true) {
                try {
                    val line = readlnOrNull() ?: break
                    val trimmed = line.trim()

                    when {
                        trimmed.equals("t", ignoreCase = true) -> {
                            isThinkingExpanded = !isThinkingExpanded
                        }
                        trimmed.equals("a", ignoreCase = true) -> {
                            showToolDetails = !showToolDetails
                        }
                        trimmed.isNotEmpty() && !isWaitingForResponse -> {
                            userInputChannel.send(trimmed)
                        }
                    }
                } catch (e: Exception) {
                    break
                }
            }
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
        eventSink = { event ->
            when (event) {
                is BrainiacEvent.SendMessage -> {
                    if (!isWaitingForResponse) {
                        userInputChannel.trySend(event.message)
                    }
                }
                BrainiacEvent.ToggleThinking -> {
                    isThinkingExpanded = !isThinkingExpanded
                }
                BrainiacEvent.ToggleToolDetails -> {
                    showToolDetails = !showToolDetails
                }
            }
        }
    )
}

// ============================================================================
// Circuit UI (Pure Presentation)
// ============================================================================

@Composable
fun BrainiacUi(state: BrainiacState) {
    Column {
        HeaderPanel()
        Spacer()
        ThinkingPanel(
            thinking = state.thinking,
            isThinking = state.isThinking,
            isExpanded = state.isThinkingExpanded,
            dots = state.loadingDots
        )
        Spacer()
        ToolActivityPanel(
            activities = state.toolActivities,
            showDetails = state.showToolDetails
        )
        Spacer()
        ConversationPanel(messages = state.messages.takeLast(10))
        Spacer()
        StatusPanel(
            isWaiting = state.isWaitingForResponse,
            dots = state.loadingDots
        )
        Spacer()
        InputPanel(isDisabled = state.isWaitingForResponse)
        FooterPanel()
    }
}

// ============================================================================
// UI Components
// ============================================================================

@Composable
fun HeaderPanel() {
    Text("╔════════════════════════════════════════════════════════════════════════╗", color = Color.Cyan, textStyle = TextStyle.Bold)
    Text("║                          🧠 BRAINIAC AI                                ║", color = Color.Cyan, textStyle = TextStyle.Bold)
    Text("╚════════════════════════════════════════════════════════════════════════╝", color = Color.Cyan, textStyle = TextStyle.Bold)
}

@Composable
fun ThinkingPanel(thinking: String, isThinking: Boolean, isExpanded: Boolean, dots: Int) {
    if (!isThinking && thinking.isEmpty()) return

    val dotsText = ".".repeat(dots)
    Text("💭 Thinking${if (!isExpanded) " [Press 't' to expand]" else " [Press 't' to collapse]"}$dotsText", color = Color.Yellow)

    if (isExpanded && thinking.isNotEmpty()) {
        Text("   ${thinking.take(500)}", color = Color.Yellow)
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
            Text("   $icon ${activity.toolName}: ${activity.details.take(60)}", color = Color.Green)
        } else {
            Text("   $icon ${activity.toolName}: ${activity.summary}", color = Color.Green)
        }
    }
}

@Composable
fun ConversationPanel(messages: List<ChatMessage>) {
    Text("━━━━━━━━━━━━━━━━━━━━━━━━ Conversation ━━━━━━━━━━━━━━━━━━━━━━━━", color = Color.Cyan, textStyle = TextStyle.Bold)

    if (messages.isEmpty()) {
        Text("   Welcome! Type your message below to start chatting with Brainiac.", color = Color.White)
    }

    messages.forEach { message ->
        val timeFormat = SimpleDateFormat("HH:mm:ss")
        val time = timeFormat.format(Date(message.timestamp))

        when (message.sender) {
            MessageSender.USER -> {
                Text("   [$time] You: ${message.content}", color = Color.White)
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
    Text("━━━━━━━━━━━━━━━━━━━━━━━━━━ Input ━━━━━━━━━━━━━━━━━━━━━━━━━━━", color = Color.Blue)

    val prompt = if (isDisabled) {
        "   ⏸  Waiting for response... (input disabled)"
    } else {
        "   > Type your message and press Enter█"
    }

    Text(prompt, color = if (isDisabled) Color.White else Color.White)
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

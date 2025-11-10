package com.duchastel.simon.brainiac.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.tokenizer.feature.MessageTokenizer
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.Executors.promptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.tokenizer.SimpleRegexBasedTokenizer
import com.duchastel.simon.brainiac.core.process.CoreLoop
import com.duchastel.simon.brainiac.core.process.CoreLoopInstruction
import com.duchastel.simon.brainiac.core.process.context.BrainiacContext
import com.duchastel.simon.brainiac.core.process.memory.LongTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.memory.ShortTermMemoryRepository
import com.duchastel.simon.brainiac.core.process.prompt.Prompts
import kotlinx.coroutines.runBlocking

sealed interface UserMessage {
    data object Stop : UserMessage
    data class Message(val userQuery: String) : UserMessage
}

/**
 * Core agent for running a Brainiac instance.
 */
class CoreAgent(
    private val config: CoreAgentConfig,
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val awaitUserMessage: suspend () -> UserMessage,
    private val onThinking: (String) -> Unit = {},
) {
    private val brainiacContext = BrainiacContext(
        highThoughtModel = config.highThoughtModel,
        mediumThoughtModel = config.mediumThoughtModel,
        lowThoughtModel = config.lowThoughtModel,
    )
    val thinkingTool = ThinkingTool(onThinking)
    private val coreLoop = CoreLoop(
        shortTermMemoryRepository = shortTermMemoryRepository,
        longTermMemoryRepository = longTermMemoryRepository,
        brainiacContext = brainiacContext,
        awaitNextInstruction = {
            when (val message = awaitUserMessage()) {
                UserMessage.Stop -> CoreLoopInstruction.Stop
                is UserMessage.Message -> CoreLoopInstruction.Query(message.userQuery)
            }
        },
        onThinking = onThinking,
        thinkingToolName = thinkingTool.name
    )
    private val coreLoopStrategy = coreLoop.strategy("core-loop")

    /**
     * Runs the agent with the given user query.
     */
    @Suppress("UnstableApiUsage")
    fun run() = runBlocking {
        // Add ThinkingTool to the tool registry
        val enhancedToolRegistry = ToolRegistry {
            config.toolRegistry.tools.forEach { tool(it) }
            tool(thinkingTool)
        }

        val agent = AIAgent(
            promptExecutor = promptExecutor(config.executionClients),
            toolRegistry = enhancedToolRegistry,
            strategy = coreLoopStrategy,
            agentConfig = AIAgentConfig(
                prompt = Prompt.build("brainiac-core") {
                    system(Prompts.BRAINIAC_SYSTEM)
                },
                model = config.highThoughtModel,
                maxAgentIterations = Int.MAX_VALUE,
            ),
            installFeatures = {
                install(MessageTokenizer) {
                    tokenizer = SimpleRegexBasedTokenizer()
                }
                config.onEventHandler?.let { handler ->
                    install(EventHandler) {
                        onLLMCallCompleted { ctx ->
                            handler(ctx.responses)
                        }
                    }
                }
            }
        )

        agent.run(Unit)
    }
}

/**
 * Configuration for CoreAgent.
 *
 * @property toolRegistry Tools available to the agent
 * @property executionClients Map of LLM providers to their clients
 * @property onEventHandler Optional handler for agent events (e.g., messages, tool calls)
 */
data class CoreAgentConfig(
    val highThoughtModel: LLModel,
    val mediumThoughtModel: LLModel,
    val lowThoughtModel: LLModel,
    val toolRegistry: ToolRegistry,
    val executionClients: Map<LLMProvider, LLMClient>,
    val onEventHandler: ((List<Message>) -> Unit)? = null,
)

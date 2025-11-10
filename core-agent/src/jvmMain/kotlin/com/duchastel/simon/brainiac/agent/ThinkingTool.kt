package com.duchastel.simon.brainiac.agent

import ai.koog.agents.core.tools.SimpleTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class ThinkingArgs(
    val thought: String
)

/**
 * Special tool for internal thinking/reasoning.
 *
 * This tool is handled specially by the CoreLoop - instead of being executed
 * like a regular tool, its output is routed to a callback for display/logging.
 */
class ThinkingTool(val onThought: (String) -> Unit) : SimpleTool<ThinkingArgs>() {

    override val name: String = "think"

    override val description: String = """
        Express your internal thoughts and reasoning process.

        IMPORTANT: This is for your internal reasoning and planning. It allows you to think
        through problems step-by-step before taking actions or communicating with the user.

        Use this tool to:
        - Break down complex problems
        - Plan your approach
        - Reason through edge cases
        - Debug your thinking
        - Keep track of what you've learned

        Your thoughts are NOT shown to the user directly - use the 'talk' tool to communicate
        with the user.
    """.trimIndent()

    override val argsSerializer: KSerializer<ThinkingArgs> = serializer()

    override suspend fun doExecute(args: ThinkingArgs): String {
        onThought(args.thought)
        return "Thought recorded"
    }
}

package com.duchastel.simon.brainiac.tools.talk

import ai.koog.agents.core.tools.SimpleTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class TalkArgs(
    val message: String
)

class TalkTool : SimpleTool<TalkArgs>() {

    override val name: String = "talk"

    override val description: String = """
        Communicate a message to the user.

        IMPORTANT: This is the ONLY way to send messages to the user. Your regular responses
        are internal thoughts and are NOT shown to the user. Whenever you want to communicate
        with the user, you MUST use this tool.

        Use this tool to:
        - Answer user questions
        - Provide status updates
        - Ask clarifying questions
        - Share results or findings
        - Give explanations

        The message will be displayed directly to the user.
    """.trimIndent()

    override val argsSerializer: KSerializer<TalkArgs> = serializer()

    override suspend fun doExecute(args: TalkArgs): String {
        return "Message delivered to user"
    }
}

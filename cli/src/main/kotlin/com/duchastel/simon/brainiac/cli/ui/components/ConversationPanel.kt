package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.models.ChatMessage
import com.duchastel.simon.brainiac.cli.models.MessageSender
import com.duchastel.simon.brainiac.cli.ui.utils.LabeledDivider
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationPanel(messages: List<ChatMessage>) {
    LabeledDivider(label = "Conversation", color = Color.Cyan, textStyle = TextStyle.Bold)

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

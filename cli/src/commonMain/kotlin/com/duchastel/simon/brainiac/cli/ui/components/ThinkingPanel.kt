package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text

@Composable
fun ThinkingPanel(thinking: String, isThinking: Boolean, isExpanded: Boolean, dots: Int) {
    if (!isThinking && thinking.isEmpty()) return

    val dotsText = ".".repeat(dots)
    Text("💭 Thinking${if (!isExpanded) " [Press Ctrl-T to expand]" else " [Press Ctrl-T to collapse]"}$dotsText", color = Color.Yellow)

    if (isExpanded && thinking.isNotEmpty()) {
        Text("   ${thinking.take(500)}", color = Color.Yellow)
    }
}

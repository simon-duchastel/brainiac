package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.models.ToolActivity
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text

@Composable
fun ToolActivityPanel(activities: List<ToolActivity>, showDetails: Boolean) {
    if (activities.isEmpty()) return

    Text("🔧 Tool Activity${if (!showDetails) " [Press Ctrl-A to show details]" else " [Press Ctrl-A to hide details]"}", color = Color.Green)

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

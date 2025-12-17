package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

/**
 * Animated ASCII brain header with neural pulse effects
 */
@Composable
fun HeaderPanel(animationFrame: Int = 0) {
    val terminalWidth = LocalTerminalState.current.size.columns

    // Neural pulse characters that cycle through animation frames
    val pulseChars = listOf('·', '•', '●', '◉', '◎', '○', '◌', '◯', '●', '◉', '•', '·')
    val currentPulse = pulseChars[animationFrame % pulseChars.size]

    // Synaptic spark characters
    val sparkChars = listOf('✦', '✧', '⚡', '★', '☆', '✶', '✴', '✸', '✹', '✺', '✧', '✦')
    val currentSpark = sparkChars[animationFrame % sparkChars.size]

    // Color cycling for the neural glow effect
    val colors = listOf(
        Color.Cyan,
        Color.BrightCyan,
        Color.BrightBlue,
        Color.Blue,
        Color.BrightMagenta,
        Color.Magenta,
        Color.BrightMagenta,
        Color.Blue,
        Color.BrightBlue,
        Color.BrightCyan,
        Color.Cyan,
        Color.BrightCyan
    )
    val primaryColor = colors[animationFrame % colors.size]
    val secondaryColor = colors[(animationFrame + 4) % colors.size]
    val accentColor = colors[(animationFrame + 8) % colors.size]

    // Neural activity indicators that pulse at different phases
    val neuron1 = pulseChars[(animationFrame + 0) % pulseChars.size]
    val neuron2 = pulseChars[(animationFrame + 3) % pulseChars.size]
    val neuron3 = pulseChars[(animationFrame + 6) % pulseChars.size]
    val neuron4 = pulseChars[(animationFrame + 9) % pulseChars.size]

    // The magnificent ASCII brain with animated neural pulses
    val brainArt = listOf(
        "                    $currentSpark        $currentSpark                     ",
        "              ░░░▒▒▒▓▓▓▓▓▓▓▓▓▓▒▒▒░░░              ",
        "          ░▒▓█$neuron1 ══════════════════ $neuron2█▓▒░          ",
        "        ▒▓██$neuron3═══╗  NEURAL CORTEX  ╔═══$neuron4██▓▒        ",
        "      ░▓██$currentPulse═══╬══════════════════╬═══$currentPulse██▓░      ",
        "     ▒██$neuron1══╬══╝  ┌──────────┐  ╚══╬══$neuron2██▒     ",
        "    ▒██$neuron3══╬════  │ BRAINIAC │  ════╬══$neuron4██▒    ",
        "   ░██$currentPulse══╬════  │    AI    │  ════╬══$currentPulse██░   ",
        "   ▓█$neuron1═══╬════  └──────────┘  ════╬═══$neuron2█▓   ",
        "   ██$neuron3═══╬══╗  ┌────────────┐  ╔══╬═══$neuron4██   ",
        "   ██$currentPulse═══╬══╬══│ SYNAPTIC   │══╬══╬═══$currentPulse██   ",
        "   ██$neuron1═══╬══╬══│   NETWORK  │══╬══╬═══$neuron2██   ",
        "   ▓█$neuron3═══╬══╬══└────────────┘══╬══╬═══$neuron4█▓   ",
        "   ░██$currentPulse══╬══╚════════════════╝══╬══$currentPulse██░   ",
        "    ▒██$neuron1══╬════════════════════╬══$neuron2██▒    ",
        "     ▒██$neuron3══╬══════════════════╬══$neuron4██▒     ",
        "      ░▓██$currentPulse═══════════════════$currentPulse██▓░      ",
        "        ▒▓██$neuron1═══════════════$neuron2██▓▒        ",
        "          ░▒▓█$neuron3═════════════$neuron4█▓▒░          ",
        "              ░░░▒▒▒▓▓▓▓▓▓▓▓▒▒▒░░░              ",
        "                    $currentSpark        $currentSpark                     "
    )

    // Top border with animated sparks
    val topBorder = buildString {
        append("╔")
        for (i in 0 until (terminalWidth - 2).coerceAtLeast(0)) {
            if (i % 8 == (animationFrame % 8)) {
                append(currentSpark)
            } else {
                append("═")
            }
        }
        append("╗")
    }

    // Bottom border with animated sparks
    val bottomBorder = buildString {
        append("╚")
        for (i in 0 until (terminalWidth - 2).coerceAtLeast(0)) {
            if (i % 8 == ((animationFrame + 4) % 8)) {
                append(currentSpark)
            } else {
                append("═")
            }
        }
        append("╝")
    }

    Column {
        // Top border
        Text(topBorder, color = primaryColor, textStyle = TextStyle.Bold)

        // Brain art with centered padding
        brainArt.forEachIndexed { index, line ->
            val lineColor = when {
                index < 2 || index >= brainArt.size - 2 -> accentColor
                index in 3..5 || index in brainArt.size - 6..brainArt.size - 4 -> secondaryColor
                else -> primaryColor
            }
            val padding = ((terminalWidth - 2 - line.length) / 2).coerceAtLeast(0)
            val leftPad = " ".repeat(padding)
            val rightPad = " ".repeat((terminalWidth - 2 - padding - line.length).coerceAtLeast(0))
            Text("║$leftPad$line$rightPad║", color = lineColor, textStyle = TextStyle.Bold)
        }

        // Bottom border
        Text(bottomBorder, color = primaryColor, textStyle = TextStyle.Bold)
    }
}

package com.duchastel.simon.brainiac.cli.ui.utils

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.theme.BrainiacTheme
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

/**
 * Draws a horizontal divider line across the full width
 */
@Composable
fun Divider(
    char: Char = '━',
    color: Color = BrainiacTheme.colors.divider,
    textStyle: TextStyle = TextStyle.Bold
) {
    val terminalWidth = LocalTerminalState.current.size.columns
    Text(char.toString().repeat(terminalWidth), color = color, textStyle = textStyle)
}

/**
 * Draws a labeled divider with text in the middle
 */
@Composable
fun LabeledDivider(
    label: String,
    char: Char = '━',
    color: Color = BrainiacTheme.colors.sectionHeader,
    textStyle: TextStyle = TextStyle.Bold
) {
    val terminalWidth = LocalTerminalState.current.size.columns
    val padding = (terminalWidth - label.length - 2) / 2
    val left = char.toString().repeat(padding.coerceAtLeast(0))
    val right = char.toString().repeat((terminalWidth - padding - label.length - 2).coerceAtLeast(0))
    Text("$left $label $right", color = color, textStyle = textStyle)
}

/**
 * Simple vertical spacer
 */
@Composable
fun Spacer() {
    Text("")
}

/**
 * Draws a boxed header with a centered label
 */
@Composable
fun BoxedHeader(
    label: String,
    color: Color = BrainiacTheme.colors.primary,
    textStyle: TextStyle = TextStyle.Bold
) {
    val terminalWidth = LocalTerminalState.current.size.columns
    val padding = (terminalWidth - label.length - 2) / 2
    val leftPad = " ".repeat(padding.coerceAtLeast(0))
    val rightPad = " ".repeat((terminalWidth - padding - label.length - 2).coerceAtLeast(0))

    Text("╔${"═".repeat((terminalWidth - 2).coerceAtLeast(0))}╗", color = color, textStyle = textStyle)
    Text("║$leftPad$label$rightPad║", color = color, textStyle = textStyle)
    Text("╚${"═".repeat((terminalWidth - 2).coerceAtLeast(0))}╝", color = color, textStyle = textStyle)
}

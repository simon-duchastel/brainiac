package com.duchastel.simon.brainiac.cli.ui.utils

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

/**
 * Draws a horizontal divider line across the full width
 */
@Composable
fun Divider(
    char: Char = '━',
    color: Color = Color.White,
    textStyle: TextStyle = TextStyle.None
) {
    Text(char.toString().repeat(76), color = color, textStyle = textStyle)
}

/**
 * Draws a labeled divider with text in the middle
 */
@Composable
fun LabeledDivider(
    label: String,
    char: Char = '━',
    color: Color = Color.White,
    textStyle: TextStyle = TextStyle.None
) {
    val padding = (76 - label.length - 2) / 2
    val left = char.toString().repeat(padding)
    val right = char.toString().repeat(76 - padding - label.length - 2)
    Text("$left $label $right", color = color, textStyle = textStyle)
}

/**
 * Simple vertical spacer
 */
@Composable
fun Spacer() {
    Text("")
}

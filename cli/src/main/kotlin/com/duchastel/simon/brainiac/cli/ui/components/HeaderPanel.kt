package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

/**
 * Elegant ASCII brain header
 */
@Composable
fun HeaderPanel(animationFrame: Int = 0) {
    val terminalWidth = LocalTerminalState.current.size.columns

    // Highly detailed realistic brain with dense texturing
    val brainArt = listOf(
        "                                          ░░▒▒▓▓▓▓▓▓▓▓▓▓▓▓▓▓▒▒░░                                          ",
        "                                  ░░▒▒▓▓████████████████████████████▓▓▒▒░░                                ",
        "                            ░░▒▓████████▓▓▒▒░░▒▒▓▓████▓▓▒▒░░▒▒▓▓████████▓▒░░                              ",
        "                        ░▒▓██████▓▓▒▒▓▓████▓▓▒▒░░▒▒▓▓▒▒░░▒▓████▓▓▒▒▓▓██████▓▒░                            ",
        "                     ░▒████████▒▒▓▓████▓▓▒▒▓▓██▓▓░░░░░░▓▓██▓▓▒▒▓▓████▓▓▒▒████████▒░                       ",
        "                   ░▓██████▓▓▒▒████▓▓▒▒▓▓██▓▓▒▒██▓▓▒▒▓▓██▒▒▓▓██▓▓▒▒▓▓████▒▒▓▓██████▓░                     ",
        "                 ░▓██████▓▓▒▒██▓▓▒▒▓▓██▓▓▒▒██▓▓▒▒▓▓▓▓▒▒▓▓██▒▒▓▓██▓▓▒▒▓▓██▒▒▓▓██████▓░                     ",
        "                ░██████▓▓▒▒██▓▓▒▒▓▓██▒▒▓▓██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▓▓▒▒▓▓██▒▒▓▓██████░                      ",
        "               ░▓████▓▓▒▒██▓▓▒▒▓▓██▒▒▓▓██▒▒██▓▓▒▒▓▓▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒▓▓██▒▒▓▓████▓░                     ",
        "              ░▓████▓▓▒▒██▒▒▓▓██▒▒▓▓██▒▒██▓▓▒▒██▓▓██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▒▒▓▓████▓░                      ",
        "              ░████▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓▒▒██▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒▓▓████░                        ",
        "             ░▓████▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒████▓░                       ",
        "             ░████▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒▓▓████░                       ",
        "             ░████▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒▓▓▒▒████░                       ",
        "             ░████▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓████░                         ",
        "              ░████▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▒▒▓▓██▒▒██▓▓▒▒████░                          ",
        "              ░▓████▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒▓▓████▓░                          ",
        "               ░████▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▒▒▓▓██▒▒██▓▓▒▒▓▓████░                           ",
        "                ░▓████▒▒▓▓██▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▒▒██▓▓▒▒██▒▒████▓░                            ",
        "                 ░▓████▓▓▒▒██▓▓▒▒██▓▓▒▒██▓▓▒▒██▒▒▓▓██▒▒██▓▓▒▒██▓▓▒▒██▓▓████▓░                             ",
        "                   ░▓████▓▓▒▒██▓▓▒▒██▓▓▒▒██▓▓▒▒████▒▒▓▓██▒▒██▓▓▒▒▓▓██▒▒████▓░                             ",
        "                     ░▒████▓▓▒▒██▓▓▒▒██▓▓▒▒▓▓██▓▓▒▒██▓▓▒▒██▓▓▒▒██▓▓▒▒████▒░                               ",
        "                       ░▒▓████▓▓▒▒██▓▓▒▒▓▓██▓▓▒▒▓▓██▓▓▒▒██▓▓▒▒██▓▓████▓▒░                                 ",
        "                          ░▒▓████▓▓▒▒▓▓██▓▓▒▒▓▓▓▓▓▓▓▓▒▒▓▓██▓▓▒▒████▓▒░                                    ",
        "                              ░▒▓████▓▓▒▒▓▓▓▓▓▓▓▓▓▓▓▓▓▓▒▒▓▓████▓▒░                                        ",
        "                                  ░░▒▒▓▓████████████████▓▓▒▒░░                                            ",
        "                                          ░░▒▒▒▒▒▒▒▒░░                                                    "
    )

    // Rounded borders
    val topBorder = "╭${"─".repeat((terminalWidth - 2).coerceAtLeast(0))}╮"
    val bottomBorder = "╰${"─".repeat((terminalWidth - 2).coerceAtLeast(0))}╯"

    Column {
        Text(topBorder, color = Color.Magenta, textStyle = TextStyle.Bold)

        brainArt.forEach { line ->
            val padding = ((terminalWidth - 2 - line.length) / 2).coerceAtLeast(0)
            val leftPad = " ".repeat(padding)
            val rightPad = " ".repeat((terminalWidth - 2 - padding - line.length).coerceAtLeast(0))
            Text("│$leftPad$line$rightPad│", color = Color.Magenta, textStyle = TextStyle.Bold)
        }

        Text(bottomBorder, color = Color.Magenta, textStyle = TextStyle.Bold)
    }
}

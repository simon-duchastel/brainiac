package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

@Composable
fun HeaderPanel() {
    Text("╔════════════════════════════════════════════════════════════════════════╗", color = Color.Cyan, textStyle = TextStyle.Bold)
    Text("║                          🧠 BRAINIAC AI                                ║", color = Color.Cyan, textStyle = TextStyle.Bold)
    Text("╚════════════════════════════════════════════════════════════════════════╝", color = Color.Cyan, textStyle = TextStyle.Bold)
}

package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.theme.BrainiacTheme
import com.jakewharton.mosaic.ui.Text

@Composable
fun StatusPanel(isWaiting: Boolean, dots: Int) {
    if (isWaiting) {
        val colors = BrainiacTheme.colors
        val dotsText = ".".repeat(dots)
        Text("   ⏳ Waiting for Brainiac's response$dotsText", color = colors.status)
    }
}

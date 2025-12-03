package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text

@Composable
fun StatusPanel(isWaiting: Boolean, dots: Int) {
    if (isWaiting) {
        val dotsText = ".".repeat(dots)
        Text("   ⏳ Waiting for Brainiac's response$dotsText", color = Color.Magenta)
    }
}

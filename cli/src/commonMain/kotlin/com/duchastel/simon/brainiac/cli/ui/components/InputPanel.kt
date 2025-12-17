package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.utils.LabeledDivider
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text

@Composable
fun InputPanel(inputBuffer: String, isDisabled: Boolean) {
    LabeledDivider(label = "Input", color = Color.Blue)

    val prompt = if (isDisabled) {
        "   ⏸  Waiting for response... (input disabled)"
    } else {
        "   > $inputBuffer█"
    }

    Text(prompt, color = if (isDisabled) Color.White else Color.White)
}

package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.theme.BrainiacTheme
import com.duchastel.simon.brainiac.cli.ui.utils.LabeledDivider
import com.jakewharton.mosaic.ui.Text

@Composable
fun InputPanel(inputBuffer: String, isDisabled: Boolean) {
    val colors = BrainiacTheme.colors
    LabeledDivider(label = "Input", color = colors.info)

    val prompt = if (isDisabled) {
        "   ⏸  Waiting for response... (input disabled)"
    } else {
        "   > $inputBuffer█"
    }

    Text(prompt, color = if (isDisabled) colors.disabled else colors.input)
}

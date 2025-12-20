package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.theme.BrainiacTheme
import com.duchastel.simon.brainiac.cli.ui.utils.BoxedHeader
import com.jakewharton.mosaic.ui.TextStyle

@Composable
fun HeaderPanel() {
    val colors = BrainiacTheme.colors
    BoxedHeader(
        label = "BRAINIAC AI",
        color = colors.primary,
        textStyle = TextStyle.Bold
    )
}

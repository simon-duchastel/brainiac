package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.utils.BoxedHeader
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.TextStyle

@Composable
fun HeaderPanel() {
    BoxedHeader(
        label = "BRAINIAC AI",
        color = Color.Cyan,
        textStyle = TextStyle.Bold
    )
}

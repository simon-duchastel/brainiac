package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.utils.Divider
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Text

@Composable
fun FooterPanel() {
    Divider(color = Color.White)
    Text("   Shortcuts: [Ctrl-T] Toggle Thinking | [Ctrl-A] Toggle Tool Details | [Ctrl-C] Quit", color = Color.White)
}

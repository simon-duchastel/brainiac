package com.duchastel.simon.brainiac.cli.ui.components

import androidx.compose.runtime.Composable
import com.duchastel.simon.brainiac.cli.ui.theme.BrainiacTheme
import com.duchastel.simon.brainiac.cli.ui.utils.Divider
import com.jakewharton.mosaic.ui.Text

@Composable
fun FooterPanel() {
    val colors = BrainiacTheme.colors
    Divider(color = colors.divider)
    Text("   Shortcuts: [Ctrl-T] Toggle Thinking | [Ctrl-A] Toggle Tool Details | [Ctrl-C] Quit", color = colors.textMuted)
}

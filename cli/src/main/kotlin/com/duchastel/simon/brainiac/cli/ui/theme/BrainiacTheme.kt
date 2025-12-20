package com.duchastel.simon.brainiac.cli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.ui.Color

/**
 * Brainiac color palette.
 * Available Mosaic colors: Black, Blue, Cyan, Green, Magenta, Red, White, Yellow
 */
object BrainiacColors {
    val primary: Color = Color.Magenta
    val secondary: Color = Color.Magenta
    val surface: Color = Color.White
    val surfaceMuted: Color = Color.Black

    val textPrimary: Color = Color.White
    val textSecondary: Color = Color.White
    val textMuted: Color = Color.White

    val success: Color = Color.Green
    val warning: Color = Color.Yellow
    val info: Color = Color.Cyan
    val status: Color = Color.Blue
    val error: Color = Color.Red

    val divider: Color = Color.White
    val sectionHeader: Color = Color.Magenta
    val input: Color = Color.White
    val disabled: Color = Color.White
}

data class BrainiacColorScheme(
    val primary: Color = BrainiacColors.primary,
    val secondary: Color = BrainiacColors.secondary,
    val surface: Color = BrainiacColors.surface,
    val surfaceMuted: Color = BrainiacColors.surfaceMuted,
    val textPrimary: Color = BrainiacColors.textPrimary,
    val textSecondary: Color = BrainiacColors.textSecondary,
    val textMuted: Color = BrainiacColors.textMuted,
    val success: Color = BrainiacColors.success,
    val warning: Color = BrainiacColors.warning,
    val info: Color = BrainiacColors.info,
    val status: Color = BrainiacColors.status,
    val error: Color = BrainiacColors.error,
    val divider: Color = BrainiacColors.divider,
    val sectionHeader: Color = BrainiacColors.sectionHeader,
    val input: Color = BrainiacColors.input,
    val disabled: Color = BrainiacColors.disabled
)

val LocalBrainiacColors = staticCompositionLocalOf { BrainiacColorScheme() }

@Composable
fun BrainiacTheme(
    colorScheme: BrainiacColorScheme = BrainiacColorScheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBrainiacColors provides colorScheme) {
        content()
    }
}

object BrainiacTheme {
    val colors: BrainiacColorScheme
        @Composable
        get() = LocalBrainiacColors.current
}

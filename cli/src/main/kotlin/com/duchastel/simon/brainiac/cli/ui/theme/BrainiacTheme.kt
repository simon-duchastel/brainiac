package com.duchastel.simon.brainiac.cli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.ui.Color

/**
 * Brainiac Color Palette
 *
 * A modern, sleek theme optimized for terminal display.
 * Uses the available Mosaic Color palette effectively.
 *
 * Available Mosaic colors: Black, Blue, Cyan, Green, Magenta, Red, White, Yellow
 */
object BrainiacColors {
    // ═══════════════════════════════════════════════════════════════════════
    // PRIMARY BRAND COLORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Primary brand color - Clean cyan for headers and branding */
    val primary: Color = Color.Cyan

    /** Secondary brand color - Cyan for AI responses (consistent branding) */
    val secondary: Color = Color.Cyan

    // ═══════════════════════════════════════════════════════════════════════
    // SURFACE & BACKGROUND
    // ═══════════════════════════════════════════════════════════════════════

    /** Primary surface color - Clean white */
    val surface: Color = Color.White

    /** Muted surface - Black for contrast */
    val surfaceMuted: Color = Color.Black

    // ═══════════════════════════════════════════════════════════════════════
    // TEXT COLORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Primary text - White for maximum readability */
    val textPrimary: Color = Color.White

    /** Secondary text - White for supporting content */
    val textSecondary: Color = Color.White

    /** Muted text - White for hints (contextual usage) */
    val textMuted: Color = Color.White

    // ═══════════════════════════════════════════════════════════════════════
    // SEMANTIC ACCENT COLORS (The "Pop"!)
    // ═══════════════════════════════════════════════════════════════════════

    /** Success/Tools - Vibrant green */
    val success: Color = Color.Green

    /** Warning/Thinking - Warm yellow */
    val warning: Color = Color.Yellow

    /** Info/Input - Sky blue */
    val info: Color = Color.Blue

    /** Status/Loading - Soft magenta */
    val status: Color = Color.Magenta

    /** Error state - Red */
    val error: Color = Color.Red

    // ═══════════════════════════════════════════════════════════════════════
    // UI ELEMENT COLORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Divider lines - White */
    val divider: Color = Color.White

    /** Section headers/labels - Primary brand cyan */
    val sectionHeader: Color = Color.Cyan

    /** User input cursor and text */
    val input: Color = Color.White

    /** Disabled/inactive state */
    val disabled: Color = Color.White
}

/**
 * Semantic color roles for consistent theming across components.
 * Maps conceptual UI roles to the color palette.
 */
data class BrainiacColorScheme(
    // Brand
    val primary: Color = BrainiacColors.primary,
    val secondary: Color = BrainiacColors.secondary,

    // Surfaces
    val surface: Color = BrainiacColors.surface,
    val surfaceMuted: Color = BrainiacColors.surfaceMuted,

    // Text
    val textPrimary: Color = BrainiacColors.textPrimary,
    val textSecondary: Color = BrainiacColors.textSecondary,
    val textMuted: Color = BrainiacColors.textMuted,

    // Semantic accents
    val success: Color = BrainiacColors.success,
    val warning: Color = BrainiacColors.warning,
    val info: Color = BrainiacColors.info,
    val status: Color = BrainiacColors.status,
    val error: Color = BrainiacColors.error,

    // UI Elements
    val divider: Color = BrainiacColors.divider,
    val sectionHeader: Color = BrainiacColors.sectionHeader,
    val input: Color = BrainiacColors.input,
    val disabled: Color = BrainiacColors.disabled
)

/**
 * CompositionLocal for accessing the current theme colors.
 */
val LocalBrainiacColors = staticCompositionLocalOf { BrainiacColorScheme() }

/**
 * Brainiac Theme wrapper that provides the color scheme to all child composables.
 */
@Composable
fun BrainiacTheme(
    colorScheme: BrainiacColorScheme = BrainiacColorScheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBrainiacColors provides colorScheme
    ) {
        content()
    }
}

/**
 * Convenience accessor for theme colors within composables.
 */
object BrainiacTheme {
    val colors: BrainiacColorScheme
        @Composable
        get() = LocalBrainiacColors.current
}

package com.duchastel.simon.brainiac.cli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.ui.Color

/**
 * Brainiac Color Palette
 *
 * A modern, sleek theme with clean whites and greys accented by vibrant colors.
 * Designed for optimal terminal readability while maintaining visual appeal.
 */
object BrainiacColors {
    // ═══════════════════════════════════════════════════════════════════════
    // PRIMARY BRAND COLORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Primary brand color - Electric cyan for headers and branding */
    val primary: Color = Color.BrightCyan

    /** Secondary brand color - Softer cyan for AI responses */
    val secondary: Color = Color.Cyan

    // ═══════════════════════════════════════════════════════════════════════
    // SURFACE & BACKGROUND
    // ═══════════════════════════════════════════════════════════════════════

    /** Primary surface color - Clean white */
    val surface: Color = Color.White

    /** Muted surface - Subtle grey for less prominent elements */
    val surfaceMuted: Color = Color.BrightBlack

    // ═══════════════════════════════════════════════════════════════════════
    // TEXT COLORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Primary text - Bright white for maximum readability */
    val textPrimary: Color = Color.BrightWhite

    /** Secondary text - Softer white for supporting content */
    val textSecondary: Color = Color.White

    /** Muted text - Grey for hints and less important info */
    val textMuted: Color = Color.BrightBlack

    // ═══════════════════════════════════════════════════════════════════════
    // SEMANTIC ACCENT COLORS (The "Pop"!)
    // ═══════════════════════════════════════════════════════════════════════

    /** Success/Tools - Vibrant mint green */
    val success: Color = Color.BrightGreen

    /** Warning/Thinking - Warm golden amber */
    val warning: Color = Color.BrightYellow

    /** Info/Input - Sky blue */
    val info: Color = Color.BrightBlue

    /** Status/Loading - Soft lavender */
    val status: Color = Color.BrightMagenta

    /** Error state - Coral red */
    val error: Color = Color.BrightRed

    // ═══════════════════════════════════════════════════════════════════════
    // UI ELEMENT COLORS
    // ═══════════════════════════════════════════════════════════════════════

    /** Divider lines - Subtle grey */
    val divider: Color = Color.BrightBlack

    /** Section headers/labels - Primary brand */
    val sectionHeader: Color = Color.BrightCyan

    /** User input cursor and text */
    val input: Color = Color.BrightWhite

    /** Disabled/inactive state */
    val disabled: Color = Color.BrightBlack
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

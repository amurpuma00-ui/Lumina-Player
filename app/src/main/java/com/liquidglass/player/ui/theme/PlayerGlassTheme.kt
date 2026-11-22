package com.liquidglass.player.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.palette.graphics.Palette
import com.liquidglass.player.data.PlayerThemePreset

data class PlayerPalette(
    val bgBase: Color,
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val glassTint: Color,
    val accent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isLight: Boolean = false
)

fun extractAlbumPalette(bitmap: Bitmap?): PlayerPalette? {
    if (bitmap == null) return null
    val p = runCatching { Palette.from(bitmap).generate() }.getOrNull() ?: return null
    val vibrant = p.vibrantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF38BDF8)
    val darkMuted = p.darkMutedSwatch?.rgb?.let { Color(it) } ?: Color(0xFF0A0F1F)
    val lightVibrant = p.lightVibrantSwatch?.rgb?.let { Color(it) } ?: Color(0xFFA855F7)
    val muted = p.mutedSwatch?.rgb?.let { Color(it) } ?: Color(0xFF2563EB)

    return PlayerPalette(
        bgBase = Color(
            red = (darkMuted.red * 0.35f).coerceIn(0.02f, 0.14f),
            green = (darkMuted.green * 0.35f).coerceIn(0.02f, 0.14f),
            blue = (darkMuted.blue * 0.42f).coerceIn(0.04f, 0.18f),
            alpha = 1f
        ),
        orb1 = vibrant,
        orb2 = lightVibrant,
        orb3 = muted,
        glassTint = Color(0xFFFFFFFF),
        accent = vibrant,
        secondaryAccent = lightVibrant,
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xC2E2E8F0)
    )
}

@Composable
fun rememberAnimatedPlayerPalette(
    preset: PlayerThemePreset,
    albumPalette: PlayerPalette?
): PlayerPalette {
    val context = LocalContext.current

    val target = remember(preset, albumPalette) {
        when (preset) {
            PlayerThemePreset.ALBUM_DYNAMIC -> albumPalette ?: PlayerPalette(
                bgBase = Color(0xFF070B1A),
                orb1 = Color(0xFF00D2FF),
                orb2 = Color(0xFF7B2CBF),
                orb3 = Color(0xFF3A86FF),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFF38BDF8),
                secondaryAccent = Color(0xFFC084FC),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xBFE2E8F0)
            )
            PlayerThemePreset.LIQUID_OCEAN -> PlayerPalette(
                bgBase = Color(0xFF050B18),
                orb1 = Color(0xFF00C2FF),
                orb2 = Color(0xFF3A5BFF),
                orb3 = Color(0xFF7B2CBF),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFF38BDF8),
                secondaryAccent = Color(0xFF818CF8),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xB8E2E8F0)
            )
            PlayerThemePreset.MIDNIGHT_AURORA -> PlayerPalette(
                bgBase = Color(0xFF040712),
                orb1 = Color(0xFF00F5D4),
                orb2 = Color(0xFF9B5DE5),
                orb3 = Color(0xFFF15BB5),
                glassTint = Color(0xFFE0F2FE),
                accent = Color(0xFF00F5D4),
                secondaryAccent = Color(0xFFF15BB5),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xB8CBD5E1)
            )
            PlayerThemePreset.SUNSET_PRISM -> PlayerPalette(
                bgBase = Color(0xFF150712),
                orb1 = Color(0xFFFF6B6B),
                orb2 = Color(0xFFFF9F1C),
                orb3 = Color(0xFF9D4EDD),
                glassTint = Color(0xFFFFF1F2),
                accent = Color(0xFFFF9F1C),
                secondaryAccent = Color(0xFFFF6B6B),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xC0FFE4E6)
            )
            PlayerThemePreset.NEON_CYBER -> PlayerPalette(
                bgBase = Color(0xFF070514),
                orb1 = Color(0xFFFF007F),
                orb2 = Color(0xFF00F0FF),
                orb3 = Color(0xFF7000FF),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFF00F0FF),
                secondaryAccent = Color(0xFFFF007F),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xC4E2E8F0)
            )
            PlayerThemePreset.EMERALD_GLASS -> PlayerPalette(
                bgBase = Color(0xFF04120E),
                orb1 = Color(0xFF10B981),
                orb2 = Color(0xFF06B6D4),
                orb3 = Color(0xFF34D399),
                glassTint = Color(0xFFECFDF5),
                accent = Color(0xFF34D399),
                secondaryAccent = Color(0xFF22D3EE),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xB8D1FAE5)
            )
            PlayerThemePreset.FROST_LIGHT -> PlayerPalette(
                bgBase = Color(0xFFEAF2FF),
                orb1 = Color(0xFF60A5FA),
                orb2 = Color(0xFFA78BFA),
                orb3 = Color(0xFF38BDF8),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFF2563EB),
                secondaryAccent = Color(0xFF7C3AED),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xB3334155),
                isLight = true
            )
            PlayerThemePreset.AMOLED_OBSIDIAN -> PlayerPalette(
                bgBase = Color(0xFF000000),
                orb1 = Color(0xFF1E293B),
                orb2 = Color(0xFF334155),
                orb3 = Color(0xFF0F172A),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFFFF9F0A),
                secondaryAccent = Color(0xFF38BDF8),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xA6CBD5E1)
            )
            PlayerThemePreset.MATERIAL_YOU -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dyn = dynamicDarkColorScheme(context)
                    PlayerPalette(
                        bgBase = dyn.background,
                        orb1 = dyn.primary,
                        orb2 = dyn.tertiary,
                        orb3 = dyn.secondary,
                        glassTint = dyn.surfaceVariant,
                        accent = dyn.primary,
                        secondaryAccent = dyn.tertiary,
                        textPrimary = dyn.onBackground,
                        textSecondary = dyn.onSurfaceVariant
                    )
                } else {
                    PlayerPalette(
                        bgBase = Color(0xFF050B18),
                        orb1 = Color(0xFF00C2FF),
                        orb2 = Color(0xFF3A5BFF),
                        orb3 = Color(0xFF7B2CBF),
                        glassTint = Color(0xFFFFFFFF),
                        accent = Color(0xFF38BDF8),
                        secondaryAccent = Color(0xFF818CF8),
                        textPrimary = Color(0xFFFFFFFF),
                        textSecondary = Color(0xB8E2E8F0)
                    )
                }
            }
        }
    }

    val spec = tween<Color>(durationMillis = 650)
    val bgBase by animateColorAsState(target.bgBase, spec, label = "bgBase")
    val orb1 by animateColorAsState(target.orb1, spec, label = "orb1")
    val orb2 by animateColorAsState(target.orb2, spec, label = "orb2")
    val orb3 by animateColorAsState(target.orb3, spec, label = "orb3")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val secondary by animateColorAsState(target.secondaryAccent, spec, label = "secondary")

    return target.copy(
        bgBase = bgBase,
        orb1 = orb1,
        orb2 = orb2,
        orb3 = orb3,
        accent = accent,
        secondaryAccent = secondary
    )
}

fun buildCustomTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
    )
}

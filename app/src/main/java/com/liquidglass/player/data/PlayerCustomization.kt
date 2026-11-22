package com.liquidglass.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.playerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "liquid_glass_player_settings")

enum class PlayerThemePreset(val title: String) {
    ALBUM_DYNAMIC("От обложки"),
    LIQUID_OCEAN("Liquid Ocean"),
    MIDNIGHT_AURORA("Midnight Aurora"),
    SUNSET_PRISM("Sunset Prism"),
    NEON_CYBER("Neon Cyber"),
    EMERALD_GLASS("Emerald Glass"),
    FROST_LIGHT("Frost Light"),
    AMOLED_OBSIDIAN("AMOLED Black"),
    MATERIAL_YOU("Material You")
}

enum class RefreshRateMode(val title: String, val targetHz: Float) {
    AUTO("Авто", 0f),
    HZ_60("60 Гц", 60f),
    HZ_90("90 Гц", 90f),
    HZ_120("120 Гц", 120f),
    MAX("Макс. Гц", 240f)
}

enum class VisualizerStyle(val title: String) {
    WAVE("Волна"),
    BARS("Спектр"),
    ORB("Сфера"),
    OFF("Выкл")
}

enum class FontPreset(val title: String) {
    SYSTEM_SANS("Стандартный"),
    SERIF("С засечками"),
    MONOSPACE("Моноширинный"),
    CURSIVE("Рукописный"),
    CUSTOM("Свой шрифт")
}

enum class AnimationSmoothness(val title: String, val damping: Float, val stiffness: Float) {
    SNAPPY("Резкая", 0.85f, 900f),
    FLUID_120("120 Гц шёлк", 0.78f, 380f),
    DREAMY("Мягкая", 0.72f, 220f)
}

data class PlayerCustomizationConfig(
    val themePreset: PlayerThemePreset = PlayerThemePreset.ALBUM_DYNAMIC,
    val refreshRateMode: RefreshRateMode = RefreshRateMode.HZ_120,
    val smoothness: AnimationSmoothness = AnimationSmoothness.FLUID_120,
    val showFpsBadge: Boolean = false,
    val glassOpacity: Float = 0.22f,
    val blurRadius: Float = 28f,
    val specularIntensity: Float = 0.90f,
    val chromaticEdge: Float = 0.65f,
    val cornerRadiusDp: Float = 26f,
    val motionBlurEnabled: Boolean = true,
    val motionBlurIntensity: Float = 0.85f,
    val animatedBackground: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.WAVE,
    val vinylSpinEffect: Boolean = true,
    val fontPreset: FontPreset = FontPreset.SYSTEM_SANS,
    val customFontPath: String = "",
    val customFontName: String = "",
    val fontScale: Float = 1.0f,
    val hapticFeedback: Boolean = true
)

class PlayerSettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preset")
        val REFRESH_RATE = stringPreferencesKey("refresh_rate")
        val SMOOTHNESS = stringPreferencesKey("smoothness")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val BLUR_RADIUS = floatPreferencesKey("blur_radius")
        val SPECULAR = floatPreferencesKey("specular")
        val CHROMATIC = floatPreferencesKey("chromatic")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val MOTION_BLUR = booleanPreferencesKey("motion_blur")
        val MOTION_BLUR_INTENSITY = floatPreferencesKey("motion_blur_intensity")
        val ANIMATED_BG = booleanPreferencesKey("animated_bg")
        val ANIM_SPEED = floatPreferencesKey("anim_speed")
        val VISUALIZER = stringPreferencesKey("visualizer")
        val VINYL_SPIN = booleanPreferencesKey("vinyl_spin")
        val FONT_PRESET = stringPreferencesKey("font_preset")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HAPTIC = booleanPreferencesKey("haptic")
    }

    val configFlow: Flow<PlayerCustomizationConfig> = context.playerSettingsDataStore.data.map { p ->
        PlayerCustomizationConfig(
            themePreset = p[Keys.THEME]?.let { runCatching { PlayerThemePreset.valueOf(it) }.getOrNull() }
                ?: PlayerThemePreset.ALBUM_DYNAMIC,
            refreshRateMode = p[Keys.REFRESH_RATE]?.let { runCatching { RefreshRateMode.valueOf(it) }.getOrNull() }
                ?: RefreshRateMode.HZ_120,
            smoothness = p[Keys.SMOOTHNESS]?.let { runCatching { AnimationSmoothness.valueOf(it) }.getOrNull() }
                ?: AnimationSmoothness.FLUID_120,
            showFpsBadge = p[Keys.SHOW_FPS] ?: false,
            glassOpacity = p[Keys.GLASS_OPACITY] ?: 0.22f,
            blurRadius = p[Keys.BLUR_RADIUS] ?: 28f,
            specularIntensity = p[Keys.SPECULAR] ?: 0.90f,
            chromaticEdge = p[Keys.CHROMATIC] ?: 0.65f,
            cornerRadiusDp = p[Keys.CORNER_RADIUS] ?: 26f,
            motionBlurEnabled = p[Keys.MOTION_BLUR] ?: true,
            motionBlurIntensity = p[Keys.MOTION_BLUR_INTENSITY] ?: 0.85f,
            animatedBackground = p[Keys.ANIMATED_BG] ?: true,
            animationSpeed = p[Keys.ANIM_SPEED] ?: 1.0f,
            visualizerStyle = p[Keys.VISUALIZER]?.let { runCatching { VisualizerStyle.valueOf(it) }.getOrNull() }
                ?: VisualizerStyle.WAVE,
            vinylSpinEffect = p[Keys.VINYL_SPIN] ?: true,
            fontPreset = p[Keys.FONT_PRESET]?.let { runCatching { FontPreset.valueOf(it) }.getOrNull() }
                ?: FontPreset.SYSTEM_SANS,
            customFontPath = p[Keys.CUSTOM_FONT_PATH] ?: "",
            customFontName = p[Keys.CUSTOM_FONT_NAME] ?: "",
            fontScale = p[Keys.FONT_SCALE] ?: 1.0f,
            hapticFeedback = p[Keys.HAPTIC] ?: true
        )
    }

    suspend fun saveConfig(c: PlayerCustomizationConfig) {
        context.playerSettingsDataStore.edit { p ->
            p[Keys.THEME] = c.themePreset.name
            p[Keys.REFRESH_RATE] = c.refreshRateMode.name
            p[Keys.SMOOTHNESS] = c.smoothness.name
            p[Keys.SHOW_FPS] = c.showFpsBadge
            p[Keys.GLASS_OPACITY] = c.glassOpacity
            p[Keys.BLUR_RADIUS] = c.blurRadius
            p[Keys.SPECULAR] = c.specularIntensity
            p[Keys.CHROMATIC] = c.chromaticEdge
            p[Keys.CORNER_RADIUS] = c.cornerRadiusDp
            p[Keys.MOTION_BLUR] = c.motionBlurEnabled
            p[Keys.MOTION_BLUR_INTENSITY] = c.motionBlurIntensity
            p[Keys.ANIMATED_BG] = c.animatedBackground
            p[Keys.ANIM_SPEED] = c.animationSpeed
            p[Keys.VISUALIZER] = c.visualizerStyle.name
            p[Keys.VINYL_SPIN] = c.vinylSpinEffect
            p[Keys.FONT_PRESET] = c.fontPreset.name
            p[Keys.CUSTOM_FONT_PATH] = c.customFontPath
            p[Keys.CUSTOM_FONT_NAME] = c.customFontName
            p[Keys.FONT_SCALE] = c.fontScale
            p[Keys.HAPTIC] = c.hapticFeedback
        }
    }
}

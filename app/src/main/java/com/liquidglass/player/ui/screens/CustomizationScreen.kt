package com.liquidglass.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.player.data.*
import com.liquidglass.player.ui.components.LiquidGlass2Panel
import com.liquidglass.player.ui.components.LiquidGlassActionButton
import com.liquidglass.player.ui.theme.PlayerPalette
import java.util.Locale

@Composable
fun CustomizationScreen(
    config: PlayerCustomizationConfig,
    palette: PlayerPalette,
    supportedHz: List<Int>,
    activeHz: Int,
    onUpdate: ((PlayerCustomizationConfig) -> PlayerCustomizationConfig) -> Unit,
    onReset: () -> Unit,
    onPickCustomFont: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Кастомизация",
                    color = palette.textPrimary,
                    fontSize = (24f * config.fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "120 Гц, шрифты и Liquid Glass 2.0",
                    color = palette.textSecondary,
                    fontSize = (12f * config.fontScale).sp
                )
            }
            TextButton(onClick = onReset) {
                Text("Сброс", color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }

        // --- 120 Hz & Ultra Smoothness Card ---
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = palette.accent)
                        Text(
                            text = "Плавность и герцовка",
                            color = palette.textPrimary,
                            fontSize = (16f * config.fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Сейчас: ${activeHz} Гц",
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Поддерживаемые частоты дисплея: ${supportedHz.joinToString(", ") { "${it} Гц" }}",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RefreshRateMode.entries.forEach { mode ->
                        val selected = config.refreshRateMode == mode
                        OptionChip(
                            text = mode.title,
                            selected = selected,
                            palette = palette,
                            config = config,
                            onClick = { onUpdate { it.copy(refreshRateMode = mode) } }
                        )
                    }
                }

                Text("Физика пружинных анимаций", color = palette.textSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimationSmoothness.entries.forEach { sm ->
                        val selected = config.smoothness == sm
                        OptionChip(
                            text = sm.title,
                            selected = selected,
                            palette = palette,
                            config = config,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdate { it.copy(smoothness = sm) } }
                        )
                    }
                }

                ToggleRow(
                    title = "Показывать индикатор Гц / FPS сверху",
                    checked = config.showFpsBadge,
                    palette = palette,
                    onCheckedChange = { v -> onUpdate { it.copy(showFpsBadge = v) } }
                )
            }
        }

        // --- Custom Font Card (.ttf / .otf) ---
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.FontDownload, contentDescription = null, tint = palette.accent)
                    Text(
                        text = "Шрифт интерфейса",
                        color = palette.textPrimary,
                        fontSize = (16f * config.fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontPreset.entries.forEach { fp ->
                        val selected = config.fontPreset == fp
                        val label = if (fp == FontPreset.CUSTOM && config.customFontName.isNotBlank()) {
                            "Свой: ${config.customFontName.take(12)}"
                        } else fp.title
                        OptionChip(
                            text = label,
                            selected = selected,
                            palette = palette,
                            config = config,
                            onClick = {
                                if (fp == FontPreset.CUSTOM && config.customFontPath.isBlank()) {
                                    onPickCustomFont()
                                } else {
                                    onUpdate { it.copy(fontPreset = fp) }
                                }
                            }
                        )
                    }
                }

                LiquidGlassActionButton(
                    palette = palette,
                    config = config,
                    accentFill = true,
                    onClick = onPickCustomFont,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Загрузить свой шрифт (.ttf / .otf)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                SliderRow(
                    title = "Масштаб шрифта",
                    valueText = "${(config.fontScale * 100).toInt()}%",
                    value = config.fontScale,
                    range = 0.85f..1.25f,
                    palette = palette,
                    onValueChange = { v -> onUpdate { it.copy(fontScale = v) } }
                )
            }
        }

        // --- Liquid Glass 2.0 Visuals & Themes ---
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Тема и эффекты Liquid Glass 2.0",
                    color = palette.textPrimary,
                    fontSize = (16f * config.fontScale).sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerThemePreset.entries.forEach { preset ->
                        OptionChip(
                            text = preset.title,
                            selected = config.themePreset == preset,
                            palette = palette,
                            config = config,
                            onClick = { onUpdate { it.copy(themePreset = preset) } }
                        )
                    }
                }

                Text("Аудио-визуализатор", color = palette.textSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisualizerStyle.entries.forEach { vs ->
                        OptionChip(
                            text = vs.title,
                            selected = config.visualizerStyle == vs,
                            palette = palette,
                            config = config,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdate { it.copy(visualizerStyle = vs) } }
                        )
                    }
                }

                SliderRow(
                    title = "Плотность жидкого стекла",
                    valueText = "${(config.glassOpacity * 100).toInt()}%",
                    value = config.glassOpacity,
                    range = 0.08f..0.55f,
                    palette = palette,
                    onValueChange = { v -> onUpdate { it.copy(glassOpacity = v) } }
                )

                SliderRow(
                    title = "Глубина размытия фона (Blur)",
                    valueText = "${config.blurRadius.toInt()} dp",
                    value = config.blurRadius,
                    range = 0f..50f,
                    palette = palette,
                    onValueChange = { v -> onUpdate { it.copy(blurRadius = v) } }
                )

                SliderRow(
                    title = "Зеркальные блики граней",
                    valueText = "${(config.specularIntensity * 100).toInt()}%",
                    value = config.specularIntensity,
                    range = 0.1f..1.0f,
                    palette = palette,
                    onValueChange = { v -> onUpdate { it.copy(specularIntensity = v) } }
                )

                SliderRow(
                    title = "Хроматическое преломление (Призма)",
                    valueText = "${(config.chromaticEdge * 100).toInt()}%",
                    value = config.chromaticEdge,
                    range = 0f..1.0f,
                    palette = palette,
                    onValueChange = { v -> onUpdate { it.copy(chromaticEdge = v) } }
                )

                SliderRow(
                    title = "Радиус скругления панелей",
                    valueText = "${config.cornerRadiusDp.toInt()} dp",
                    value = config.cornerRadiusDp,
                    range = 10f..36f,
                    palette = palette,
                    onValueChange = { v -> onUpdate { it.copy(cornerRadiusDp = v) } }
                )

                HorizontalDivider(color = palette.textSecondary.copy(alpha = 0.2f))

                ToggleRow(
                    title = "Motion Blur при свайпах и нажатиях",
                    checked = config.motionBlurEnabled,
                    palette = palette,
                    onCheckedChange = { v -> onUpdate { it.copy(motionBlurEnabled = v) } }
                )

                if (config.motionBlurEnabled) {
                    SliderRow(
                        title = "Сила Motion Blur",
                        valueText = "${(config.motionBlurIntensity * 100).toInt()}%",
                        value = config.motionBlurIntensity,
                        range = 0.2f..1.6f,
                        palette = palette,
                        onValueChange = { v -> onUpdate { it.copy(motionBlurIntensity = v) } }
                    )
                }

                ToggleRow(
                    title = "Живой флюидный фон (Aurora)",
                    checked = config.animatedBackground,
                    palette = palette,
                    onCheckedChange = { v -> onUpdate { it.copy(animatedBackground = v) } }
                )

                if (config.animatedBackground) {
                    SliderRow(
                        title = "Скорость анимации фона",
                        valueText = String.format(Locale.US, "%.1fx", config.animationSpeed),
                        value = config.animationSpeed,
                        range = 0.2f..2.5f,
                        palette = palette,
                        onValueChange = { v -> onUpdate { it.copy(animationSpeed = v) } }
                    )
                }

                ToggleRow(
                    title = "Вращение обложки (Винил)",
                    checked = config.vinylSpinEffect,
                    palette = palette,
                    onCheckedChange = { v -> onUpdate { it.copy(vinylSpinEffect = v) } }
                )

                ToggleRow(
                    title = "Тактильный виброотклик (Haptics)",
                    checked = config.hapticFeedback,
                    palette = palette,
                    onCheckedChange = { v -> onUpdate { it.copy(hapticFeedback = v) } }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionChip(
    text: String,
    selected: Boolean,
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LiquidGlass2Panel(
        palette = palette,
        config = config,
        shape = RoundedCornerShape(14.dp),
        tintOverride = if (selected) palette.accent else palette.glassTint,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    palette: PlayerPalette,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = palette.textPrimary, fontSize = 13.sp)
            Text(valueText, color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.textSecondary.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    palette: PlayerPalette,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = palette.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accent
            )
        )
    }
}

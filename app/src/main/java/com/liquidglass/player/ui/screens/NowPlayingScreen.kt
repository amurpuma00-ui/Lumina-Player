package com.liquidglass.player.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.player.data.PlayerCustomizationConfig
import com.liquidglass.player.ui.components.LiquidAudioVisualizer
import com.liquidglass.player.ui.components.LiquidGlass2Panel
import com.liquidglass.player.ui.components.LiquidGlassActionButton
import com.liquidglass.player.ui.theme.PlayerPalette
import com.liquidglass.player.viewmodel.PlayerUiState
import com.liquidglass.player.viewmodel.RepeatModeOption
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onEqBandChange: (Int, Float) -> Unit,
    onBassBoostChange: (Float) -> Unit
) {
    var showEqSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    val track = state.currentTrack
    val isFav = track != null && state.favoriteIds.contains(track.id)

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val spinAngle by if (config.vinylSpinEffect && state.isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vinyl_angle"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Utility Row (Speed, Equalizer, Sleep Timer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassActionButton(
                palette = palette,
                config = config,
                onClick = {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    val idx = speeds.indexOfFirst { kotlin.math.abs(it - state.playbackSpeed) < 0.05f }
                    onSpeedChange(speeds[(idx + 1) % speeds.size])
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.2gx", state.playbackSpeed),
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassActionButton(
                    palette = palette,
                    config = config,
                    accentFill = state.equalizer.enabled,
                    onClick = { showEqSheet = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Equalizer, contentDescription = "Эквалайзер", tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        Text("EQ", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LiquidGlassActionButton(
                    palette = palette,
                    config = config,
                    accentFill = state.sleepTimerRemainingSec > 0,
                    onClick = { showSleepDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = "Таймер сна", tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        if (state.sleepTimerRemainingSec > 0) {
                            Text("${state.sleepTimerRemainingSec / 60}м", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Liquid Glass Album Artwork Card
        val artShape = if (config.vinylSpinEffect) CircleShape else RoundedCornerShape(config.cornerRadiusDp.dp)
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            shape = artShape,
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier
                .size(256.dp)
                .rotate(if (config.vinylSpinEffect) spinAngle else 0f)
        ) {
            val bmp = state.currentAlbumArt
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Обложка трека",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(artShape)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = palette.accent.copy(alpha = 0.85f),
                        modifier = Modifier.size(110.dp)
                    )
                }
            }
        }

        // Live Audio Visualizer
        LiquidAudioVisualizer(
            style = config.visualizerStyle,
            isPlaying = state.isPlaying,
            palette = palette,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        // Track Title & Favorite
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "Выберите трек",
                    color = palette.textPrimary,
                    fontSize = (21f * config.fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist ?: "Откройте вкладку «Музыка»",
                    color = palette.textSecondary,
                    fontSize = (14f * config.fontScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (track != null) {
                LiquidGlassActionButton(
                    palette = palette,
                    config = config,
                    onClick = { onToggleFavorite(track.id) },
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (isFav) palette.secondaryAccent else palette.textPrimary
                    )
                }
            }
        }

        // Seekbar Panel
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                val progress = (state.positionMs.toFloat() / state.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                Slider(
                    value = progress,
                    onValueChange = { frac -> onSeek((frac * state.durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.textSecondary.copy(alpha = 0.25f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(state.positionMs), color = palette.textSecondary, fontSize = 12.sp)
                    Text(formatDuration(state.durationMs), color = palette.textSecondary, fontSize = 12.sp)
                }
            }
        }

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassActionButton(
                palette = palette,
                config = config,
                accentFill = state.shuffleEnabled,
                onClick = onToggleShuffle,
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Перемешать", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
            }

            LiquidGlassActionButton(
                palette = palette,
                config = config,
                onClick = onPrev,
                contentPadding = PaddingValues(14.dp)
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Предыдущий", tint = palette.textPrimary, modifier = Modifier.size(28.dp))
            }

            LiquidGlassActionButton(
                palette = palette,
                config = config,
                accentFill = true,
                onClick = onPlayPause,
                contentPadding = PaddingValues(20.dp)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Воспроизвести / Пауза",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            LiquidGlassActionButton(
                palette = palette,
                config = config,
                onClick = onNext,
                contentPadding = PaddingValues(14.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Следующий", tint = palette.textPrimary, modifier = Modifier.size(28.dp))
            }

            LiquidGlassActionButton(
                palette = palette,
                config = config,
                accentFill = state.repeatMode != RepeatModeOption.OFF,
                onClick = onCycleRepeat,
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatModeOption.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Повтор",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Equalizer Bottom Sheet
    if (showEqSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEqSheet = false },
            containerColor = palette.bgBase.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Эквалайзер и Bass Boost", color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = state.equalizer.enabled,
                        onCheckedChange = onToggleEq,
                        colors = SwitchDefaults.colors(checkedTrackColor = palette.accent)
                    )
                }

                state.equalizer.bandLabels.forEachIndexed { idx, label ->
                    val level = state.equalizer.bandLevels.getOrElse(idx) { 0.5f }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(label, color = palette.textSecondary, fontSize = 12.sp, modifier = Modifier.width(50.dp))
                        Slider(
                            value = level,
                            onValueChange = { onEqBandChange(idx, it) },
                            enabled = state.equalizer.enabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                        )
                    }
                }

                Text("Bass Boost: ${(state.equalizer.bassBoost * 100).toInt()}%", color = palette.accent, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = state.equalizer.bassBoost,
                    onValueChange = onBassBoostChange,
                    enabled = state.equalizer.enabled,
                    colors = SliderDefaults.colors(thumbColor = palette.secondaryAccent, activeTrackColor = palette.secondaryAccent)
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = palette.bgBase.copy(alpha = 0.95f),
            title = { Text("Таймер сна", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Выключить", 15 to "15 минут", 30 to "30 минут", 45 to "45 минут", 60 to "1 час").forEach { (mins, label) ->
                        LiquidGlass2Panel(
                            palette = palette,
                            config = config,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSleepTimer(mins)
                                    showSleepDialog = false
                                }
                        ) {
                            Text(label, color = palette.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Закрыть", color = palette.accent)
                }
            }
        )
    }
}

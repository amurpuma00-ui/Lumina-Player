package com.liquidglass.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.player.data.AudioTrack
import com.liquidglass.player.data.PlayerCustomizationConfig
import com.liquidglass.player.data.TrackSortOrder
import com.liquidglass.player.ui.components.LiquidGlass2Panel
import com.liquidglass.player.ui.components.LiquidGlassActionButton
import com.liquidglass.player.ui.theme.PlayerPalette
import com.liquidglass.player.viewmodel.PlayerUiState
import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

@Composable
fun LibraryScreen(
    state: PlayerUiState,
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    onRequestPermission: () -> Unit,
    onScanAgain: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (TrackSortOrder) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onSelectTrack: (AudioTrack) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Моя музыка",
                    color = palette.textPrimary,
                    fontSize = (24f * config.fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.filteredTracks.size} треков на устройстве",
                    color = palette.textSecondary,
                    fontSize = (12f * config.fontScale).sp
                )
            }
            LiquidGlassActionButton(
                palette = palette,
                config = config,
                onClick = onScanAgain,
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Сканировать треки",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Glass Search Field
        LiquidGlass2Panel(
            palette = palette,
            config = config,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        "Поиск песни, исполнителя...",
                        color = palette.textSecondary,
                        fontSize = (14f * config.fontScale).sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textSecondary)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить", tint = palette.textSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = palette.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter & Sort Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlass2Panel(
                palette = palette,
                config = config,
                shape = RoundedCornerShape(50),
                tintOverride = if (state.showFavoritesOnly) palette.secondaryAccent else palette.glassTint,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                modifier = Modifier.clickable { onToggleFavoritesFilter() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (state.showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (state.showFavoritesOnly) Color.White else palette.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Избранное",
                        color = if (state.showFavoritesOnly) Color.White else palette.textPrimary,
                        fontSize = (12f * config.fontScale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            TrackSortOrder.entries.forEach { order ->
                val selected = state.sortOrder == order
                LiquidGlass2Panel(
                    palette = palette,
                    config = config,
                    shape = RoundedCornerShape(50),
                    tintOverride = if (selected) palette.accent else palette.glassTint,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.clickable { onSortChange(order) }
                ) {
                    Text(
                        text = order.title,
                        color = if (selected) Color.White else palette.textPrimary,
                        fontSize = (12f * config.fontScale).sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Content area
        if (!state.hasAudioPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LiquidGlass2Panel(
                    palette = palette,
                    config = config,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Нужен доступ к аудиофайлам",
                            color = palette.textPrimary,
                            fontSize = (17f * config.fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Разрешите доступ к скачанной музыке на Android 16, чтобы плеер нашёл все ваши MP3/FLAC треки.",
                            color = palette.textSecondary,
                            fontSize = (13f * config.fontScale).sp
                        )
                        LiquidGlassActionButton(
                            palette = palette,
                            config = config,
                            accentFill = true,
                            onClick = onRequestPermission,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Разрешить доступ", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (state.isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
        } else if (state.filteredTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LiquidGlass2Panel(palette = palette, config = config) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.MusicOff, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(42.dp))
                        Text("Треки не найдены", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Скачайте MP3 или FLAC в папку Music/Download и нажмите обновить.",
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(state.filteredTracks, key = { it.id }) { track ->
                    val isCurrent = state.currentTrack?.id == track.id
                    val isFav = state.favoriteIds.contains(track.id)

                    LiquidGlass2Panel(
                        palette = palette,
                        config = config,
                        tintOverride = if (isCurrent) palette.accent else palette.glassTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(track) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LiquidGlass2Panel(
                                palette = palette,
                                config = config,
                                shape = CircleShape,
                                tintOverride = if (isCurrent) palette.secondaryAccent else palette.accent,
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrent && state.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isCurrent) Color.White else palette.textPrimary,
                                    fontSize = (15f * config.fontScale).sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                    color = if (isCurrent) Color.White.copy(alpha = 0.85f) else palette.textSecondary,
                                    fontSize = (12f * config.fontScale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onToggleFavorite(track.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "В избранное",
                                    tint = if (isFav) palette.secondaryAccent else palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

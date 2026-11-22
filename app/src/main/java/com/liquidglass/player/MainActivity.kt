package com.liquidglass.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.player.ui.components.LiquidGlass2AuroraBackground
import com.liquidglass.player.ui.components.LiquidGlass2Panel
import com.liquidglass.player.ui.components.LiquidGlassActionButton
import com.liquidglass.player.ui.components.liquidMotionBlur
import com.liquidglass.player.ui.screens.*
import com.liquidglass.player.ui.theme.buildCustomTypography
import com.liquidglass.player.ui.theme.rememberAnimatedPlayerPalette
import com.liquidglass.player.util.CustomFontManager
import com.liquidglass.player.util.DisplayRefreshController
import com.liquidglass.player.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class NavTab(val title: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            LiquidGlassPlayerRoot(activity = this)
        }
    }
}

@Composable
fun LiquidGlassPlayerRoot(
    activity: ComponentActivity,
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()

    // Apply 120Hz / Display Refresh Rate whenever setting changes
    val supportedHz = remember { DisplayRefreshController.getSupportedRefreshRates(activity) }
    var activeHz by remember { mutableIntStateOf(DisplayRefreshController.getCurrentRefreshRate(activity)) }

    LaunchedEffect(config.refreshRateMode) {
        activeHz = DisplayRefreshController.applyRefreshRate(activity, config.refreshRateMode)
    }

    // Live FPS monitor (only active if badge is enabled)
    var measuredFps by remember { mutableIntStateOf(activeHz) }
    LaunchedEffect(config.showFpsBadge) {
        if (!config.showFpsBadge) return@LaunchedEffect
        var frames = 0
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                frames++
                val diff = now - lastTime
                if (diff >= 500_000_000L) {
                    measuredFps = ((frames * 1_000_000_000L) / diff).toInt()
                    frames = 0
                    lastTime = now
                }
            }
        }
    }

    // Audio permission launcher (Android 13–16 READ_MEDIA_AUDIO / fallback Android 12)
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(audioPermission)
        }
    }

    // Custom font (.ttf / .otf) file picker
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = viewModel.importCustomFont(uri)
            Toast.makeText(
                context,
                if (ok) "Свой шрифт успешно применён!" else "Не удалось загрузить файл шрифта (.ttf/.otf)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Resolve Custom FontFamily & Dynamic Album Palette
    val fontFamily = remember(config.fontPreset, config.customFontPath) {
        CustomFontManager.resolveFontFamily(config)
    }
    val typography = remember(fontFamily) { buildCustomTypography(fontFamily) }
    val palette = rememberAnimatedPlayerPalette(config.themePreset, state.dynamicAlbumPalette)

    val tabs = remember {
        listOf(
            NavTab("Музыка", Icons.Rounded.LibraryMusic),
            NavTab("Плеер", Icons.Rounded.PlayCircle),
            NavTab("График", Icons.Rounded.InsertChart),
            NavTab("Стиль", Icons.Rounded.Tune)
        )
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Real-time directional Motion Blur synced with pager velocity/offset
    val motionBlurX by remember(config.motionBlurEnabled, config.motionBlurIntensity) {
        derivedStateOf {
            if (!config.motionBlurEnabled) 0f
            else {
                val offset = abs(pagerState.currentPageOffsetFraction)
                val curve = (1f - abs(offset - 0.5f) * 2f).coerceAtLeast(0f) * (if (offset > 0.01f) 1f else 0f)
                val direct = (offset * 2f).coerceAtMost(1f)
                maxOf(curve, direct * 0.65f) * 24f * config.motionBlurIntensity
            }
        }
    }

    MaterialTheme(typography = typography) {
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidGlass2AuroraBackground(
                palette = palette,
                config = config,
                isPlaying = state.isPlaying
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // Optional Live 120Hz / FPS Pill
                AnimatedVisibility(visible = config.showFpsBadge) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlass2Panel(
                            palette = palette,
                            config = config,
                            shape = RoundedCornerShape(50),
                            tintOverride = palette.accent,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${activeHz} Гц • ${measuredFps} FPS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Main Horizontal Pager with Motion Blur
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .liquidMotionBlur(radiusX = motionBlurX, radiusY = motionBlurX * 0.1f)
                ) { page ->
                    when (page) {
                        0 -> LibraryScreen(
                            state = state,
                            palette = palette,
                            config = config,
                            onRequestPermission = { permissionLauncher.launch(audioPermission) },
                            onScanAgain = viewModel::scanLibrary,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSortChange = viewModel::setSortOrder,
                            onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                            onSelectTrack = { track ->
                                viewModel.playTrack(track)
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                        1 -> NowPlayingScreen(
                            state = state,
                            palette = palette,
                            config = config,
                            onPlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::playNext,
                            onPrev = viewModel::playPrevious,
                            onSeek = viewModel::seekTo,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onCycleRepeat = viewModel::cycleRepeatMode,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onSpeedChange = viewModel::setPlaybackSpeed,
                            onSleepTimer = viewModel::setSleepTimerMinutes,
                            onToggleEq = viewModel::toggleEqualizer,
                            onEqBandChange = viewModel::setEqualizerBand,
                            onBassBoostChange = viewModel::setBassBoost
                        )
                        2 -> StatsScreen(
                            stats = stats,
                            palette = palette,
                            config = config,
                            onClearStats = viewModel::clearStats
                        )
                        3 -> CustomizationScreen(
                            config = config,
                            palette = palette,
                            supportedHz = supportedHz,
                            activeHz = activeHz,
                            onUpdate = viewModel::updateConfig,
                            onReset = viewModel::resetConfig,
                            onPickCustomFont = {
                                fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                }

                // Floating Mini-Player Bar (shown when a track is loaded and not on Now Playing tab)
                AnimatedVisibility(visible = state.currentTrack != null && pagerState.currentPage != 1) {
                    val track = state.currentTrack
                    if (track != null) {
                        LiquidGlass2Panel(
                            palette = palette,
                            config = config,
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .clickable { scope.launch { pagerState.animateScrollToPage(1) } }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = palette.accent)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        color = palette.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                LiquidGlassActionButton(
                                    palette = palette,
                                    config = config,
                                    accentFill = true,
                                    onClick = viewModel::togglePlayPause,
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                LiquidGlassActionButton(
                                    palette = palette,
                                    config = config,
                                    onClick = viewModel::playNext,
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Liquid Glass Bottom Navigation Bar
                LiquidGlass2Panel(
                    palette = palette,
                    config = config,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        tabs.forEachIndexed { idx, tab ->
                            val selected = pagerState.currentPage == idx
                            LiquidGlass2Panel(
                                palette = palette,
                                config = config,
                                shape = RoundedCornerShape(50),
                                tintOverride = if (selected) palette.accent else Color.Transparent,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (selected) Color.White else palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (selected) {
                                        Text(
                                            text = tab.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

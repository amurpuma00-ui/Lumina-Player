package com.liquidglass.player.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.liquidglass.player.data.*
import com.liquidglass.player.ui.theme.PlayerPalette
import com.liquidglass.player.ui.theme.extractAlbumPalette
import com.liquidglass.player.util.CustomFontManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatModeOption(val label: String) {
    OFF("Выкл"),
    ALL("Все"),
    ONE("Один трек")
}

data class EqualizerState(
    val enabled: Boolean = false,
    val bandLevels: List<Float> = listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f), // 0f..1f
    val bandLabels: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"),
    val bassBoost: Float = 0f // 0f..1f
)

data class PlayerUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: TrackSortOrder = TrackSortOrder.DATE_ADDED,
    val showFavoritesOnly: Boolean = false,
    val currentTrack: AudioTrack? = null,
    val currentAlbumArt: Bitmap? = null,
    val dynamicAlbumPalette: PlayerPalette? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 1L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.ALL,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingSec: Int = 0,
    val isScanning: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val equalizer: EqualizerState = EqualizerState()
) {
    val filteredTracks: List<AudioTrack>
        get() {
            val base = tracks.filter { t ->
                val matchesFav = !showFavoritesOnly || favoriteIds.contains(t.id)
                val q = searchQuery.trim()
                val matchesQuery = q.isEmpty() ||
                    t.title.contains(q, ignoreCase = true) ||
                    t.artist.contains(q, ignoreCase = true) ||
                    t.album.contains(q, ignoreCase = true)
                matchesFav && matchesQuery
            }
            return when (sortOrder) {
                TrackSortOrder.DATE_ADDED -> base.sortedByDescending { it.dateAdded }
                TrackSortOrder.TITLE -> base.sortedBy { it.title.lowercase() }
                TrackSortOrder.ARTIST -> base.sortedBy { it.artist.lowercase() }
                TrackSortOrder.DURATION -> base.sortedByDescending { it.durationMs }
            }
        }
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = PlayerSettingsRepository(application)
    private val audioRepo = AudioRepository(application)
    private val statsRepo = ListeningStatsRepository(application)

    val config: StateFlow<PlayerCustomizationConfig> = settingsRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerCustomizationConfig())

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _stats = MutableStateFlow(ListeningStatsSummary())
    val stats: StateFlow<ListeningStatsSummary> = _stats.asStateFlow()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private var tickerJob: Job? = null
    private var sleepJob: Job? = null
    private var statAccumulatorSec = 0

    init {
        _uiState.update { it.copy(favoriteIds = audioRepo.loadFavoriteIds()) }
        refreshStats()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update {
                        it.copy(durationMs = exoPlayer.duration.coerceAtLeast(1L))
                    }
                    setupAudioEffectsIfNeeded()
                } else if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }
        })

        startProgressAndStatsTicker()
    }

    private fun setupAudioEffectsIfNeeded() {
        if (equalizer != null) return
        val sessionId = exoPlayer.audioSessionId
        if (sessionId <= 0) return
        runCatching {
            val eq = Equalizer(0, sessionId)
            eq.enabled = _uiState.value.equalizer.enabled
            equalizer = eq
            val bands = eq.numberOfBands.toInt().coerceAtMost(5)
            val labels = (0 until bands).map { idx ->
                val hz = eq.getCenterFreq(idx.toShort()) / 1000
                if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
            }
            _uiState.update {
                it.copy(equalizer = it.equalizer.copy(bandLabels = labels))
            }
        }
        runCatching {
            val bb = BassBoost(0, sessionId)
            bb.enabled = _uiState.value.equalizer.enabled
            bassBoost = bb
        }
    }

    private fun startProgressAndStatsTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                val playing = exoPlayer.isPlaying
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(1L)
                _uiState.update { it.copy(positionMs = pos, durationMs = dur, isPlaying = playing) }

                if (playing) {
                    statAccumulatorSec++
                    // Every 2 ticks = 1 second; flush every 4 seconds for low I/O overhead
                    if (statAccumulatorSec >= 8) {
                        val track = _uiState.value.currentTrack
                        if (track != null) {
                            statsRepo.recordPlaybackSeconds(track, 4L, incrementPlayCount = false)
                            refreshStats()
                        }
                        statAccumulatorSec = 0
                    }
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
        if (granted) scanLibrary()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val scanned = audioRepo.scanDownloadedAudio()
            _uiState.update { state ->
                state.copy(
                    tracks = scanned,
                    isScanning = false,
                    hasAudioPermission = true
                )
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        exoPlayer.setMediaItem(MediaItem.fromUri(track.contentUri))
        exoPlayer.prepare()
        exoPlayer.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
        exoPlayer.play()

        _uiState.update {
            it.copy(
                currentTrack = track,
                positionMs = 0L,
                durationMs = track.durationMs.coerceAtLeast(1L),
                isPlaying = true
            )
        }

        statsRepo.recordPlaybackSeconds(track, 1L, incrementPlayCount = true)
        refreshStats()

        viewModelScope.launch {
            val bmp = audioRepo.loadAlbumArtBitmap(track)
            val dynamicPal = extractAlbumPalette(bmp)
            _uiState.update {
                it.copy(currentAlbumArt = bmp, dynamicAlbumPalette = dynamicPal)
            }
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value.currentTrack
        if (current == null) {
            _uiState.value.filteredTracks.firstOrNull()?.let { playTrack(it) }
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, _uiState.value.durationMs))
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun playNext() {
        val list = _uiState.value.filteredTracks.ifEmpty { _uiState.value.tracks }
        if (list.isEmpty()) return
        val current = _uiState.value.currentTrack
        val next = if (_uiState.value.shuffleEnabled && list.size > 1) {
            list.filter { it.id != current?.id }.random()
        } else {
            val idx = list.indexOfFirst { it.id == current?.id }
            list[(idx + 1).mod(list.size)]
        }
        playTrack(next)
    }

    fun playPrevious() {
        if (exoPlayer.currentPosition > 3500L) {
            seekTo(0L)
            return
        }
        val list = _uiState.value.filteredTracks.ifEmpty { _uiState.value.tracks }
        if (list.isEmpty()) return
        val current = _uiState.value.currentTrack
        val idx = list.indexOfFirst { it.id == current?.id }
        val prev = list[(idx - 1).mod(list.size)]
        playTrack(prev)
    }

    private fun handleTrackEnded() {
        when (_uiState.value.repeatMode) {
            RepeatModeOption.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            RepeatModeOption.ALL -> playNext()
            RepeatModeOption.OFF -> {
                val list = _uiState.value.filteredTracks
                val idx = list.indexOfFirst { it.id == _uiState.value.currentTrack?.id }
                if (idx in 0 until list.lastIndex) playNext()
            }
        }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleRepeatMode() {
        val modes = RepeatModeOption.entries
        _uiState.update { s ->
            val next = modes[(modes.indexOf(s.repeatMode) + 1) % modes.size]
            s.copy(repeatMode = next)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        exoPlayer.playbackParameters = PlaybackParameters(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
    }

    fun toggleFavorite(trackId: Long) {
        val updated = audioRepo.toggleFavorite(trackId)
        _uiState.update { it.copy(favoriteIds = updated) }
    }

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun setSortOrder(order: TrackSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    // --- Sleep Timer ---
    fun setSleepTimerMinutes(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingSec = 0) }
            return
        }
        val totalSec = minutes * 60
        _uiState.update { it.copy(sleepTimerRemainingSec = totalSec) }
        sleepJob = viewModelScope.launch {
            var left = totalSec
            while (left > 0 && isActive) {
                delay(1000)
                left--
                _uiState.update { it.copy(sleepTimerRemainingSec = left) }
            }
            if (left == 0) {
                exoPlayer.pause()
            }
        }
    }

    // --- Equalizer & BassBoost ---
    fun toggleEqualizer(enabled: Boolean) {
        setupAudioEffectsIfNeeded()
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        _uiState.update { it.copy(equalizer = it.equalizer.copy(enabled = enabled)) }
    }

    fun setEqualizerBand(bandIndex: Int, normalized: Float) {
        setupAudioEffectsIfNeeded()
        val eq = equalizer
        val levels = _uiState.value.equalizer.bandLevels.toMutableList()
        if (bandIndex in levels.indices) {
            levels[bandIndex] = normalized.coerceIn(0f, 1f)
            _uiState.update { it.copy(equalizer = it.equalizer.copy(bandLevels = levels)) }
        }
        if (eq != null && bandIndex < eq.numberOfBands) {
            runCatching {
                val range = eq.bandLevelRange
                val min = range[0].toInt()
                val max = range[1].toInt()
                val target = (min + (max - min) * normalized).toInt().toShort()
                eq.setBandLevel(bandIndex.toShort(), target)
            }
        }
    }

    fun setBassBoost(normalized: Float) {
        setupAudioEffectsIfNeeded()
        val clamped = normalized.coerceIn(0f, 1f)
        _uiState.update { it.copy(equalizer = it.equalizer.copy(bassBoost = clamped)) }
        runCatching {
            bassBoost?.setStrength((clamped * 1000).toInt().toShort())
        }
    }

    // --- Stats ---
    fun refreshStats() {
        _stats.value = statsRepo.loadSummary()
    }

    fun clearStats() {
        statsRepo.clearStats()
        refreshStats()
    }

    // --- Customization & Custom Font ---
    fun updateConfig(transform: (PlayerCustomizationConfig) -> PlayerCustomizationConfig) {
        val next = transform(config.value)
        viewModelScope.launch { settingsRepo.saveConfig(next) }
    }

    fun resetConfig() {
        viewModelScope.launch { settingsRepo.saveConfig(PlayerCustomizationConfig()) }
    }

    fun importCustomFont(uri: Uri): Boolean {
        val imported = CustomFontManager.importFontFromUri(getApplication(), uri) ?: return false
        updateConfig {
            it.copy(
                fontPreset = FontPreset.CUSTOM,
                customFontPath = imported.path,
                customFontName = imported.displayName
            )
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        sleepJob?.cancel()
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        exoPlayer.release()
    }
}

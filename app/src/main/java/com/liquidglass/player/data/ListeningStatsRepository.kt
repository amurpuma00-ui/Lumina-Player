package com.liquidglass.player.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyListeningPoint(
    val dateKey: String,
    val dayLabel: String,
    val minutes: Float
)

data class TrackStatEntry(
    val key: String,
    val title: String,
    val artist: String,
    val playCount: Int,
    val secondsListened: Long
)

data class ArtistStatEntry(
    val artist: String,
    val secondsListened: Long,
    val playCount: Int
)

data class ListeningStatsSummary(
    val totalSeconds: Long = 0L,
    val totalPlays: Int = 0,
    val streakDays: Int = 0,
    val todaySeconds: Long = 0L,
    val last7Days: List<DailyListeningPoint> = emptyList(),
    val last30Days: List<DailyListeningPoint> = emptyList(),
    val topTracks: List<TrackStatEntry> = emptyList(),
    val topArtists: List<ArtistStatEntry> = emptyList()
)

class ListeningStatsRepository(private val context: Context) {

    private val statsFile: File by lazy { File(context.filesDir, "listening_stats.json") }
    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val weekdayFormat = SimpleDateFormat("EE", Locale("ru"))
    private val dayNumFormat = SimpleDateFormat("dd", Locale.US)

    private fun readRoot(): JSONObject {
        if (!statsFile.exists()) return JSONObject()
        return runCatching { JSONObject(statsFile.readText()) }.getOrDefault(JSONObject())
    }

    private fun writeRoot(root: JSONObject) {
        runCatching { statsFile.writeText(root.toString()) }
    }

    @Synchronized
    fun recordPlaybackSeconds(track: AudioTrack, secondsDelta: Long, incrementPlayCount: Boolean = false) {
        if (secondsDelta <= 0 && !incrementPlayCount) return
        val root = readRoot()
        val todayKey = dayKeyFormat.format(Date())

        // Daily totals
        val dailyObj = root.optJSONObject("daily") ?: JSONObject().also { root.put("daily", it) }
        dailyObj.put(todayKey, dailyObj.optLong(todayKey, 0L) + secondsDelta)

        // Overall totals
        root.put("total_seconds", root.optLong("total_seconds", 0L) + secondsDelta)
        if (incrementPlayCount) {
            root.put("total_plays", root.optInt("total_plays", 0) + 1)
        }

        // Per-track stats
        val tracksObj = root.optJSONObject("tracks") ?: JSONObject().also { root.put("tracks", it) }
        val trackKey = "${track.id}"
        val tObj = tracksObj.optJSONObject(trackKey) ?: JSONObject().apply {
            put("title", track.title)
            put("artist", track.artist)
            put("plays", 0)
            put("seconds", 0L)
        }
        tObj.put("title", track.title)
        tObj.put("artist", track.artist)
        tObj.put("seconds", tObj.optLong("seconds", 0L) + secondsDelta)
        if (incrementPlayCount) {
            tObj.put("plays", tObj.optInt("plays", 0) + 1)
        }
        tracksObj.put(trackKey, tObj)

        // Per-artist stats
        val artistsObj = root.optJSONObject("artists") ?: JSONObject().also { root.put("artists", it) }
        val aObj = artistsObj.optJSONObject(track.artist) ?: JSONObject().apply {
            put("plays", 0)
            put("seconds", 0L)
        }
        aObj.put("seconds", aObj.optLong("seconds", 0L) + secondsDelta)
        if (incrementPlayCount) {
            aObj.put("plays", aObj.optInt("plays", 0) + 1)
        }
        artistsObj.put(track.artist, aObj)

        writeRoot(root)
    }

    @Synchronized
    fun loadSummary(): ListeningStatsSummary {
        val root = readRoot()
        val dailyObj = root.optJSONObject("daily") ?: JSONObject()
        val tracksObj = root.optJSONObject("tracks") ?: JSONObject()
        val artistsObj = root.optJSONObject("artists") ?: JSONObject()

        val todayKey = dayKeyFormat.format(Date())
        val todaySeconds = dailyObj.optLong(todayKey, 0L)

        // Build 7-day chart
        val last7 = mutableListOf<DailyListeningPoint>()
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = dayKeyFormat.format(cal.time)
            val label = weekdayFormat.format(cal.time).replaceFirstChar { it.uppercase() }.take(2)
            val secs = dailyObj.optLong(key, 0L)
            last7.add(DailyListeningPoint(key, label, secs / 60f))
        }

        // Build 30-day chart
        val last30 = mutableListOf<DailyListeningPoint>()
        for (i in 29 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = dayKeyFormat.format(cal.time)
            val label = dayNumFormat.format(cal.time)
            val secs = dailyObj.optLong(key, 0L)
            last30.add(DailyListeningPoint(key, label, secs / 60f))
        }

        // Calculate streak
        var streak = 0
        for (i in 0..365) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = dayKeyFormat.format(cal.time)
            if (dailyObj.optLong(key, 0L) > 0L) {
                streak++
            } else if (i > 0) {
                break
            }
        }

        // Top tracks
        val topTracks = mutableListOf<TrackStatEntry>()
        val tKeys = tracksObj.keys()
        while (tKeys.hasNext()) {
            val k = tKeys.next()
            val obj = tracksObj.optJSONObject(k) ?: continue
            topTracks.add(
                TrackStatEntry(
                    key = k,
                    title = obj.optString("title", "Трек"),
                    artist = obj.optString("artist", "Неизвестно"),
                    playCount = obj.optInt("plays", 0),
                    secondsListened = obj.optLong("seconds", 0L)
                )
            )
        }
        topTracks.sortByDescending { it.secondsListened }

        // Top artists
        val topArtists = mutableListOf<ArtistStatEntry>()
        val aKeys = artistsObj.keys()
        while (aKeys.hasNext()) {
            val artist = aKeys.next()
            val obj = artistsObj.optJSONObject(artist) ?: continue
            topArtists.add(
                ArtistStatEntry(
                    artist = artist,
                    secondsListened = obj.optLong("seconds", 0L),
                    playCount = obj.optInt("plays", 0)
                )
            )
        }
        topArtists.sortByDescending { it.secondsListened }

        return ListeningStatsSummary(
            totalSeconds = root.optLong("total_seconds", 0L),
            totalPlays = root.optInt("total_plays", 0),
            streakDays = streak,
            todaySeconds = todaySeconds,
            last7Days = last7,
            last30Days = last30,
            topTracks = topTracks.take(8),
            topArtists = topArtists.take(6)
        )
    }

    @Synchronized
    fun clearStats() {
        if (statsFile.exists()) statsFile.delete()
    }
}

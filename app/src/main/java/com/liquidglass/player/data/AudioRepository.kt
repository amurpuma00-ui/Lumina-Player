package com.liquidglass.player.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val dateAdded: Long,
    val folderName: String
)

enum class TrackSortOrder(val title: String) {
    DATE_ADDED("Новые"),
    TITLE("По названию"),
    ARTIST("По артисту"),
    DURATION("По длительности")
}

class AudioRepository(private val context: Context) {

    private val favoritesPrefs by lazy {
        context.getSharedPreferences("player_favorites", Context.MODE_PRIVATE)
    }

    fun loadFavoriteIds(): Set<Long> {
        return favoritesPrefs.getStringSet("fav_ids", emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    fun toggleFavorite(trackId: Long): Set<Long> {
        val current = loadFavoriteIds().toMutableSet()
        if (!current.add(trackId)) current.remove(trackId)
        favoritesPrefs.edit()
            .putStringSet("fav_ids", current.map { it.toString() }.toSet())
            .apply()
        return current
    }

    suspend fun scanDownloadedAudio(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Без названия"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Неизвестный исполнитель" else rawArtist
                    val album = cursor.getString(albumCol) ?: "Сингл"
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val relPath = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                    val folder = relPath.trimEnd('/').substringAfterLast('/', "Музыка")

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    tracks.add(
                        AudioTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            folderName = folder
                        )
                    )
                }
            }
        }
        tracks
    }

    suspend fun loadAlbumArtBitmap(track: AudioTrack): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Try MediaMetadataRetriever embedded picture
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, track.contentUri)
            val bytes = retriever.embeddedPicture
            retriever.release()
            if (bytes != null) {
                return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
        // 2. Fallback to MediaStore albumart URI
        runCatching {
            context.contentResolver.openInputStream(track.albumArtUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }
}

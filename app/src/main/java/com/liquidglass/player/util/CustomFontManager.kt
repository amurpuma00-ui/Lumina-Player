package com.liquidglass.player.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import com.liquidglass.player.data.FontPreset
import com.liquidglass.player.data.PlayerCustomizationConfig
import java.io.File

object CustomFontManager {

    data class ImportedFont(val path: String, val displayName: String)

    fun importFontFromUri(context: Context, uri: Uri): ImportedFont? {
        return runCatching {
            val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val originalName = queryDisplayName(context, uri) ?: "custom_font.ttf"
            val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFile = File(fontsDir, "active_$safeName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            // Validate that Android can actually parse this font
            Typeface.createFromFile(destFile)
            ImportedFont(path = destFile.absolutePath, displayName = originalName.substringBeforeLast("."))
        }.getOrNull()
    }

    fun resolveFontFamily(config: PlayerCustomizationConfig): FontFamily {
        return when (config.fontPreset) {
            FontPreset.SYSTEM_SANS -> FontFamily.SansSerif
            FontPreset.SERIF -> FontFamily.Serif
            FontPreset.MONOSPACE -> FontFamily.Monospace
            FontPreset.CURSIVE -> FontFamily.Cursive
            FontPreset.CUSTOM -> {
                val file = File(config.customFontPath)
                if (config.customFontPath.isNotBlank() && file.exists()) {
                    runCatching { FontFamily(Typeface.createFromFile(file)) }
                        .getOrDefault(FontFamily.SansSerif)
                } else {
                    FontFamily.SansSerif
                }
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }
}

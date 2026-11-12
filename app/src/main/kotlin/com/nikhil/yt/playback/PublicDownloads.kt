/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.playback

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import com.nikhil.yt.db.entities.Song
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Keeps exactly ONE copy of every downloaded song, inside Download/Velune Music.
 *
 * Flow: ExoPlayer downloads into its private cache -> [moveFromCache] writes the
 * finished song into the public folder -> caller deletes it from the private cache.
 * Playback later reads the public file directly (see MusicService), so the song
 * exists once on disk and any music player can open it.
 */
object PublicDownloads {
    private const val TAG = "PublicDownloads"
    const val FOLDER_NAME = "Velune Music"
    private const val PREFS = "velune_public_downloads"

    private class Kind(val ext: String, val mime: String)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun has(context: Context, songId: String): Boolean = prefs(context).contains(songId)

    /**
     * Uri of the public file for [songId], or null if there is none.
     * If a mapping exists but the file was deleted from outside the app,
     * the mapping is dropped and [onMissing] is called.
     */
    fun resolve(context: Context, songId: String, onMissing: ((String) -> Unit)? = null): Uri? {
        val stored = prefs(context).getString(songId, null) ?: return null
        val uri = stored.toUri()
        val exists = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
        if (exists) return uri
        prefs(context).edit().remove(songId).apply()
        onMissing?.invoke(songId)
        return null
    }

    /** Deletes the public file (used when the user removes a download in the app). */
    fun delete(context: Context, songId: String) {
        val stored = prefs(context).getString(songId, null) ?: return
        val uri = stored.toUri()
        runCatching {
            if (uri.scheme == "file") {
                uri.path?.let { File(it).delete() }
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        }.onFailure { Log.w(TAG, "Could not delete file for $songId", it) }
        prefs(context).edit().remove(songId).apply()
    }

    /**
     * Writes the fully downloaded song from [cache] into the public folder and
     * remembers where it is. Returns null on success (the caller may now remove the
     * song from [cache]), otherwise a short reason why it failed.
     */
    fun moveFromCache(
        context: Context,
        cache: Cache,
        song: Song,
        expectedLength: Long = -1L,
    ): String? =
        try {
            moveInternal(context, cache, song, expectedLength)
        } catch (t: Throwable) {
            Log.e(TAG, "Export failed for ${song.song.id}", t)
            "${t.javaClass.simpleName}: ${t.message}"
        }

    private fun moveInternal(context: Context, cache: Cache, song: Song, expectedLength: Long): String? {
        val songId = song.song.id
        if (has(context, songId)) return null

        val spans = cache.getCachedSpans(songId)
            .filter { it.isCached && it.file != null }
            .sortedBy { it.position }
        if (spans.isEmpty()) return "no downloaded data found"

        // The spans must cover the song without holes and reach its full length.
        var total = 0L
        for (span in spans) {
            if (span.position > total) return "downloaded data has a gap"
            total = maxOf(total, span.position + span.length)
        }
        val expected = expectedLength.takeIf { it > 0 }
            ?: ContentMetadata.getContentLength(cache.getContentMetadata(songId)).takeIf { it > 0 }
        if (expected != null && total < expected) return "incomplete ($total of $expected bytes)"

        val kind = sniff(spans.first().file!!, song.format?.mimeType)
        val baseName = buildBaseName(song)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, spans, total, kind, baseName, songId)
        } else {
            saveLegacy(context, spans, total, kind, baseName, songId)
        }

        prefs(context).edit().putString(songId, uri.toString()).apply()
        return null
    }

    // Android 10+ : MediaStore.Downloads, no permission needed for our own files.
    private fun saveWithMediaStore(
        context: Context,
        spans: List<CacheSpan>,
        total: Long,
        kind: Kind,
        baseName: String,
        songId: String,
    ): Uri {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME"

        var name = "$baseName.${kind.ext}"
        findExisting(context, collection, relativePath, name)?.let { (uri, size) ->
            if (size == total) return uri // already there, reuse it, no duplicate
            name = "$baseName [$songId].${kind.ext}"
            findExisting(context, collection, relativePath, name)?.let { (uri2, size2) ->
                if (size2 == total) return uri2
                resolver.delete(uri2, null, null) // broken leftover, replace it
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, kind.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: error("could not create file in Download/$FOLDER_NAME")
        return try {
            val written = resolver.openOutputStream(uri)?.use { writeSpans(spans, it) } ?: -1L
            if (written != total) error("Wrote $written of $total bytes")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            uri
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }

    private fun findExisting(
        context: Context,
        collection: Uri,
        relativePath: String,
        name: String,
    ): Pair<Uri, Long>? =
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf("$relativePath/", name),
            null,
        )?.use { c ->
            if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) to c.getLong(1) else null
        }

    // Android 8-9 : plain file in the public Downloads folder, needs WRITE_EXTERNAL_STORAGE.
    private fun saveLegacy(
        context: Context,
        spans: List<CacheSpan>,
        total: Long,
        kind: Kind,
        baseName: String,
        songId: String,
    ): Uri {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            error("storage permission not granted")
        }
        @Suppress("DEPRECATION")
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            FOLDER_NAME,
        ).apply { mkdirs() }

        var target = File(dir, "$baseName.${kind.ext}")
        if (target.exists() && target.length() != total) {
            target = File(dir, "$baseName [$songId].${kind.ext}")
        }
        if (!(target.exists() && target.length() == total)) {
            try {
                target.outputStream().use { writeSpans(spans, it) }
                if (target.length() != total) error("Incomplete file")
            } catch (t: Throwable) {
                target.delete()
                throw t
            }
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(kind.mime), null)
        }
        return Uri.fromFile(target)
    }

    // Writes spans in order, skipping bytes already written if spans overlap.
    private fun writeSpans(spans: List<CacheSpan>, out: OutputStream): Long {
        var written = 0L
        for (span in spans) {
            val skip = (written - span.position).coerceAtLeast(0L)
            if (skip >= span.length) continue
            FileInputStream(span.file!!).use { input ->
                var toSkip = skip
                while (toSkip > 0) {
                    val n = input.skip(toSkip)
                    if (n <= 0) break
                    toSkip -= n
                }
                written += input.copyTo(out)
            }
        }
        return written
    }

    // Decide the extension from the real bytes, not from the (possibly wrong) mime type.
    private fun sniff(file: File, fallbackMime: String?): Kind {
        val b = ByteArray(12)
        val n = runCatching { FileInputStream(file).use { it.read(b) } }.getOrDefault(0)
        fun at(i: Int, s: String) = n >= i + s.length && s.indices.all { b[i + it] == s[it].code.toByte() }
        return when {
            at(4, "ftyp") -> Kind("m4a", "audio/mp4")
            n >= 4 && b[0] == 0x1A.toByte() && b[1] == 0x45.toByte() &&
                b[2] == 0xDF.toByte() && b[3] == 0xA3.toByte() -> Kind("webm", "audio/webm")
            at(0, "OggS") -> Kind("ogg", "audio/ogg")
            at(0, "ID3") || (n >= 2 && b[0] == 0xFF.toByte() && (b[1].toInt() and 0xE0) == 0xE0) ->
                Kind("mp3", "audio/mpeg")
            fallbackMime?.contains("webm") == true -> Kind("webm", "audio/webm")
            else -> Kind("m4a", "audio/mp4")
        }
    }

    private fun buildBaseName(song: Song): String {
        val title = song.song.title.ifBlank { song.song.id }
        val artist = song.artists.joinToString(", ") { it.name }
        val raw = if (artist.isNotBlank()) "$artist - $title" else title
        return raw.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(120)
    }
}

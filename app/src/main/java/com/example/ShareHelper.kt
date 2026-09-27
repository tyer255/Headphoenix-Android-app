package com.example

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.remote.models.TrackDto
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object ShareHelper {

    fun shareTrack(context: Context, track: TrackDto) {
        val shareUrl = "https://headphonix.app/track/${track.id}"
        val shareText = "Listen to \"${track.title}\" by ${track.artist} on Headphonix:\n$shareUrl"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, "${track.title} • ${track.artist}")
            putExtra(Intent.EXTRA_SUBJECT, "${track.title} by ${track.artist}")
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Share \"${track.title}\"")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    fun shareLyrics(context: Context, track: TrackDto, selectedLines: List<String>) {
        val lyricsText = selectedLines.joinToString("\n").trim()
        val textToShare = if (lyricsText.isNotEmpty()) lyricsText else "Listen to ${track.title} on Headphonix"
        
        val encodedText = try {
            URLEncoder.encode(textToShare, "UTF-8")
        } catch (e: Exception) {
            textToShare
        }

        val shareUrl = "https://headphonix.app/lyrics/${track.id}?text=$encodedText"
        val shareText = "\"$textToShare\"\n\n— ${track.title} by ${track.artist}\n$shareUrl"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, "Lyrics from ${track.title}")
            putExtra(Intent.EXTRA_SUBJECT, "Lyrics from ${track.title} • ${track.artist}")
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Share lyrics from \"${track.title}\"")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    fun shareCardImage(context: Context, track: TrackDto, bitmap: Bitmap) {
        try {
            val imagesDir = File(context.cacheDir, "shared_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val imageFile = File(imagesDir, "headphonix_card_${System.currentTimeMillis()}.png")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, imageFile)

            val shareUrl = "https://headphonix.app/track/${track.id}"
            val shareText = "Check out \"${track.title}\" by ${track.artist} on Headphonix:\n$shareUrl"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_TITLE, "${track.title} • ${track.artist}")
                clipData = ClipData.newRawUri("Track Card", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(sendIntent, "Share \"${track.title}\" card")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            // Fallback to text share if image generation or FileProvider fails
            shareTrack(context, track)
        }
    }
}

package com.grace.app.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/**
 * Renders + persists QR codes for the Event-attendance feature.
 *
 * The QR data is just a deep link (grace://event-checkin/{id}) so the rendered
 * image is valid forever — printable, shareable, screenshottable.
 *
 * - [saveToGallery] writes to `Pictures/GRACE/` via MediaStore (API 29+).
 *   On older API (26–28) it falls back to the legacy public Pictures dir
 *   (no permission needed below 29 for app-private cache files).
 * - [shareToIntent] writes to the app's cache + emits a FileProvider URI in
 *   an ACTION_SEND intent. Works on all API levels; safe from
 *   FileUriExposedException because of the FileProvider hop.
 */
object QrSaver {

    private const val DEFAULT_SIZE_PX = 1024

    /** Encode a payload + render to an opaque white-bg Bitmap. */
    fun render(payload: String, sizePx: Int = DEFAULT_SIZE_PX): Bitmap {
        val matrix = QRCodeWriter().encode(
            payload, BarcodeFormat.QR_CODE, sizePx, sizePx,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )
        )
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp).apply { drawColor(Color.WHITE) }
        val paint = Paint().apply { color = Color.BLACK }
        val cell = sizePx.toFloat() / matrix.width
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix.get(x, y)) {
                    canvas.drawRect(
                        x * cell, y * cell,
                        (x + 1) * cell, (y + 1) * cell,
                        paint
                    )
                }
            }
        }
        return bmp
    }

    /**
     * Save the QR for [payload] into the user's Pictures gallery under
     * `Pictures/GRACE/<safeName>.png`. Returns the inserted Uri on success
     * or null on failure (no need to surface specifics — UI just toasts).
     */
    fun saveToGallery(context: Context, payload: String, displayName: String): Uri? {
        val safeName = sanitize(displayName) + "-" + System.currentTimeMillis()
        val bmp = render(payload)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeName.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/GRACE"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                // Pre-Q fallback — write into the legacy public Pictures dir.
                // No runtime permission needed for the app's own cache, but
                // public Pictures writes do require WRITE_EXTERNAL_STORAGE
                // on API < 29. We declare it scoped-to-Q below in the
                // manifest so this just works without prompts on Q+.
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ),
                    "Royals"
                ).apply { if (!exists()) mkdirs() }
                val file = File(dir, "$safeName.png")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Uri.fromFile(file)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Build an ACTION_SEND chooser intent for [payload]. Writes the PNG
     * into the app's cache dir and exposes it via FileProvider — works on
     * all supported API levels without storage permissions.
     */
    fun shareIntent(
        context: Context,
        payload: String,
        displayName: String
    ): Intent? = try {
        val bmp = render(payload)
        val cacheDir = File(context.cacheDir, "qr").apply { if (!exists()) mkdirs() }
        val file = File(cacheDir, sanitize(displayName) + ".png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, displayName)
            putExtra(Intent.EXTRA_TEXT, "$displayName — scan to check in.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(send, "Share QR for $displayName").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } catch (_: Exception) {
        null
    }

    private fun sanitize(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "grace-event" }
}

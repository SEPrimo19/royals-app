package com.grace.app.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object VerseImageRenderer {

    private const val W = 1080
    private const val H = 1350

    enum class CardFont(val label: String) {
        SERIF("Serif"),
        ITALIC("Italic"),
        SANS("Sans"),
        BOLD("Bold");

        fun typeface(): Typeface = when (this) {
            SERIF -> Typeface.SERIF
            ITALIC -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            SANS -> Typeface.SANS_SERIF
            BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    data class Background(
        val name: String,
        val topColor: Int,
        val bottomColor: Int,
        val textColor: Int
    )

    val backgrounds: List<Background> = listOf(
        Background("Night", 0xFF08090F.toInt(), 0xFF14152A.toInt(), 0xFFEDE5D8.toInt()),
        Background("Gold", 0xFFC9A84C.toInt(), 0xFFE8C96A.toInt(), 0xFF08090F.toInt()),
        Background("Cream", 0xFFEDE5D8.toInt(), 0xFFE7DDCB.toInt(), 0xFF08090F.toInt()),
        Background("Purple", 0xFF3A1F5C.toInt(), 0xFF9B5DE5.toInt(), 0xFFFFFFFF.toInt()),
        Background("Green", 0xFF0F4A38.toInt(), 0xFF3ECF8E.toInt(), 0xFFFFFFFF.toInt()),
        Background("Rose", 0xFF5C1F33.toInt(), 0xFFE05C7A.toInt(), 0xFFFFFFFF.toInt()),
        Background("Sky", 0xFF12233F.toInt(), 0xFF4A7CFF.toInt(), 0xFFFFFFFF.toInt())
    )

    fun render(
        context: Context,
        verseText: String,
        reference: String,
        background: Background?,
        font: CardFont,
        imageUri: Uri?
    ): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val textColor: Int
        val photo = imageUri?.let { decodeSampled(context, it) }
        if (photo != null) {
            drawCenterCrop(canvas, photo)
            canvas.drawColor(0x80000000.toInt())
            textColor = 0xFFFFFFFF.toInt()
        } else {
            val bg = background ?: backgrounds.first()
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, H.toFloat(),
                    bg.topColor, bg.bottomColor, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
            textColor = bg.textColor
        }

        val margin = 110
        val maxTextW = W - 2 * margin
        val typeface = font.typeface()
        val quoted = "“" + verseText.trim() + "”"

        var verseLayout: StaticLayout? = null
        var chosenSize = 32f
        for (size in intArrayOf(66, 60, 54, 48, 42, 38, 34, 30)) {
            val paint = TextPaint().apply {
                isAntiAlias = true
                color = textColor
                this.typeface = typeface
                textSize = size.toFloat()
            }
            val layout = centeredLayout(quoted, paint, maxTextW)
            if (layout.height <= H - 2 * margin - 130) {
                verseLayout = layout
                chosenSize = size.toFloat()
                break
            }
            verseLayout = layout
            chosenSize = size.toFloat()
        }
        val vLayout = verseLayout!!

        val refPaint = TextPaint().apply {
            isAntiAlias = true
            color = textColor
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            textSize = (chosenSize * 0.6f).coerceAtLeast(24f)
        }
        val refLayout = centeredLayout("$reference · KJV", refPaint, maxTextW)

        val gap = 44
        val totalH = vLayout.height + gap + refLayout.height
        var y = (H - totalH) / 2f

        canvas.save(); canvas.translate(margin.toFloat(), y); vLayout.draw(canvas); canvas.restore()
        y += vLayout.height + gap
        canvas.save(); canvas.translate(margin.toFloat(), y); refLayout.draw(canvas); canvas.restore()

        return bmp
    }

    fun shareIntent(context: Context, bitmap: Bitmap): Intent? = try {
        val dir = File(context.cacheDir, "verse").apply { if (!exists()) mkdirs() }
        val file = File(dir, "verse_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Share verse").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } }
    } catch (_: Exception) {
        null
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? = try {
        val name = "verse_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Royals"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null
            context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Royals"
            ).apply { if (!exists()) mkdirs() }
            val file = File(dir, name)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Uri.fromFile(file)
        }
    } catch (_: Exception) {
        null
    }

    private fun centeredLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
            .build()

    private fun decodeSampled(context: Context, uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / sample > W * 2 || bounds.outHeight / sample > H * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    } catch (_: Exception) {
        null
    }

    private fun drawCenterCrop(canvas: Canvas, src: Bitmap) {
        val sw = src.width
        val sh = src.height
        val targetAspect = W.toFloat() / H
        val srcRect = if (sw.toFloat() / sh > targetAspect) {
            val cropW = (sh * targetAspect).toInt()
            val left = (sw - cropW) / 2
            Rect(left, 0, left + cropW, sh)
        } else {
            val cropH = (sw / targetAspect).toInt()
            val top = (sh - cropH) / 2
            Rect(0, top, sw, top + cropH)
        }
        canvas.drawBitmap(src, srcRect, Rect(0, 0, W, H), Paint().apply { isFilterBitmap = true })
    }
}

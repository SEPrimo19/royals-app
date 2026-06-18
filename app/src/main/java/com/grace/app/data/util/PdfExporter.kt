package com.grace.app.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PdfExporter {

    private const val PAGE_W = 612
    private const val PAGE_H = 792
    private const val MARGIN = 40f
    private const val LINE_GAP = 6f

    private const val COVER_LOGO_H = 64f
    private const val SIMPLE_LOGO_H = 56f
    private const val FOOTER_LOGO_H = 38f
    private const val FOOTER_LOGO_W = 130f
    private const val FOOTER_BLOCK_H = 70f
    private const val FOOTER_TOP_PAD = 8f

    private const val CHURCH_NAME = "CHURCH OF THE NAZARENE"
    private val CHURCH_ADDRESS_LINES = listOf(
        "Purok 6, Feeder Road St. Pob. Norte, San Isidro",
        "6409, Northern Samar"
    )
    private val FOOTER_CONTACT_LINES = listOf(
        "Purok 6, Feeder Road St., Pob. Norte, San Isidro, 6409 Northern Samar",
        "Cellphone No.: 09106265459",
        "Email: jhonclarencerulona19@gmail.com",
        "Facebook Page: https://www.facebook.com/profile.php?id=61571241399222"
    )

    data class PdfReport(
        val title: String,
        val subtitle: String? = null,
        val cover: CoverInfo? = null,
        val sections: List<PdfBlock>
    )

    data class CoverInfo(
        val ownerName: String,
        val ownerSubtitle: String? = null,
        val dateGenerated: LocalDateTime = LocalDateTime.now(),
        val extras: List<Pair<String, String>> = emptyList()
    )

    sealed interface PdfBlock { val heading: String }

    data class PdfSection(
        override val heading: String,
        val rows: List<PdfRow>
    ) : PdfBlock

    data class PdfTwoColumn(
        override val heading: String,
        val pairs: List<Pair<String, String>>
    ) : PdfBlock

    data class PdfTable(
        override val heading: String,
        val columns: List<String>,
        val rows: List<List<PdfTableCell>>,
        val columnWeights: List<Float> = emptyList()
    ) : PdfBlock

    data class PdfTableCell(val text: String, val color: Int? = null)

    data class PdfRow(val left: String, val right: String? = null)

    private fun titlePaint() = Paint().apply {
        color = Color.rgb(0x08, 0x09, 0x0F)
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    private fun coverChurchPaint() = Paint().apply {
        color = Color.rgb(0x2A, 0x5A, 0x52)
        textSize = 14f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.1f
    }
    private fun coverAddressPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 10f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private fun coverFieldLabelPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 11f
        isAntiAlias = true
    }
    private fun coverTitlePaint() = Paint().apply {
        color = Color.rgb(0x08, 0x09, 0x0F)
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private fun coverFieldCenterPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 11f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private fun subtitlePaint() = Paint().apply {
        color = Color.rgb(0x7A, 0x64, 0x30)
        textSize = 11f
        isAntiAlias = true
    }
    private fun headingPaint() = Paint().apply {
        color = Color.rgb(0xC9, 0xA8, 0x4C)
        textSize = 12f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        letterSpacing = 0.12f
    }
    private fun bodyPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 11.5f
        isAntiAlias = true
    }
    private fun rightPaint() = bodyPaint().apply {
        textAlign = Paint.Align.RIGHT
        color = Color.GRAY
    }
    private fun rulePaint() = Paint().apply {
        color = Color.rgb(0xE5, 0xE5, 0xE5)
        strokeWidth = 0.5f
    }
    private fun genStampPaint() = Paint().apply {
        color = Color.GRAY
        textSize = 9f
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }
    private fun footerContactPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 8.5f
        isAntiAlias = true
    }
    private fun pagePaint() = Paint().apply {
        color = Color.GRAY
        textSize = 9f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    fun render(context: Context, report: PdfReport): ByteArray {
        val logo = loadAsset(context, "logo.png")
        val footerImg = loadAsset(context, "footer.png")
        val background = loadAsset(context, "background.png")

        val doc = PdfDocument()
        val title = titlePaint()
        val subtitle = subtitlePaint()
        val heading = headingPaint()
        val body = bodyPaint()
        val right = rightPaint()
        val rule = rulePaint()
        val stamp = genStampPaint()
        val coverChurch = coverChurchPaint()
        val coverAddr = coverAddressPaint()
        val coverField = coverFieldLabelPaint()
        val coverTitle = coverTitlePaint()
        val coverFieldCenter = coverFieldCenterPaint()
        val contactPaint = footerContactPaint()
        val pageNumPaint = pagePaint()
        val watermarkPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 130
        }

        val genStamp = "Generated: " +
            (report.cover?.dateGenerated ?: LocalDateTime.now())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        var pageNum = 0
        var pageInfo: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        var page: PdfDocument.Page = doc.startPage(pageInfo)
        var canvas: android.graphics.Canvas = page.canvas
        var cursorY: Float = MARGIN
        pageNum = 1

        fun drawWatermark(c: android.graphics.Canvas) {
            if (background == null) return
            val aspect = background.width.toFloat() / background.height.toFloat()
            val targetH = PAGE_H * 0.55f
            val targetW = targetH * aspect
            val left = (PAGE_W - targetW) / 2f
            val top = (PAGE_H - targetH) / 2f
            c.drawBitmap(
                background, null,
                RectF(left, top, left + targetW, top + targetH),
                watermarkPaint
            )
        }

        drawWatermark(canvas)

        fun startNewPage(): android.graphics.Canvas {
            pageNum += 1
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            cursorY = MARGIN
            drawWatermark(canvas)
            return canvas
        }

        fun drawFooter(c: android.graphics.Canvas, currentPage: Int) {
            val bandTop = PAGE_H - FOOTER_BLOCK_H
            if (footerImg != null) {
                val aspect = footerImg.width.toFloat() / footerImg.height.toFloat()
                val drawH = FOOTER_LOGO_H
                val drawW = (drawH * aspect).coerceAtMost(FOOTER_LOGO_W)
                val left = MARGIN
                val top = bandTop + (FOOTER_BLOCK_H - drawH - 14f) / 2f
                c.drawBitmap(
                    footerImg, null,
                    RectF(left, top, left + drawW, top + drawH),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
            }
            val textX = MARGIN + FOOTER_LOGO_W + 10f
            var ty = bandTop + 12f
            FOOTER_CONTACT_LINES.forEach { line ->
                c.drawText(line, textX, ty, contactPaint)
                ty += contactPaint.textSize + 2f
            }
            c.drawText(
                "Page $currentPage",
                PAGE_W / 2f,
                PAGE_H - 12f,
                pageNumPaint
            )
        }

        if (report.cover != null) {
            if (logo != null) {
                val aspect = logo.width.toFloat() / logo.height.toFloat()
                val drawH = COVER_LOGO_H
                val drawW = drawH * aspect
                val left = (PAGE_W - drawW) / 2f
                canvas.drawBitmap(
                    logo, null,
                    RectF(left, cursorY, left + drawW, cursorY + drawH),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                cursorY += drawH + 8f
            }
            canvas.drawText(genStamp, PAGE_W - MARGIN, MARGIN + 4f, stamp)

            canvas.drawText(CHURCH_NAME, PAGE_W / 2f,
                cursorY + coverChurch.textSize, coverChurch)
            cursorY += coverChurch.textSize + 4f
            CHURCH_ADDRESS_LINES.forEach { line ->
                canvas.drawText(line, PAGE_W / 2f,
                    cursorY + coverAddr.textSize, coverAddr)
                cursorY += coverAddr.textSize + 2f
            }
            cursorY += 10f
            canvas.drawLine(MARGIN, cursorY, PAGE_W - MARGIN, cursorY, rule)
            val headerBottom = cursorY

            val c = report.cover
            val titleH = coverTitle.textSize + 18f
            val fieldH = coverFieldCenter.textSize + LINE_GAP
            val fieldCount =
                1 +
                (if (!c.ownerSubtitle.isNullOrBlank()) 1 else 0) +
                c.extras.size +
                1
            val blockH = titleH + (fieldCount * fieldH)

            val availTop = headerBottom + 12f
            val availBottom = PAGE_H - FOOTER_BLOCK_H - 12f
            val blockTop = (availTop + availBottom - blockH) / 2f
            cursorY = blockTop

            canvas.drawText(report.title, PAGE_W / 2f,
                cursorY + coverTitle.textSize, coverTitle)
            cursorY += coverTitle.textSize + 18f

            canvas.drawText("Name: ${c.ownerName}", PAGE_W / 2f,
                cursorY + coverFieldCenter.textSize, coverFieldCenter)
            cursorY += coverFieldCenter.textSize + LINE_GAP
            c.ownerSubtitle?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, PAGE_W / 2f,
                    cursorY + coverFieldCenter.textSize, coverFieldCenter)
                cursorY += coverFieldCenter.textSize + LINE_GAP
            }
            c.extras.forEach { (label, value) ->
                canvas.drawText("$label: $value", PAGE_W / 2f,
                    cursorY + coverFieldCenter.textSize, coverFieldCenter)
                cursorY += coverFieldCenter.textSize + LINE_GAP
            }
            canvas.drawText(
                "Date: " + c.dateGenerated.format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                PAGE_W / 2f, cursorY + coverFieldCenter.textSize, coverFieldCenter
            )
            cursorY += coverFieldCenter.textSize + LINE_GAP

            drawFooter(canvas, pageNum)
            doc.finishPage(page)
            canvas = startNewPage()
        } else {
            if (logo != null) {
                val aspect = logo.width.toFloat() / logo.height.toFloat()
                val drawH = SIMPLE_LOGO_H
                val drawW = drawH * aspect
                canvas.drawBitmap(
                    logo, null,
                    RectF(
                        PAGE_W - MARGIN - drawW, cursorY,
                        PAGE_W - MARGIN, cursorY + drawH
                    ),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
            }
            canvas.drawText(genStamp, PAGE_W - MARGIN,
                cursorY + SIMPLE_LOGO_H + 12f, stamp)
            canvas.drawText(report.title, MARGIN,
                cursorY + title.textSize, title)
            cursorY += title.textSize + LINE_GAP
            report.subtitle?.let {
                canvas.drawText(it, MARGIN, cursorY + subtitle.textSize, subtitle)
                cursorY += subtitle.textSize + LINE_GAP
            }
            cursorY = maxOf(cursorY, MARGIN + SIMPLE_LOGO_H) + 8f
            canvas.drawLine(MARGIN, cursorY, PAGE_W - MARGIN, cursorY, rule)
            cursorY += 12f
        }

        val contentBottom = PAGE_H - FOOTER_BLOCK_H - FOOTER_TOP_PAD

        fun drawWrappedLeft(text: String, paint: Paint, maxWidth: Float): Float {
            if (text.isEmpty()) return paint.textSize + LINE_GAP
            var remaining = text
            var used = 0f
            while (remaining.isNotEmpty()) {
                val chars = paint.breakText(remaining, true, maxWidth, null)
                if (chars <= 0) break
                var line = remaining.substring(0, chars)
                if (chars < remaining.length) {
                    val lastSpace = line.lastIndexOf(' ')
                    if (lastSpace > 0 && lastSpace > chars / 2) {
                        line = line.substring(0, lastSpace)
                    }
                }
                if (cursorY + paint.textSize > contentBottom) {
                    drawFooter(canvas, pageNum)
                    doc.finishPage(page)
                    canvas = startNewPage()
                }
                canvas.drawText(line.trimEnd(), MARGIN,
                    cursorY + paint.textSize, paint)
                cursorY += paint.textSize + LINE_GAP
                used += paint.textSize + LINE_GAP
                remaining = remaining.substring(line.length).trimStart()
            }
            return used
        }

        fun ensureSpace(needed: Float) {
            if (cursorY + needed > contentBottom) {
                drawFooter(canvas, pageNum)
                doc.finishPage(page)
                canvas = startNewPage()
            }
        }

        val tableHeader = Paint(body).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.BLACK
        }
        val cellPaint = Paint(body)

        for (section in report.sections) {
            ensureSpace(heading.textSize + 18f)
            canvas.drawText(
                section.heading.uppercase(), MARGIN,
                cursorY + heading.textSize, heading
            )
            cursorY += heading.textSize + 6f
            canvas.drawLine(MARGIN, cursorY, PAGE_W - MARGIN, cursorY, rule)
            cursorY += 6f

            when (section) {
                is PdfSection -> for (row in section.rows) {
                    if (row.right.isNullOrBlank()) {
                        drawWrappedLeft(row.left, body, PAGE_W - 2 * MARGIN)
                    } else {
                        ensureSpace(body.textSize + LINE_GAP)
                        val rightTextWidth = right.measureText(row.right)
                        val leftMaxWidth = (PAGE_W - 2 * MARGIN) - rightTextWidth - 12f
                        val firstChunk = body.breakText(
                            row.left, true, leftMaxWidth, null)
                        val firstLine = if (firstChunk >= row.left.length) row.left
                            else row.left.substring(0, firstChunk).trimEnd()
                        canvas.drawText(firstLine, MARGIN,
                            cursorY + body.textSize, body)
                        canvas.drawText(row.right, PAGE_W - MARGIN,
                            cursorY + body.textSize, right)
                        cursorY += body.textSize + LINE_GAP
                        if (firstChunk < row.left.length) {
                            val rest = row.left.substring(firstChunk).trimStart()
                            drawWrappedLeft(rest, body, PAGE_W - 2 * MARGIN)
                        }
                    }
                }

                is PdfTwoColumn -> {
                    val colWidth = (PAGE_W - 2 * MARGIN) / 2f
                    val pairs = section.pairs
                    var i = 0
                    while (i < pairs.size) {
                        ensureSpace(body.textSize + LINE_GAP)
                        val left = "${pairs[i].first}: ${pairs[i].second}"
                        canvas.drawText(left, MARGIN,
                            cursorY + body.textSize, body)
                        if (i + 1 < pairs.size) {
                            val rightText =
                                "${pairs[i + 1].first}: ${pairs[i + 1].second}"
                            canvas.drawText(rightText, MARGIN + colWidth,
                                cursorY + body.textSize, body)
                        }
                        cursorY += body.textSize + LINE_GAP
                        i += 2
                    }
                }

                is PdfTable -> {
                    val cols = section.columns
                    val tableWidth = PAGE_W - 2 * MARGIN
                    val weights = if (section.columnWeights.size == cols.size)
                        section.columnWeights else List(cols.size) { 1f }
                    val totalWeight = weights.sum().coerceAtLeast(0.01f)
                    val widths = weights.map { (it / totalWeight) * tableWidth }
                    val xs = FloatArray(cols.size)
                    var x = MARGIN
                    for (c in cols.indices) {
                        xs[c] = x
                        x += widths[c]
                    }

                    ensureSpace(tableHeader.textSize + LINE_GAP + 4f)
                    for (c in cols.indices) {
                        canvas.drawText(cols[c], xs[c],
                            cursorY + tableHeader.textSize, tableHeader)
                    }
                    cursorY += tableHeader.textSize + 3f
                    canvas.drawLine(MARGIN, cursorY,
                        PAGE_W - MARGIN, cursorY, rule)
                    cursorY += 4f

                    for (row in section.rows) {
                        ensureSpace(body.textSize + LINE_GAP)
                        for (c in cols.indices) {
                            val cell = row.getOrNull(c) ?: continue
                            cellPaint.color = cell.color ?: body.color
                            canvas.drawText(cell.text, xs[c],
                                cursorY + body.textSize, cellPaint)
                        }
                        cursorY += body.textSize + LINE_GAP
                    }
                }
            }
            cursorY += 12f
        }

        drawFooter(canvas, pageNum)
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        logo?.recycle()
        footerImg?.recycle()
        background?.recycle()
        return out.toByteArray()
    }

    private fun loadAsset(context: Context, name: String): Bitmap? = try {
        context.assets.open(name).use { BitmapFactory.decodeStream(it) }
    } catch (_: Throwable) {
        null
    }
    @Suppress("unused") private val keepRectImport = Rect(0, 0, 0, 0)

    fun saveToGallery(context: Context, report: PdfReport, filename: String): Uri? {
        val bytes = runCatching { render(context, report) }.getOrNull() ?: return null
        val safe = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bytes, safe)
        } else {
            saveLegacy(context, bytes, safe)
        }
    }

    fun shareIntent(context: Context, report: PdfReport, filename: String): Intent? {
        val bytes = runCatching { render(context, report) }.getOrNull() ?: return null
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(cacheDir, safe)
        return try {
            FileOutputStream(file).use { it.write(bytes) }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.let { Intent.createChooser(it, "Share PDF") }
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveViaMediaStore(
        context: Context, bytes: ByteArray, filename: String
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/Royals"
            )
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                stream.write(bytes)
            }
            uri
        } catch (_: Throwable) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveLegacy(context: Context, bytes: ByteArray, filename: String): Uri? {
        return try {
            val dir = File(
                @Suppress("DEPRECATION")
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                "Royals"
            ).apply { mkdirs() }
            val file = File(dir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file)
        } catch (_: Throwable) {
            null
        }
    }
}

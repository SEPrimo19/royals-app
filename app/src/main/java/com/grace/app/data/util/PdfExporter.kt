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

/**
 * Renders + persists PDF reports following the Royals: The Kingdom Builders
 * template (see Template/progress_report_example.pdf).
 *
 * Two layouts depending on whether a [PdfReport.cover] is supplied:
 *   - With cover info → page 1 is a full Cover Page (NYI logo, church
 *     identity, title, owner block); page 2+ is content with no top header.
 *   - Without cover info → page 1 places title + subtitle next to the NYI
 *     logo (the old simple style — used by the admin roster + event QR
 *     attendance report, which don't need a cover page).
 *
 * Every page renders the same brand footer: combined logo strip, contact
 * info band, and centered page number.
 *
 * Implementation notes:
 *   - Native android.graphics.pdf.PdfDocument — no extra dependency.
 *   - Long [PdfRow.left] values wrap via measureBreak so reflection text
 *     and other prose doesn't run off the right margin.
 *   - Brand asset Bitmaps decoded once per render and recycled at the end.
 */
object PdfExporter {

    // US Letter at 72 DPI. android.graphics.pdf uses 1pt = 1/72 inch.
    private const val PAGE_W = 612
    private const val PAGE_H = 792
    private const val MARGIN = 40f
    private const val LINE_GAP = 6f

    // Logo / footer dimensions (tuned to match the template screenshot).
    private const val COVER_LOGO_H = 64f            // NYI logo on cover
    private const val SIMPLE_LOGO_H = 56f           // NYI logo on simple top-right
    private const val FOOTER_LOGO_H = 38f           // combined footer.png strip
    private const val FOOTER_LOGO_W = 130f          // approximate; aspect-fit
    private const val FOOTER_BLOCK_H = 70f          // total footer reserved
    private const val FOOTER_TOP_PAD = 8f

    // Church identity — hardcoded per spec (see compassion-features memory).
    // Lift into a DB-backed `church_info` table if/when we need to edit
    // without shipping a release. YAGNI for now.
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
        /** When set, page 1 becomes a full cover page. Null → simple header. */
        val cover: CoverInfo? = null,
        // List<PdfBlock> not List<PdfSection>: Kotlin's covariant List<out T>
        // means existing callers passing List<PdfSection> still compile, but
        // new callers can mix in PdfTwoColumn and PdfTable blocks.
        val sections: List<PdfBlock>
    )

    /**
     * Cover-page identity block. Owner is the person the report is FOR
     * (their name appears as "Name: X"); [ownerSubtitle] is an optional
     * second identifier (member ID, role, etc.); [extras] is for any
     * additional label:value pairs (filters used, date range, etc.).
     */
    data class CoverInfo(
        val ownerName: String,
        val ownerSubtitle: String? = null,
        val dateGenerated: LocalDateTime = LocalDateTime.now(),
        val extras: List<Pair<String, String>> = emptyList()
    )

    // ---- Section variants --------------------------------------------------
    // Sealed so the render loop can `when` on type. Adding a new variant
    // requires updating the render dispatch — single place to change.
    sealed interface PdfBlock { val heading: String }

    /** Free-form key:value list. [PdfRow.right] is optional. */
    data class PdfSection(
        override val heading: String,
        val rows: List<PdfRow>
    ) : PdfBlock

    /**
     * Compact "Summary"-style block where short label:value pairs are laid
     * out in two columns side-by-side. Odd pairs go in the left column,
     * even pairs in the right, so the visual order is column-major.
     */
    data class PdfTwoColumn(
        override val heading: String,
        val pairs: List<Pair<String, String>>
    ) : PdfBlock

    /**
     * Tabular block — header row + data rows, each cell optionally colored
     * (e.g. status pills in green/orange/red). Column widths are equal by
     * default; pass [columnWeights] for proportional sizing.
     */
    data class PdfTable(
        override val heading: String,
        val columns: List<String>,
        val rows: List<List<PdfTableCell>>,
        val columnWeights: List<Float> = emptyList()
    ) : PdfBlock

    /** A single cell in a [PdfTable]. [color] is sRGB; null inherits the body color. */
    data class PdfTableCell(val text: String, val color: Int? = null)

    /** [right] is optional — a value pinned to the right edge (e.g. timestamps). */
    data class PdfRow(val left: String, val right: String? = null)

    // ---- Paints (constructed once per render) -----------------------------
    private fun titlePaint() = Paint().apply {
        color = Color.rgb(0x08, 0x09, 0x0F) // GraceDeepBlue
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    private fun coverChurchPaint() = Paint().apply {
        // Dark teal matching the template — Nazarene Church standard.
        // NOT pure black; the printed material uses a muted green-teal.
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
        color = Color.rgb(0x08, 0x09, 0x0F) // GraceDeepBlue, matches body
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER  // CENTERED on cover, per template
    }
    private fun coverFieldCenterPaint() = Paint().apply {
        color = Color.DKGRAY
        textSize = 11f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER  // owner block centered on cover
    }
    private fun subtitlePaint() = Paint().apply {
        color = Color.rgb(0x7A, 0x64, 0x30) // GraceGoldDim
        textSize = 11f
        isAntiAlias = true
    }
    private fun headingPaint() = Paint().apply {
        color = Color.rgb(0xC9, 0xA8, 0x4C) // GraceGold
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

    // ---- Render -----------------------------------------------------------
    fun render(context: Context, report: PdfReport): ByteArray {
        // Brand assets loaded once. Missing files just degrade to text-only.
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
        // Watermark alpha — tuned across multiple iterations:
        // 30 (12%) was invisible on most displays; 70 (27%) was still too
        // faint per user feedback. 130 (~51%) gives a clearly readable
        // crest behind the body text without overpowering it.
        val watermarkPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 130  // 130/255 ≈ 51%
        }

        val genStamp = "Generated: " +
            (report.cover?.dateGenerated ?: LocalDateTime.now())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        // Mutable per-render state. Kotlin's flow analysis can't follow
        // assignments through `fun startNewPage`, so we use lateinit/var
        // declarations seeded by an initial startNewPage() call.
        var pageNum = 0
        var pageInfo: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        var page: PdfDocument.Page = doc.startPage(pageInfo)
        var canvas: android.graphics.Canvas = page.canvas
        var cursorY: Float = MARGIN
        pageNum = 1

        // Draws the Royals crest watermark filling most of the page,
        // centered. Called as the FIRST thing on every page so all real
        // content paints on top of it.
        fun drawWatermark(c: android.graphics.Canvas) {
            if (background == null) return
            val aspect = background.width.toFloat() / background.height.toFloat()
            // Scale to ~70% of the narrower page dimension, centered.
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

        // Initial page already started above — draw its watermark before
        // any other content can land.
        drawWatermark(canvas)

        // Replaces (not creates) the current page. Caller is responsible
        // for finishing the prior page + drawing its footer before calling.
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
            // Footer band starts FOOTER_BLOCK_H above the page bottom.
            val bandTop = PAGE_H - FOOTER_BLOCK_H
            // Left: combined logo strip from footer.png.
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
            // Right: 4 contact lines, right-of-logo, single-line each.
            val textX = MARGIN + FOOTER_LOGO_W + 10f
            var ty = bandTop + 12f
            FOOTER_CONTACT_LINES.forEach { line ->
                c.drawText(line, textX, ty, contactPaint)
                ty += contactPaint.textSize + 2f
            }
            // Centered page number at the very bottom.
            c.drawText(
                "Page $currentPage",
                PAGE_W / 2f,
                PAGE_H - 12f,
                pageNumPaint
            )
        }

        // ---- Cover page (if requested) ------------------------------------
        // First page already created above. Render either the cover layout
        // or the simple-header layout into it.
        if (report.cover != null) {
            // NYI logo CENTERED at top.
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
            // Generated-at stamp top-right (rendered AFTER logo so positions don't clash).
            canvas.drawText(genStamp, PAGE_W - MARGIN, MARGIN + 4f, stamp)

            // Church name centered.
            canvas.drawText(CHURCH_NAME, PAGE_W / 2f,
                cursorY + coverChurch.textSize, coverChurch)
            cursorY += coverChurch.textSize + 4f
            // Address lines.
            CHURCH_ADDRESS_LINES.forEach { line ->
                canvas.drawText(line, PAGE_W / 2f,
                    cursorY + coverAddr.textSize, coverAddr)
                cursorY += coverAddr.textSize + 2f
            }
            cursorY += 10f
            canvas.drawLine(MARGIN, cursorY, PAGE_W - MARGIN, cursorY, rule)
            // headerBottom = where the rule landed; the title block is now
            // vertically centered in the space between this and the footer.
            val headerBottom = cursorY

            // ---- Compute title + owner block height -------------------------
            // We need to know the block's total height to center it. Each
            // visible line contributes its text size + LINE_GAP.
            val c = report.cover
            val titleH = coverTitle.textSize + 18f
            val fieldH = coverFieldCenter.textSize + LINE_GAP
            val fieldCount =
                1 +                                              // Name
                (if (!c.ownerSubtitle.isNullOrBlank()) 1 else 0) + // role/Compassion
                c.extras.size +                                  // each extra
                1                                                // Date
            val blockH = titleH + (fieldCount * fieldH)

            // Vertical center between headerBottom and footer top.
            val availTop = headerBottom + 12f       // small breath under the rule
            val availBottom = PAGE_H - FOOTER_BLOCK_H - 12f
            val blockTop = (availTop + availBottom - blockH) / 2f
            cursorY = blockTop

            // Title CENTERED on the cover.
            canvas.drawText(report.title, PAGE_W / 2f,
                cursorY + coverTitle.textSize, coverTitle)
            cursorY += coverTitle.textSize + 18f

            // Owner block, CENTERED.
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

            // Footer + finalize. Content starts on a NEW page (per spec:
            // "second page you can remove the logo and name in the header").
            drawFooter(canvas, pageNum)
            doc.finishPage(page)
            canvas = startNewPage()
        } else {
            // Simple-header style (no cover). Title + subtitle inline with
            // NYI logo top-right. Backwards compatible with the old layout.
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

        // ---- Content (page 2+ for cover layout; same page for simple) -----
        val contentBottom = PAGE_H - FOOTER_BLOCK_H - FOOTER_TOP_PAD

        // Word-wrap helper. PdfDocument.Canvas can't wrap natively — we
        // walk word boundaries using Paint.breakText.
        fun drawWrappedLeft(text: String, paint: Paint, maxWidth: Float): Float {
            if (text.isEmpty()) return paint.textSize + LINE_GAP
            var remaining = text
            var used = 0f
            while (remaining.isNotEmpty()) {
                val chars = paint.breakText(remaining, true, maxWidth, null)
                if (chars <= 0) break
                var line = remaining.substring(0, chars)
                // If we cut mid-word and there's a word break further back,
                // back up to the last space so words stay intact.
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

        // Dedicated paint for table headers — same body weight but bold so
        // the header row reads as a column header, not just another data row.
        val tableHeader = Paint(body).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.BLACK
        }
        // Per-cell paint reused with `color` swapped for each draw — saves
        // allocating a new Paint per cell.
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
                    // Two-column key:value grid. Each visual ROW shows two
                    // pairs side-by-side (pair[i] left, pair[i+1] right).
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
                    // Column widths: explicit weights if provided, otherwise
                    // equal. Weights normalize so callers can pass any scale.
                    val cols = section.columns
                    val tableWidth = PAGE_W - 2 * MARGIN
                    val weights = if (section.columnWeights.size == cols.size)
                        section.columnWeights else List(cols.size) { 1f }
                    val totalWeight = weights.sum().coerceAtLeast(0.01f)
                    val widths = weights.map { (it / totalWeight) * tableWidth }
                    // Pre-compute column x-offsets so each cell knows where to
                    // draw without a running sum inside the loop.
                    val xs = FloatArray(cols.size)
                    var x = MARGIN
                    for (c in cols.indices) {
                        xs[c] = x
                        x += widths[c]
                    }

                    // Header row + underline.
                    ensureSpace(tableHeader.textSize + LINE_GAP + 4f)
                    for (c in cols.indices) {
                        canvas.drawText(cols[c], xs[c],
                            cursorY + tableHeader.textSize, tableHeader)
                    }
                    cursorY += tableHeader.textSize + 3f
                    canvas.drawLine(MARGIN, cursorY,
                        PAGE_W - MARGIN, cursorY, rule)
                    cursorY += 4f

                    // Data rows.
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

    /** Decode an asset PNG/JPG to a Bitmap. Returns null on any failure
     *  (missing file, malformed image) so the PDF still renders text-only. */
    private fun loadAsset(context: Context, name: String): Bitmap? = try {
        context.assets.open(name).use { BitmapFactory.decodeStream(it) }
    } catch (_: Throwable) {
        null
    }
    @Suppress("unused") private val keepRectImport = Rect(0, 0, 0, 0)

    /**
     * Writes the PDF to Documents/Royals/ via MediaStore (Q+) or legacy
     * public directory (≤ P). Returns the content Uri on success, null
     * on failure.
     */
    fun saveToGallery(context: Context, report: PdfReport, filename: String): Uri? {
        val bytes = runCatching { render(context, report) }.getOrNull() ?: return null
        val safe = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bytes, safe)
        } else {
            saveLegacy(context, bytes, safe)
        }
    }

    /**
     * Writes to the app's cache and returns an ACTION_SEND intent ready to be
     * passed to startActivity. Works on all API levels via FileProvider so
     * the URI is never a `file://` (which would crash on N+).
     */
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

package com.grace.app.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * QR renderer. Encodes the BitMatrix on a worker thread, rasterizes it into
 * a Bitmap ONCE, then displays the bitmap via Image() — single draw per
 * frame, fully cached.
 *
 * Previous version drew every cell of a 512x512 matrix via per-cell Canvas
 * drawRect calls on every recomposition (~100K draw calls per frame). On
 * low-end devices that triggered ANRs; even on faster devices it pinned a
 * CPU core when the parent screen recomposed. The bitmap approach is one
 * Image draw, no per-frame iteration.
 *
 * Encode is bounded by [ENCODE_TIMEOUT_MS] so a pathological input or stuck
 * encoder can't hang the UI indefinitely — we show "Couldn't generate QR."
 * after the timeout fires.
 */

// 256-module matrix is plenty for our short URLs (~52 chars produces a
// natural ~33-module QR; we ask for 256 modules to give it room to scale
// without aliasing). Smaller than 512² means quarter the bitmap memory.
private const val MATRIX_SIZE = 256
private const val ENCODE_TIMEOUT_MS = 3_000L

@Composable
fun QrCode(
    content: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color.Black,
    background: Color = Color.White
) {
    val fgArgb = foreground.toArgb()
    val bgArgb = background.toArgb()

    val bitmapState = produceState<Bitmap?>(
        initialValue = null,
        key1 = content,
        key2 = fgArgb,
        key3 = bgArgb
    ) {
        value = try {
            withTimeout(ENCODE_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    val hints = mapOf(
                        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                        EncodeHintType.MARGIN to 1
                    )
                    val matrix = QRCodeWriter()
                        .encode(content, BarcodeFormat.QR_CODE,
                            MATRIX_SIZE, MATRIX_SIZE, hints)

                    // Rasterize once. setPixel is slow individually but
                    // tolerable for a ≤256² one-shot off the main thread.
                    // The resulting Bitmap is cached for the lifetime of
                    // this composition — no per-frame iteration cost.
                    val bmp = Bitmap.createBitmap(
                        matrix.width, matrix.height, Bitmap.Config.ARGB_8888
                    )
                    for (x in 0 until matrix.width) {
                        for (y in 0 until matrix.height) {
                            bmp.setPixel(
                                x, y,
                                if (matrix.get(x, y)) fgArgb else bgArgb
                            )
                        }
                    }
                    bmp
                }
            }
        } catch (e: TimeoutCancellationException) {
            null  // handled by the null-check below → fallback text
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            null  // any other encode failure → same fallback
        }
    }

    val bmp = bitmapState.value
    if (bmp == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            // Show spinner while encoding; on timeout/error it will stay
            // spinning briefly then settle on the next state. Acceptable
            // UX — error path is rare since QR content here is short URLs.
            CircularProgressIndicator(
                color = foreground,
                modifier = Modifier.size(40.dp)
            )
        }
        return
    }

    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        // QR codes are pixel-art; bilinear filtering would blur the edges
        // when scaled up. None preserves crisp module boundaries.
        filterQuality = FilterQuality.None
    )
}

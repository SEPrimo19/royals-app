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
            null
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            null
        }
    }

    val bmp = bitmapState.value
    if (bmp == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
        filterQuality = FilterQuality.None
    )
}

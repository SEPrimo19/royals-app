package com.grace.app.presentation.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Result of a scan attempt. We collapse Google Code Scanner's
 * cancellation/error split into one sealed type so callers don't have to
 * juggle separate listeners.
 */
sealed interface QrScanResult {
    data class Success(val value: String) : QrScanResult
    /** User dismissed the scanner (back press / swipe down). Not an error. */
    data object Cancelled : QrScanResult
    /** Anything else — Play Services missing, module install failed, etc. */
    data class Error(val message: String) : QrScanResult
}

/**
 * Compose-friendly launcher for Google's hosted QR scanner.
 *
 * Returns a no-arg lambda — invoke it from a button onClick. The library
 * shows its own full-screen camera overlay (no CAMERA permission needed)
 * and calls [onResult] on the main thread when the user scans, cancels,
 * or hits an error.
 *
 * The first invocation may trigger a Play Services module download (~few
 * hundred KB); after that it's instant.
 */
@Composable
fun rememberQrScanner(onResult: (QrScanResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scanner = remember(context) { buildScanner(context) }
    return remember(scanner) {
        {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue
                    if (raw.isNullOrBlank()) {
                        onResult(QrScanResult.Error("Couldn't read that code."))
                    } else {
                        onResult(QrScanResult.Success(raw))
                    }
                }
                .addOnCanceledListener { onResult(QrScanResult.Cancelled) }
                .addOnFailureListener { ex ->
                    // Common failure: the Code Scanner module hasn't been
                    // downloaded yet. Kick off an install so the next tap
                    // works, and surface a friendly message this time.
                    requestModuleInstall(context)
                    onResult(
                        QrScanResult.Error(
                            ex.localizedMessage ?: "Scanner unavailable."
                        )
                    )
                }
        }
    }
}

private fun buildScanner(context: Context): GmsBarcodeScanner {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        // Auto-zoom helps with small / far QRs (event projected on a wall).
        .enableAutoZoom()
        .build()
    return GmsBarcodeScanning.getClient(context, options)
}

/**
 * Pre-warm the Code Scanner module so a first-time scan doesn't fail with
 * "module not installed". Fire-and-forget — failures are non-fatal.
 */
private fun requestModuleInstall(context: Context) {
    runCatching {
        val client = GmsBarcodeScanning.getClient(context)
        val request = ModuleInstallRequest.newBuilder()
            .addApi(client)
            .build()
        ModuleInstall.getClient(context).installModules(request)
    }
}

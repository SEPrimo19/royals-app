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

sealed interface QrScanResult {
    data class Success(val value: String) : QrScanResult
    data object Cancelled : QrScanResult
    data class Error(val message: String) : QrScanResult
}

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
        .enableAutoZoom()
        .build()
    return GmsBarcodeScanning.getClient(context, options)
}

private fun requestModuleInstall(context: Context) {
    runCatching {
        val client = GmsBarcodeScanning.getClient(context)
        val request = ModuleInstallRequest.newBuilder()
            .addApi(client)
            .build()
        ModuleInstall.getClient(context).installModules(request)
    }
}

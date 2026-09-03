package com.medbox.app.barcode

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Feeds camera frames to ML Kit and reports the first detected barcode value.
 * Invokes [onBarcodeDetected] at most once; further frames are ignored after that.
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
    )

    /**
     * Turkish medicine boxes (İTS) print both a linear EAN-13 (same for every box of that
     * product) and a 2D Data Matrix/QR "karekod" carrying a per-box serial number. When both are
     * visible in frame, prefer the 2D code so each physical box is treated as a distinct scan
     * instead of every box of the same product reading identically.
     */
    private fun pickBarcode(barcodes: List<Barcode>) = barcodes
        .filter { !it.rawValue.isNullOrBlank() }
        .minByOrNull { formatPriority(it.format) }

    private fun formatPriority(format: Int): Int = when (format) {
        Barcode.FORMAT_DATA_MATRIX -> 0
        Barcode.FORMAT_QR_CODE -> 1
        else -> 2
    }

    @Volatile
    private var found = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (found || mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = pickBarcode(barcodes)?.rawValue
                if (value != null && !found) {
                    found = true
                    onBarcodeDetected(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

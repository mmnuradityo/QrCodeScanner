package com.example.scannerandroid.qrcode

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val barcodeView: QrCodeFrameView,
    private val previewViewWidth: Float,
    private val previewViewHeight: Float,
    private val analyze: Action
) : ImageAnalysis.Analyzer {

    private val handler = Handler(Looper.getMainLooper())

    /**
     * This parameters will handle preview box scaling
     */
    private var scaleX = 1f
    private var scaleY = 1f

    private fun translateX(x: Float) = x * scaleX
    private fun translateY(y: Float) = y * scaleY

    private fun adjustBoundingRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val img = image.image ?: return

        scaleX = previewViewWidth / img.height.toFloat()
        scaleY = previewViewHeight / img.width.toFloat()

        val inputImage = InputImage.fromMediaImage(img, image.imageInfo.rotationDegrees)

        createScanner()
            .process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val lastPosition = barcodes.size - 1
                    val rect = barcodes[lastPosition].boundingBox
                    if (rect != null
                        && barcodeView.isValid(adjustBoundingRect(rect))
                    ) {
                        barcodes[lastPosition].rawValue?.let {
                            analyze.onResult(it)
                            barcodeView.shutdown()
                        }
                    }
                }
            }
            .addOnCompleteListener {
                image.close()
            }
            .addOnFailureListener {
                image.close()
                onFail(it)
            }

    }

    private fun createScanner() = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )


    private fun onFail(ex: Exception) {
        handler.postDelayed({
            analyze.onFail(ex)
        }, 1000)
    }

    interface Action {
        fun onResult(result: String)
        fun onFail(params: Exception)
    }

}
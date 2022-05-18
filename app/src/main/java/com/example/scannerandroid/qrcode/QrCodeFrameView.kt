package com.example.scannerandroid.qrcode

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class QrCodeFrameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private var cameraPreview = PreviewView(context, attrs)
    private var qrCodeBoxView = QrCodeBoxView(context, attrs)

    private var action: QrCodeAnalyzer.Action? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    private var isRunning = false
    private val screenAspectRatio: Int
        get() {
            val metrics = resources.displayMetrics
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    init {
        addView(cameraPreview)
        addView(qrCodeBoxView)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    fun setStokeSize(dimen: Float) {
        qrCodeBoxView.setStokeSize(dimen)
    }

    fun setStrokeColor(color: Int) {
        qrCodeBoxView.setStrokeColor(color)
    }

    fun setAction(action: QrCodeAnalyzer.Action) {
        this.action = action
    }


    fun startCamera() {
        if (action == null) throw RuntimeException("please setAction first!!!")

        try {
            cameraExecutor = Executors.newSingleThreadExecutor()
            cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture!!.addListener({
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(
                            cameraExecutor!!,
                            QrCodeAnalyzer(
                                this@QrCodeFrameView,
                                cameraPreview.width.toFloat(),
                                cameraPreview.height.toFloat(),
                                action!!
                            )
                        )
                    }

                try {
                    cameraProviderFuture!!.get()?.let {
                        it.unbindAll()
                        it.bindToLifecycle(
                            context as LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            Preview.Builder()
                                .setTargetAspectRatio(screenAspectRatio)
                                .build()
                                .apply {
                                    setSurfaceProvider(cameraPreview.surfaceProvider)
                                },
                            imageAnalyzer
                        )
                    }

                    isRunning = true
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            isRunning = false
        }
    }

    fun shutdown() {
        if (!isRunning) return

        cameraExecutor?.let {
            if (!it.isShutdown && !it.isTerminated) {
                it.shutdown()
                cameraProviderFuture?.get()?.unbindAll()
                isRunning = false
            }
        }
    }

    fun isRunning(): Boolean {
        return isRunning
    }

    internal fun isValid(recF: RectF) = qrCodeBoxView.isValid(recF)

}
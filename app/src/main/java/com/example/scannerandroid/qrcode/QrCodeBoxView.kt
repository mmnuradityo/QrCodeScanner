package com.example.scannerandroid.qrcode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
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

class QrCodeBoxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private var isRunning = false
    private var strokeColor: Int = Color.RED
    private var strokeSize: Float = 10F
    private var strokeSpace: Float = 500F
    private var space: Float = 50F
    private val cornerWidth: Int = 100
    private val trPaint: Paint = Paint()

    private var left: Float = 0F
    private var top: Float = 0F
    private var right: Float = 0F
    private var bottom: Float = 0F

    private var action: QrCodeAnalyzer.Action? = null
    private var cameraExecutor: ExecutorService? = null

    init {
        space = resources.displayMetrics.widthPixels.toFloat() / 8F
        setBackgroundColor(1711276032)
        trPaint.apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private val screenAspectRatio: Int
        get() {
            val metrics = resources.displayMetrics
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    fun setStrokeColor(@ColorInt color: Int) {
        this.strokeColor = color
    }

    fun setStokeSize(size: Float) {
        this.strokeSize = size
    }

    fun isValid(rectF: RectF): Boolean {
        return rectF.left >= left && rectF.top >= top
                && rectF.left <= (left + space) && rectF.top <= (top + space)
                && rectF.right <= right && rectF.bottom <= bottom
                && rectF.right >= (right - space) && rectF.bottom >= (bottom - space)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        left = (width / 2) - strokeSpace + space
        top = (height / 2) - strokeSpace + space
        right = (width / 2) + strokeSpace - space
        bottom = (height / 2) + strokeSpace - space

        canvas.drawRect(
            left, top, right, bottom, trPaint
        )
        canvas.drawPath(
            createCornersPath(left, top, right, bottom), createPaint()
        )
    }

    private fun createPaint() = Paint(Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeSize
        color = strokeColor
        strokeJoin = Paint.Join.MITER
    }

    private fun createCornersPath(left: Float, top: Float, right: Float, bottom: Float) =
        Path().apply {
            moveTo(left, (top + cornerWidth))
            lineTo(left, top)
            lineTo((left + cornerWidth), top)

            moveTo((right - cornerWidth), top)
            lineTo(right, top)
            lineTo(right, (top + cornerWidth))

            moveTo(left, (bottom - cornerWidth))
            lineTo(left, bottom)
            lineTo((left + cornerWidth), bottom)

            moveTo((right - cornerWidth), bottom)
            lineTo(right, bottom)
            lineTo(right, (bottom - cornerWidth))
        }

    fun setAction(action: QrCodeAnalyzer.Action) {
        this.action = action
    }

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    fun startCamera(cameraPreview: PreviewView) {
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
                                this@QrCodeBoxView, cameraPreview.width.toFloat(),
                                cameraPreview.height.toFloat(), action!!
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
}
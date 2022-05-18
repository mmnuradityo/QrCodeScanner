package com.civilians.project.qrcode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

internal class QrCodeBoxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

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

    init {
        space = resources.displayMetrics.widthPixels.toFloat() / 8F
        setBackgroundColor(1711276032)
        trPaint.apply {
            color = Color.TRANSPARENT
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    fun setStrokeColor(@ColorInt color: Int) {
        this.strokeColor = color
    }

    fun setStokeSize(size: Float) {
        this.strokeSize = size
    }

    internal fun isValid(rectF: RectF): Boolean {
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

}
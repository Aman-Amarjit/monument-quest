package com.monumentquest.ui.map

import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.views.MapView

class PerspectiveMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MapView(context, attrs) {

    var pitchDegrees: Float = 0f
        set(value) { field = value.coerceIn(0f, 70f); matrixDirty = true; invalidate() }

    private val androidCamera = Camera()
    private val drawMatrix    = Matrix()
    private val inverseMatrix = Matrix()
    private var matrixDirty   = true

    private val skyPaint = Paint()
    private var skyGradientH = 0

    private fun rebuildMatrix() {
        if (width == 0 || height == 0) return
        val cx = width / 2f
        val cy = height * 0.72f
        androidCamera.save()
        androidCamera.rotateX(pitchDegrees)
        androidCamera.getMatrix(drawMatrix)
        androidCamera.restore()
        drawMatrix.preTranslate(-cx, -cy)
        drawMatrix.postTranslate(cx, cy)
        drawMatrix.invert(inverseMatrix)
        matrixDirty = false
    }

    private fun ensureSkyGradient() {
        if (skyGradientH == height) return
        skyGradientH = height
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, height * 0.32f,
            intArrayOf(
                0xFF090D16.toInt(),
                0xCC090D16.toInt(),
                0x55090D16.toInt(),
                0x00090D16.toInt()
            ),
            floatArrayOf(0f, 0.25f, 0.60f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        matrixDirty = true; skyGradientH = 0
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (pitchDegrees == 0f) { super.dispatchDraw(canvas); return }
        if (matrixDirty) rebuildMatrix()
        canvas.drawColor(0xFF090D16.toInt())
        canvas.save()
        canvas.concat(drawMatrix)
        super.dispatchDraw(canvas)
        canvas.restore()
        ensureSkyGradient()
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.40f, skyPaint)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (pitchDegrees == 0f) return super.dispatchTouchEvent(ev)
        if (matrixDirty) rebuildMatrix()
        val transformed = MotionEvent.obtain(ev)
        transformed.transform(inverseMatrix)
        val consumed = super.dispatchTouchEvent(transformed)
        transformed.recycle()
        return consumed
    }
}

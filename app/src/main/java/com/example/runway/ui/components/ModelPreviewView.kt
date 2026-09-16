package com.example.runway.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.runway.data.local.BodyPhoto
import kotlin.math.min

// Shows the user's model (IIE, 2026).
// There are two pictures and no drawing: the photo the user supplied, and the
// render the image service produced from it. When a render exists it wins, because
// it already shows the body wearing the clothes.

class ModelPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    // The user's body photo. Nothing is shown until this is set.
    var photo: BodyPhoto? = null
        set(value) { field = value; invalidate() }

    // The finished try-on render, once there is one.
    var renderedImage: Bitmap? = null
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val sourceRect = Rect()
    private val destRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = renderedImage ?: photo?.bitmap ?: return
        if (bitmap.isRecycled) return
        drawFitted(canvas, bitmap)
    }

    // As large as it goes without distorting it.
    private fun drawFitted(canvas: Canvas, bitmap: Bitmap) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return

        val scale = min(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = (width - drawW) / 2f
        val top = (height - drawH) / 2f

        sourceRect.set(0, 0, bitmap.width, bitmap.height)
        destRect.set(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bitmap, sourceRect, destRect, paint)
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

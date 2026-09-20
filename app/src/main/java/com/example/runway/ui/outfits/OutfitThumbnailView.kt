package com.example.runway.ui.outfits

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitCanvas
import com.example.runway.ui.components.OutfitCanvasView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// A small preview of an outfit: its try-on render if it has one, otherwise the builder collage.
class OutfitThumbnailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val collage = OutfitCanvasView(context)
    private val render = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private var job: Job? = null
    // Stops a recycled list row from reloading the same pictures on every update.
    private var shownKey: String? = null

    init {
        addView(collage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(render, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(outfit: Outfit?, itemsById: Map<String, Item>, scope: CoroutineScope) {
        val key = outfit?.let { "${it.id}|${it.updatedAt}|${it.renderPath}|${itemsById.keys.hashCode()}" }
        if (key == shownKey) return
        shownKey = key
        job?.cancel()

        render.setImageDrawable(null)
        collage.images = emptyMap()
        if (outfit == null) {
            collage.layers = emptyList()
            render.isVisible = false
            return
        }

        val layers = OutfitCanvas.autoLayout(OutfitCanvas.layersFor(outfit))
        val hasRender = !outfit.renderPath.isNullOrBlank()
        render.isVisible = hasRender
        collage.layers = if (hasRender) emptyList() else layers

        val px = maxOf(width, MIN_DECODE_PX)
        job = scope.launch {
            val picture = outfit.renderPath?.takeIf { it.isNotBlank() }?.let { GarmentImages.loadPath(it, px) }
            if (picture != null) {
                render.setImageBitmap(picture)
            } else {
                // The render file can be missing on another device, so fall back to the collage.
                render.isVisible = false
                collage.layers = layers
                collage.images = GarmentImages.load(outfit.itemIds, itemsById, px / 2)
            }
        }
    }

    private companion object {
        const val MIN_DECODE_PX = 240
    }
}

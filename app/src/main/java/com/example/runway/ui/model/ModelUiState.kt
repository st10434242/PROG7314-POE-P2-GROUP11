package com.example.runway.ui.model

import android.graphics.Bitmap
import com.example.runway.data.local.BodyPhoto
import com.example.runway.data.pose.PoseFailure
import com.example.runway.domain.model.ClothingSize
import com.example.runway.domain.model.FitVerdict
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.ModelProfile

// Screen state for the virtual model (IIE, 2026).

data class ModelUiState(
    val profile: ModelProfile = ModelProfile(),
    // The wardrobe items chosen for the render. SCRUM-87.
    val worn: List<Item> = emptyList(),
    val wardrobe: List<Item> = emptyList(),
    // The user's own photo. Without it there is no model.
    val photo: BodyPhoto? = null,
    val isAnalysingPhoto: Boolean = false,
    // Why the last photo was rejected, so the screen can say what to do about it.
    val photoFailure: PoseFailure? = null,
    // Set when a photo has just filled in the detected build.
    val buildDetected: Boolean = false,
    // The finished photoreal render, once the image service has returned one.
    val renderedImage: Bitmap? = null,
    val isRendering: Boolean = false,
    val isSavingOutfit: Boolean = false,
    val savedOutfitAt: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedAt: Long? = null,
    val errorMessage: String? = null,
) {
    val hasPhoto: Boolean get() = photo != null

    val isShowingRender: Boolean get() = renderedImage != null

    // An outfit is worth saving once something is actually on the model.
    val canSaveOutfit: Boolean get() = worn.isNotEmpty() && !isSavingOutfit

    // How each worn garment's size compares with the size the user usually wears.
    // Null for a garment whose size is missing or unreadable, and for a category
    // with no usual size recorded yet.
    fun fitFor(item: Item): FitVerdict? {
        val usual = when (item.category.uppercase()) {
            "TOP", "OUTERWEAR" -> profile.topSize
            "BOTTOM" -> profile.bottomSize
            else -> null
        }
        return FitVerdict.compare(ClothingSize.parse(item.size), usual)
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

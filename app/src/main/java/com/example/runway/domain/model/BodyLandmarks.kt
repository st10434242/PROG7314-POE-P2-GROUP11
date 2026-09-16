package com.example.runway.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

// Body landmarks detected in the user's photo (IIE, 2026; Google, 2026a).
// Coordinates are stored as a fraction of the photo's width and height rather than
// in pixels, so the same numbers stay correct after the photo is scaled to fit a view.

@Serializable
data class BodyPoint(val x: Float, val y: Float)

@Serializable
data class BodyLandmarks(
    val shoulderLeft: BodyPoint,
    val shoulderRight: BodyPoint,
    val hipLeft: BodyPoint,
    val hipRight: BodyPoint,
    val kneeLeft: BodyPoint,
    val kneeRight: BodyPoint,
    val ankleLeft: BodyPoint,
    val ankleRight: BodyPoint,
    val nose: BodyPoint,
) {

    val shoulderY: Float get() = (shoulderLeft.y + shoulderRight.y) / 2f
    val hipY: Float get() = (hipLeft.y + hipRight.y) / 2f
    val kneeY: Float get() = (kneeLeft.y + kneeRight.y) / 2f
    val ankleY: Float get() = (ankleLeft.y + ankleRight.y) / 2f

    val shoulderWidth: Float get() = abs(shoulderRight.x - shoulderLeft.x)
    val hipWidth: Float get() = abs(hipRight.x - hipLeft.x)
    val torsoLength: Float get() = hipY - shoulderY

    // Nose to ankle rather than crown to heel: the crown is not a landmark.
    val visibleHeight: Float get() = ankleY - nose.y

    // Above 1 means the shoulders are wider than the hips.
    val shoulderHipRatio: Float get() = if (hipWidth <= 0f) 1f else shoulderWidth / hipWidth

    // Shoulder width as a fraction of visible height. Broad shoulders on a short
    // frame give a larger number than the same shoulders on a tall one.
    val buildRatio: Float get() = if (visibleHeight <= 0f) 0f else shoulderWidth / visibleHeight

    // A starting point for the user, not a measurement. Two-dimensional landmarks
    // cannot see depth, and the pose, camera angle and clothing all move these
    // numbers, so the screen offers the result as a suggestion the user can change.
    fun suggestedBodyShape(): BodyShape = when {
        // Hourglass is deliberately never suggested: it is defined by the waist,
        // and pose detection returns no waist landmark.
        buildRatio > OVAL_BUILD && shoulderHipRatio in NARROW_RATIO..BROAD_RATIO -> BodyShape.OVAL
        shoulderHipRatio >= BROAD_RATIO -> BodyShape.INVERTED_TRIANGLE
        shoulderHipRatio <= NARROW_RATIO -> BodyShape.TRIANGLE
        else -> BodyShape.RECTANGLE
    }

    fun suggestedBodyType(): BodyType = when {
        buildRatio < SLIM_BUILD -> BodyType.SLIM
        buildRatio < AVERAGE_BUILD -> BodyType.AVERAGE
        buildRatio < ATHLETIC_BUILD -> BodyType.ATHLETIC
        else -> BodyType.FULL
    }

    companion object {
        // Thresholds come from the usual artistic proportion that shoulders span
        // about a quarter of standing height, adjusted because the measurement here
        // runs from the nose rather than the crown. Worth re-checking against real
        // photos before release.
        const val SLIM_BUILD = 0.24f
        const val AVERAGE_BUILD = 0.28f
        const val ATHLETIC_BUILD = 0.31f
        const val OVAL_BUILD = 0.33f

        const val NARROW_RATIO = 0.92f
        const val BROAD_RATIO = 1.08f
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026a. Pose detection. [online] Available at: <https://developers.google.com/ml-kit/vision/pose-detection> [Accessed 15 September 2026].
*/

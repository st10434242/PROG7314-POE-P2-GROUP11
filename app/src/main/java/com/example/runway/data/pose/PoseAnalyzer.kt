package com.example.runway.data.pose

import android.graphics.Bitmap
import com.example.runway.domain.model.BodyLandmarks
import com.example.runway.domain.model.BodyPoint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.tasks.await

// Finds the body in a photo so garments can be placed on it (IIE, 2026; Google, 2026a).
// Runs entirely on the device: the photo is never uploaded.

enum class PoseFailure {
    // Nothing that looks like a person.
    NO_PERSON,

    // A person, but not head to foot - usually a cropped or seated photo.
    PARTIAL_BODY,

    // The image could not be read at all.
    UNREADABLE,
}

class PoseDetectionException(val failure: PoseFailure) : Exception(failure.name)

class PoseAnalyzer {

    // The accurate detector rather than the fast one: this runs once on a still
    // photo, so precision matters more than frame rate.
    private val detector by lazy {
        PoseDetection.getClient(
            AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }

    suspend fun analyse(bitmap: Bitmap): Result<BodyLandmarks> = runCatching {
        val pose: Pose = detector.process(InputImage.fromBitmap(bitmap, 0)).await()

        val found = REQUIRED.associateWith { pose.getPoseLandmark(it) }

        // A landmark is returned even when the detector is guessing, so the
        // likelihood is what separates a real full-body photo from a crop.
        if (found.values.any { it == null }) throw PoseDetectionException(PoseFailure.NO_PERSON)
        if (found.values.any { it!!.inFrameLikelihood < MIN_LIKELIHOOD }) {
            throw PoseDetectionException(PoseFailure.PARTIAL_BODY)
        }

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        if (width <= 0f || height <= 0f) throw PoseDetectionException(PoseFailure.UNREADABLE)

        fun point(type: Int): BodyPoint {
            val landmark = found.getValue(type)!!
            return BodyPoint(
                x = (landmark.position.x / width).coerceIn(0f, 1f),
                y = (landmark.position.y / height).coerceIn(0f, 1f),
            )
        }

        BodyLandmarks(
            shoulderLeft = point(PoseLandmark.LEFT_SHOULDER),
            shoulderRight = point(PoseLandmark.RIGHT_SHOULDER),
            hipLeft = point(PoseLandmark.LEFT_HIP),
            hipRight = point(PoseLandmark.RIGHT_HIP),
            kneeLeft = point(PoseLandmark.LEFT_KNEE),
            kneeRight = point(PoseLandmark.RIGHT_KNEE),
            ankleLeft = point(PoseLandmark.LEFT_ANKLE),
            ankleRight = point(PoseLandmark.RIGHT_ANKLE),
            nose = point(PoseLandmark.NOSE),
        )
    }

    fun close() = detector.close()

    private companion object {
        // Every landmark the garment placement depends on.
        val REQUIRED = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_ANKLE,
        )

        const val MIN_LIKELIHOOD = 0.5f
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026a. Pose detection. [online] Available at: <https://developers.google.com/ml-kit/vision/pose-detection> [Accessed 15 September 2026].
*/

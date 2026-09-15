package com.example.runway.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.runway.domain.model.BodyLandmarks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

// The user's body photo and the landmarks found in it (IIE, 2026).
// Both stay in the app's private storage and are never sent anywhere: only the
// body type and shape the photo suggests reach the API, as ordinary settings.

data class BodyPhoto(
    val bitmap: Bitmap,
    val landmarks: BodyLandmarks,
)

class ModelPhotoStore(context: Context) {

    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DIRECTORY)
    private val photoFile = File(directory, FILE_NAME)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // Reads the picked image, correcting the rotation the camera recorded in it.
    suspend fun decode(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        // First pass reads the dimensions only; decodeStream returns null by design
        // when inJustDecodeBounds is set, so the size is read off the options.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val sizeStream = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
        sizeStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val pixelStream = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
        val decoded = pixelStream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        val rotation = readRotation(uri)
        if (rotation == 0) decoded else rotate(decoded, rotation)
    }

    // A camera records which way up it was held instead of rotating the pixels, so
    // a portrait photo arrives sideways unless this is applied.
    private fun readRotation(uri: Uri): Int {
        val stream = appContext.contentResolver.openInputStream(uri) ?: return 0
        val orientation = stream.use {
            runCatching {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    suspend fun save(bitmap: Bitmap, landmarks: BodyLandmarks): Unit = withContext(Dispatchers.IO) {
        directory.mkdirs()
        photoFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        prefs.edit().putString(KEY_LANDMARKS, json.encodeToString(landmarks)).apply()
    }

    // Returns null rather than throwing when either half is missing: a photo with
    // no landmarks cannot place a garment, so it is treated as no photo at all.
    suspend fun load(): BodyPhoto? = withContext(Dispatchers.IO) {
        if (!photoFile.exists()) return@withContext null
        val stored = prefs.getString(KEY_LANDMARKS, null) ?: return@withContext null
        val landmarks = runCatching { json.decodeFromString<BodyLandmarks>(stored) }.getOrNull()
            ?: return@withContext null
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return@withContext null
        BodyPhoto(bitmap, landmarks)
    }

    // Called when the photo is removed and when the user signs out, so one person's
    // body photo is never left behind for the next person to sign in on the device.
    fun clear() {
        photoFile.delete()
        prefs.edit().remove(KEY_LANDMARKS).apply()
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    // Halves the image until it fits, which is all BitmapFactory accepts.
    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > MAX_DIMENSION) sample *= 2
        return sample
    }

    private companion object {
        const val DIRECTORY = "model"
        const val FILE_NAME = "body.jpg"
        const val PREFS = "runway_model_photo"
        const val KEY_LANDMARKS = "landmarks"
        const val JPEG_QUALITY = 90

        // Large enough for the detector to work with, small enough to hold in memory.
        const val MAX_DIMENSION = 1280
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

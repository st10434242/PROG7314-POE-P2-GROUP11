package com.example.runway.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.TryOnGarmentDto
import com.example.runway.data.remote.api.TryOnRequestDto
import com.example.runway.domain.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

// Sends the body photo and garment photos to the API, which renders them (IIE, 2026).
// The API holds the image service's credentials; the phone never sees them.

class TryOnRepository(
    context: Context,
    private val api: RunwayApi,
) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // A render sends the user's body photo to a third party, so it is asked for
    // once and remembered, rather than happening quietly.
    var hasConsented: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) { prefs.edit().putBoolean(KEY_CONSENT, value).apply() }

    // Only these reach the model; shoes and accessories it cannot render.
    fun canRender(item: Item): Boolean =
        !item.imagePath.isNullOrBlank() && item.category.uppercase() in RENDERABLE

    suspend fun render(person: Bitmap, garments: List<Item>): Result<Bitmap> = runCatching {
        withContext(Dispatchers.IO) {
            val body = TryOnRequestDto(
                personImage = person.toBase64(),
                garments = garments.map { item ->
                    TryOnGarmentDto(
                        image = requireNotNull(decodeFile(item.imagePath)) {
                            "The photo for ${item.name} is missing from this device"
                        }.toBase64(),
                        category = item.category.uppercase(),
                        // The model reads this as well as the picture, so the
                        // wording is built from what the user actually recorded.
                        description = describe(item),
                    )
                },
            )

            val response = api.renderTryOn(body)
            val bytes = Base64.decode(response.image, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("The render came back in a format the app could not read")
        }
    }

    private fun describe(item: Item): String = listOfNotNull(
        item.colour?.takeIf { it.isNotBlank() },
        item.brand?.takeIf { it.isNotBlank() },
        item.name,
    ).joinToString(" ")

    private fun decodeFile(path: String?): Bitmap? {
        if (path.isNullOrBlank() || !File(path).exists()) return null
        return BitmapFactory.decodeFile(path)
    }

    // Scaled and compressed before encoding: a full camera frame as base64 is
    // several megabytes of request body for no extra quality at this size.
    private fun Bitmap.toBase64(): String {
        val scaled = scaleToFit(this, MAX_DIMENSION)
        val flat = flattenOntoWhite(scaled)
        val stream = ByteArrayOutputStream()
        flat.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        if (flat != scaled) flat.recycle()
        if (scaled != this) scaled.recycle()
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    // A cut-out garment is a PNG with transparent edges, and JPEG has no
    // transparency: saving one directly turns everything around the garment
    // black, which is not something the model can make sense of. Painting it
    // onto white first gives it the plain background it expects.
    private fun flattenOntoWhite(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val flat = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return flat
    }

    private fun scaleToFit(source: Bitmap, max: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= max) return source
        val ratio = max.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt(),
            (source.height * ratio).toInt(),
            true,
        )
    }

    private companion object {
        const val FILE_NAME = "runway_tryon"
        const val KEY_CONSENT = "consented"
        const val MAX_DIMENSION = 1024
        const val JPEG_QUALITY = 88
        val RENDERABLE = setOf("TOP", "OUTERWEAR", "BOTTOM", "DRESS")
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

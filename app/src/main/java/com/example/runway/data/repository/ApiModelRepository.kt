package com.example.runway.data.repository

import android.content.Context
import com.example.runway.data.remote.api.ModelProfileDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.domain.model.BodyShape
import com.example.runway.domain.model.BodyType
import com.example.runway.domain.model.ClothingSize
import com.example.runway.domain.model.ModelProfile
import com.example.runway.domain.repository.ModelRepository

// Reads and writes the body profile through the Runway REST API (IIE, 2026; Square, Inc., n.d.).
// A local copy is kept so the screen has the measurements at start-up (Android Open Source Project, 2020b).

class ApiModelRepository(
    context: Context,
    private val api: RunwayApi,
) : ModelRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun getProfile(): Result<ModelProfile> = runCatching {
        val dto = api.getModelProfile()
        cache(dto)
        dto.toDomain()
    }

    override suspend fun saveProfile(profile: ModelProfile): Result<ModelProfile> = runCatching {
        // Cached first so the screen keeps showing the user's entries even if the
        // request fails; the next successful read reconciles it.
        cache(profile.toDto())
        val saved = api.saveModelProfile(profile.toDto())
        cache(saved)
        saved.toDomain()
    }

    override fun cachedProfile(): ModelProfile = ModelProfile(
        heightCm = prefs.getInt(KEY_HEIGHT, ModelProfile.DEFAULT_HEIGHT_CM),
        weightKg = prefs.getInt(KEY_WEIGHT, 0).takeIf { it > 0 },
        topSize = ClothingSize.from(prefs.getString(KEY_TOP_SIZE, null)),
        bottomSize = ClothingSize.from(prefs.getString(KEY_BOTTOM_SIZE, null)),
        shoeSizeEu = prefs.getInt(KEY_SHOE, 0).takeIf { it > 0 },
        bodyType = BodyType.from(prefs.getString(KEY_TYPE, null)),
        bodyShape = BodyShape.from(prefs.getString(KEY_SHAPE, null)),
    )

    private fun cache(dto: ModelProfileDto) {
        prefs.edit()
            .putInt(KEY_HEIGHT, dto.heightCm)
            .putInt(KEY_WEIGHT, dto.weightKg ?: 0)
            .putString(KEY_TOP_SIZE, dto.topSize)
            .putString(KEY_BOTTOM_SIZE, dto.bottomSize)
            .putInt(KEY_SHOE, dto.shoeSizeEu ?: 0)
            .putString(KEY_TYPE, dto.bodyType)
            .putString(KEY_SHAPE, dto.bodyShape)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "runway_model"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_TOP_SIZE = "top_size"
        const val KEY_BOTTOM_SIZE = "bottom_size"
        const val KEY_SHOE = "shoe_size_eu"
        const val KEY_TYPE = "body_type"
        const val KEY_SHAPE = "body_shape"
    }
}

// Unknown values fall back to null rather than throwing.
fun ModelProfileDto.toDomain(): ModelProfile = ModelProfile(
    heightCm = heightCm.coerceIn(ModelProfile.MIN_HEIGHT_CM, ModelProfile.MAX_HEIGHT_CM),
    weightKg = weightKg?.coerceIn(ModelProfile.MIN_WEIGHT_KG, ModelProfile.MAX_WEIGHT_KG),
    topSize = ClothingSize.from(topSize),
    bottomSize = ClothingSize.from(bottomSize),
    shoeSizeEu = shoeSizeEu?.coerceIn(ModelProfile.MIN_SHOE_EU, ModelProfile.MAX_SHOE_EU),
    bodyType = BodyType.from(bodyType),
    bodyShape = BodyShape.from(bodyShape),
    updatedAt = updatedAt,
)

fun ModelProfile.toDto(): ModelProfileDto = ModelProfileDto(
    heightCm = heightCm,
    weightKg = weightKg,
    topSize = topSize?.name,
    bottomSize = bottomSize?.name,
    shoeSizeEu = shoeSizeEu,
    bodyType = bodyType?.name,
    bodyShape = bodyShape?.name,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020b. Processes and threads overview. [online] Available at: <https://developer.android.com/guide/components/processes-and-threads> [Accessed 31 July 2023].
Square, Inc., n.d.. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 31 July 2023].
*/

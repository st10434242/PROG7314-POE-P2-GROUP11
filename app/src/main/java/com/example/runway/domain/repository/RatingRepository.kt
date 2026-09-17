package com.example.runway.domain.repository

import com.example.runway.domain.model.OutfitRating
import com.example.runway.domain.model.RatingSummary

// Repository contract for confidence ratings.
// Ratings live on the API so they follow the user rather than the device.

interface RatingRepository {

    suspend fun listForOutfit(outfitId: String): Result<RatingSummary>

    suspend fun rate(outfitId: String, score: Int, note: String?): Result<OutfitRating>
}

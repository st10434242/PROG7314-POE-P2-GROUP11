package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query

// Firestore data access for confidence ratings.

class RatingService(firestore: Firestore? = null) {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val ratings get() = db.collection(Collections.CONFIDENCE_RATINGS)
    private val outfits get() = db.collection(Collections.OUTFITS)

    // Ratings are only ever read for one outfit, newest first.
    suspend fun listForOutfit(uid: String, outfitId: String): RatingSummaryResponse {
        requireOwnedOutfit(uid, outfitId)

        val documents = ratings
            .whereEqualTo(Fields.OWNER_UID, uid)
            .whereEqualTo(Fields.OUTFIT_ID, outfitId)
            .orderBy(Fields.RATED_AT, Query.Direction.DESCENDING)
            .limit(MAX_RATINGS)
            .get()
            .await()
            .documents
            .map { it.toObject(ConfidenceRatingDocument::class.java) }

        return RatingSummaryResponse(
            outfitId = outfitId,
            count = documents.size,
            average = averageScore(documents.map { it.score }),
            ratings = documents.map { it.toResponse() },
        )
    }

    suspend fun create(uid: String, outfitId: String, body: CreateRatingRequest): RatingResponse {
        body.validate()
        // Proving the outfit belongs to the caller is what stops a rating being
        // attached to somebody else's outfit.
        requireOwnedOutfit(uid, outfitId)

        val ref = ratings.document()
        val document = ConfidenceRatingDocument(
            id = ref.id,
            // From the verified token, never from the request body.
            ownerUid = uid,
            outfitId = outfitId,
            score = body.score.toLong(),
            note = body.note?.trim()?.takeIf { it.isNotEmpty() },
            ratedAt = Timestamp.now(),
        )

        ref.set(document).await()
        return document.toResponse()
    }

    // Same 404-for-everything rule the other services use.
    private suspend fun requireOwnedOutfit(uid: String, outfitId: String) {
        val snapshot = outfits.document(outfitId).get().await()

        if (!snapshot.exists()) throw NotFoundException("Outfit $outfitId was not found")

        val outfit = snapshot.toObject(OutfitDocument::class.java)
            ?: throw NotFoundException("Outfit $outfitId was not found")

        if (outfit.ownerUid != uid) throw NotFoundException("Outfit $outfitId was not found")
    }

    private companion object {
        const val MAX_RATINGS = 50
    }
}

package com.example.runway.fake

import com.example.runway.domain.model.Outfit
import com.example.runway.domain.repository.OutfitRepository

// In-memory outfits, enough for the confidence sheet to read a name.
class FakeOutfitRepository(
    private var outfit: Outfit = Outfit(id = "outfit-1", name = "Friday dinner"),
) : OutfitRepository {

    var wearCount = 0
        private set

    var getError: Throwable? = null
    var wearError: Throwable? = null

    override suspend fun list(): Result<List<Outfit>> = Result.success(listOf(outfit))

    override suspend fun get(id: String): Result<Outfit> =
        getError?.let { Result.failure(it) } ?: Result.success(outfit)

    override suspend fun create(outfit: Outfit): Result<Outfit> = Result.success(outfit)

    override suspend fun update(outfit: Outfit): Result<Outfit> {
        this.outfit = outfit
        return Result.success(outfit)
    }

    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun logWear(id: String): Result<Unit> {
        wearCount++
        return wearError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}

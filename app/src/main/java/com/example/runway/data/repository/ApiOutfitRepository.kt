package com.example.runway.data.repository

import com.example.runway.data.remote.api.OutfitDto
import com.example.runway.data.remote.api.OutfitItemDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.UpdateOutfitDto
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.repository.OutfitRepository

// Saved outfits, read and written through the Runway REST API (IIE, 2026; Square, Inc., n.d.).

class ApiOutfitRepository(
    private val api: RunwayApi,
) : OutfitRepository {

    override suspend fun list(): Result<List<Outfit>> = runCatching {
        api.getOutfits().data.map { it.toDomain() }
    }

    override suspend fun get(id: String): Result<Outfit> = runCatching {
        api.getOutfit(id).toDomain()
    }

    override suspend fun create(outfit: Outfit): Result<Outfit> = runCatching {
        api.createOutfit(outfit.toCreateDto()).toDomain()
    }

    override suspend fun update(outfit: Outfit): Result<Outfit> = runCatching {
        api.updateOutfit(
            outfit.id,
            UpdateOutfitDto(
                name = outfit.name,
                coverImagePath = outfit.renderPath,
                items = outfit.toItemDtos(),
            ),
        ).toDomain()
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        api.deleteOutfit(id)
    }
}

// The API stores a placement per garment; this app only needs the order, so the
// position fields are left at their defaults and zIndex carries the order.
private fun Outfit.toItemDtos(): List<OutfitItemDto> =
    itemIds.mapIndexed { index, itemId ->
        OutfitItemDto(clothingItemId = itemId, zIndex = index.toLong())
    }

private fun Outfit.toCreateDto(): OutfitDto = OutfitDto(
    name = name,
    coverImagePath = renderPath,
    items = toItemDtos(),
)

private fun OutfitDto.toDomain(): Outfit = Outfit(
    id = id,
    name = name,
    itemIds = items.sortedBy { it.zIndex }.map { it.clothingItemId },
    renderPath = coverImagePath,
    updatedAt = updatedAt,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Square, Inc., n.d.. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 31 July 2023].
*/

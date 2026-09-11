package com.example.runway.data.mapper

import com.example.runway.data.local.ItemEntity
import com.example.runway.data.remote.api.CreateItemDto
import com.example.runway.data.remote.api.ItemDto
import com.example.runway.data.remote.api.UpdateItemDto
import com.example.runway.domain.model.Item
import java.time.Instant
import kotlin.math.roundToLong

// Conversions between DTO, Room entity and domain model (IIE, 2026).

// Conversions between the three representations of a garment (SCRUM-82): ItemDto what the API sends and receives (JSON, ISO-8601 timestamps) ItemEntity what Room stores (String key, epoch millis) Item what the UI reasons about (plain Kotlin) Keeping the conversions in one file means the boundaries between the three are visible in a single place instead of scattered through the repository.

fun ItemEntity.toDomain(): Item = Item(
    id = id,
    name = name,
    category = category,
    colour = colour,
    brand = brand,
    size = size,
    purchasePrice = purchasePrice,
    wearCount = wearCount,
    costPerWear = costPerWear(purchasePrice, wearCount),
    archived = archived,
    updatedAt = updatedAt,
    pendingSync = pendingSync,
)

fun Item.toEntity(
    updatedAt: Long = System.currentTimeMillis(),
    pendingSync: Boolean = true,
    deleted: Boolean = false,
): ItemEntity = ItemEntity(
    id = id,
    name = name,
    category = category,
    colour = colour,
    brand = brand,
    size = size,
    purchasePrice = purchasePrice,
    wearCount = wearCount,
    archived = archived,
    deleted = deleted,
    updatedAt = updatedAt,
    pendingSync = pendingSync,
)

// A row that came from the server is by definition already synced, so pendingSync is false: writing it as true would push the server's own data straight back at it in an endless loop.
fun ItemDto.toEntity(): ItemEntity = ItemEntity(
    id = id,
    name = name,
    category = category,
    colour = colour,
    brand = brand,
    size = size,
    purchasePrice = purchasePrice,
    wearCount = wearCount,
    archived = archived,
    deleted = deleted,
    updatedAt = updatedAt.toEpochMillis(),
    pendingSync = false,
)

fun ItemEntity.toCreateDto(): CreateItemDto = CreateItemDto(
    name = name,
    category = category,
    colour = colour,
    brand = brand,
    size = size,
    purchasePrice = purchasePrice,
)

fun ItemEntity.toUpdateDto(): UpdateItemDto = UpdateItemDto(
    name = name,
    category = category,
    colour = colour,
    brand = brand,
    size = size,
    purchasePrice = purchasePrice,
    archived = archived,
)

// The API sends ISO-8601 strings; Room stores epoch milliseconds.
private fun String?.toEpochMillis(): Long =
    if (this.isNullOrBlank()) 0L
    else runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(0L)

// Rounded to cents.
private fun costPerWear(price: Double?, wearCount: Long): Double? =
    if (price == null || wearCount <= 0) null
    else ((price / wearCount) * 100).roundToLong() / 100.0

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

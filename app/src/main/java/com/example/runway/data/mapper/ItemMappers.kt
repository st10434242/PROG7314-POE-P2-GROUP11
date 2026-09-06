package com.example.runway.data.mapper

import com.example.runway.data.local.ItemEntity
import com.example.runway.domain.model.Item

fun ItemEntity.toDomain(): Item = Item(
    id = id,
    title = title,
    note = note
)

fun Item.toEntity(updatedAt: Long): ItemEntity = ItemEntity(
    id = id,
    title = title,
    note = note,
    updatedAt = updatedAt
)

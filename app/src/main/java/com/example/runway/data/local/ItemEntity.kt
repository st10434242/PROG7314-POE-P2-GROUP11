package com.example.runway.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How a row is stored. This type never leaves the data layer.
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String,
    val updatedAt: Long
)

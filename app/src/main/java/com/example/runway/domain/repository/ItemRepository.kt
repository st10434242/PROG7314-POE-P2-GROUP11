package com.example.runway.domain.repository

import com.example.runway.domain.model.Item
import kotlinx.coroutines.flow.Flow

// Repository contract the ViewModels depend on.

interface ItemRepository {
    fun observeItems(): Flow<List<Item>>

    fun observeItem(id: String): Flow<Item?>

    suspend fun save(item: Item)

    suspend fun delete(id: String)

    // Pulls changes from the API and pushes anything queued locally.
    suspend fun refresh(): Result<Unit>
}

package com.example.runway.domain.repository

import com.example.runway.domain.model.Item
import kotlinx.coroutines.flow.Flow

/**
 * The contract the ViewModel depends on. Because this is an interface, a unit
 * test can hand the ViewModel a fake and never touch a real database.
 */
interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    fun observeItem(id: Long): Flow<Item?>
    suspend fun save(item: Item)
    suspend fun delete(id: Long)
}

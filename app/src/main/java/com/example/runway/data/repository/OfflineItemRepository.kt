package com.example.runway.data.repository

import com.example.runway.data.local.ItemDao
import com.example.runway.data.mapper.toDomain
import com.example.runway.data.mapper.toEntity
import com.example.runway.domain.model.Item
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single place that decides where data comes from. Today that is Room.
 * When a remote API arrives it is added here, and nothing above this class
 * has to find out.
 */
class OfflineItemRepository(
    private val dao: ItemDao
) : ItemRepository {

    override fun observeItems(): Flow<List<Item>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeItem(id: Long): Flow<Item?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun save(item: Item) =
        dao.upsert(item.toEntity(updatedAt = System.currentTimeMillis()))

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

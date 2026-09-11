package com.example.runway.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Room data access object for the wardrobe (IIE, 2026; sqlite.org, n.d.b).

@Dao
interface ItemDao {
    // Soft-deleted rows exist for sync purposes only and are never shown.
    @Query("SELECT * FROM items WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE deleted = 0 AND category = :category ORDER BY updatedAt DESC")
    fun observeByCategory(category: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<ItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    // Used by a sync, which writes a whole page of server rows at once.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    // The outbox: local changes the API has not accepted yet.
    @Query("SELECT * FROM items WHERE pendingSync = 1")
    suspend fun pendingSync(): List<ItemEntity>

    // Flags a row deleted and queues it for the server.
    @Query("UPDATE items SET deleted = 1, pendingSync = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long)

    // Removes a row outright.
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun hardDelete(id: String)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
sqlite.org, n.d.b. Appropriate Uses For SQLite. [online] Available at: <https://www.sqlite.org/whentouse.html> [Accessed 31 July 2023].
*/

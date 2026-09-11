package com.example.runway.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity for a stored garment (IIE, 2026; sqlite.org, n.d.b).

// How a garment is stored on the device.
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val colour: String?,
    val brand: String?,
    val size: String?,
    val purchasePrice: Double?,
    val wearCount: Long,
    val archived: Boolean,
    val deleted: Boolean,
    // Epoch milliseconds.
    val updatedAt: Long,
    val pendingSync: Boolean,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
sqlite.org, n.d.b. Appropriate Uses For SQLite. [online] Available at: <https://www.sqlite.org/whentouse.html> [Accessed 31 July 2023].
*/

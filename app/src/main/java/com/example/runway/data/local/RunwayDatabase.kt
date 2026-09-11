package com.example.runway.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Room database definition for the Runway app (IIE, 2026; sqlite.org, n.d.b).

@Database(
    entities = [ItemEntity::class],
    version = 2,
    exportSchema = true
)
abstract class RunwayDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "runway.db"
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
sqlite.org, n.d.b. Appropriate Uses For SQLite. [online] Available at: <https://www.sqlite.org/whentouse.html> [Accessed 31 July 2023].
*/

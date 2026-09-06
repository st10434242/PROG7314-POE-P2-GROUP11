package com.example.runway.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bump [version] and add a migration whenever an entity changes, or the app
 * will crash on launch for anyone who already has data.
 */
@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RunwayDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "runway.db"
    }
}

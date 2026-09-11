package com.example.runway.di

import android.content.Context
import androidx.room.Room
import com.example.runway.data.auth.GoogleAuthClient
import com.example.runway.data.auth.AuthSessionStore
import com.example.runway.data.local.PreferencesSettingsStore
import com.example.runway.data.local.RunwayDatabase
import com.example.runway.data.repository.OfflineItemRepository
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.SettingsRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * Hand-rolled dependency container. Small enough to read in one sitting, and
 * it keeps construction out of the ViewModels. When the graph outgrows this,
 * swapping in Hilt is mechanical: every `by lazy` below becomes an @Provides.
 */
class AppContainer(context: Context) {

    private val database: RunwayDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            RunwayDatabase::class.java,
            RunwayDatabase.NAME
        ).build()
    }

    val itemRepository: ItemRepository by lazy {
        OfflineItemRepository(database.itemDao())
    }

    val googleAuthClient: GoogleAuthClient by lazy {
        GoogleAuthClient(context, FirebaseAuth.getInstance())
    }

    val authSessionStore: AuthSessionStore by lazy {
        AuthSessionStore(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        PreferencesSettingsStore(context)
    }
}

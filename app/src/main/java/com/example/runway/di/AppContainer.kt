package com.example.runway.di

import android.content.Context
import androidx.room.Room
import com.example.runway.data.auth.AuthSessionStore
import com.example.runway.data.auth.GoogleAuthClient
import com.example.runway.data.local.PreferencesSettingsStore
import com.example.runway.data.local.RunwayDatabase
import com.example.runway.data.local.SyncPreferences
import com.example.runway.data.remote.api.ApiClient
import com.example.runway.data.repository.OfflineItemRepository
import com.example.runway.data.repository.SyncingSettingsRepository
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.SettingsRepository
import com.google.firebase.auth.FirebaseAuth

// Manual dependency container for the Runway app (IIE, 2026).

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: RunwayDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            RunwayDatabase::class.java,
            RunwayDatabase.NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val syncPreferences: SyncPreferences by lazy {
        SyncPreferences(appContext)
    }

    val itemRepository: ItemRepository by lazy {
        OfflineItemRepository(
            dao = database.itemDao(),
            api = ApiClient.api,
            syncPreferences = syncPreferences,
            currentUid = { FirebaseAuth.getInstance().currentUser?.uid },
        )
    }

    val googleAuthClient: GoogleAuthClient by lazy {
        GoogleAuthClient(appContext, FirebaseAuth.getInstance())
    }

    val authSessionStore: AuthSessionStore by lazy {
        AuthSessionStore(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SyncingSettingsRepository(
            local = PreferencesSettingsStore(appContext),
            api = ApiClient.api,
        )
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

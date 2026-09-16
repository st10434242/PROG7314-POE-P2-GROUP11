package com.example.runway.di

import android.content.Context
import androidx.room.Room
import com.example.runway.data.auth.AuthSessionStore
import com.example.runway.data.auth.GoogleAuthClient
import com.example.runway.data.local.ItemImageStore
import com.example.runway.data.local.ModelPhotoStore
import com.example.runway.data.local.OutfitRenderStore
import com.example.runway.data.local.PreferencesSettingsStore
import com.example.runway.data.local.RunwayDatabase
import com.example.runway.data.local.SyncPreferences
import com.example.runway.data.remote.api.ApiClient
import com.example.runway.data.repository.ApiModelRepository
import com.example.runway.data.repository.ApiOutfitRepository
import com.example.runway.data.repository.OfflineItemRepository
import com.example.runway.data.repository.TryOnRepository
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.ModelRepository
import com.example.runway.domain.repository.OutfitRepository
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

    val modelRepository: ModelRepository by lazy {
        ApiModelRepository(appContext, ApiClient.api)
    }

    // Garment photos, kept on this device alongside the Room rows.
    val itemImageStore: ItemImageStore by lazy {
        ItemImageStore(appContext)
    }

    // The body photo and its landmarks, kept on this device only.
    val modelPhotoStore: ModelPhotoStore by lazy {
        ModelPhotoStore(appContext)
    }

    // Sends photos to the API for rendering. Uses the long-timeout client.
    val tryOnRepository: TryOnRepository by lazy {
        TryOnRepository(appContext, ApiClient.renderApi)
    }

    // Saved outfits. These come straight from the API rather than from Room, so
    // unlike the wardrobe they need a connection.
    val outfitRepository: OutfitRepository by lazy {
        ApiOutfitRepository(ApiClient.api)
    }

    // The try-on renders that belong to those outfits, kept on this device.
    val outfitRenderStore: OutfitRenderStore by lazy {
        OutfitRenderStore(appContext)
    }

    val googleAuthClient: GoogleAuthClient by lazy {
        // Credential Manager needs the FirebaseAuth instance to exchange the
        // Google credential for a Firebase user.
        GoogleAuthClient(appContext, FirebaseAuth.getInstance())
    }

    val authSessionStore: AuthSessionStore by lazy {
        AuthSessionStore(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        PreferencesSettingsStore(appContext)
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

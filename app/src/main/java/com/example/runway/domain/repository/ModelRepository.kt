package com.example.runway.domain.repository

import com.example.runway.domain.model.ModelProfile

// Repository contract for the user's virtual model (IIE, 2026).

interface ModelRepository {

    // Returns the saved model, or sensible defaults for a user who has never chosen one.
    suspend fun getProfile(): Result<ModelProfile>

    // Saves the whole profile. SCRUM-86.
    suspend fun saveProfile(profile: ModelProfile): Result<ModelProfile>

    // The last profile read on this device, available immediately at start-up.
    fun cachedProfile(): ModelProfile
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

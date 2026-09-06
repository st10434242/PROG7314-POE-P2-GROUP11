package com.example.runway

import android.app.Application
import com.example.runway.di.AppContainer

/**
 * Registered in AndroidManifest.xml as android:name=".RunwayApplication".
 * Forgetting that registration is what produces a ClassCastException the
 * first time a ViewModel factory runs.
 */
class RunwayApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

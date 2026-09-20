package com.example.runway.fake

import android.graphics.Bitmap
import com.example.runway.data.local.RenderStore

// No pictures in unit tests, so loading always comes back empty.
class FakeRenderStore : RenderStore {

    val deleted = mutableListOf<String?>()

    override suspend fun save(bitmap: Bitmap): String = "render.png"

    override suspend fun load(path: String?): Bitmap? = null

    override fun delete(path: String?) {
        deleted += path
    }
}

package com.example.runway.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// Try-on renders saved alongside their outfit (IIE, 2026).
// Kept on the device rather than uploaded, for the same reason as the body photo:
// the picture is of the user, and nothing needs it on a server.

// What the outfit screens need from the render store, so tests can swap in a fake.
interface RenderStore {
    suspend fun save(bitmap: Bitmap): String
    suspend fun load(path: String?): Bitmap?
    fun delete(path: String?)
}

class OutfitRenderStore(context: Context) : RenderStore {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    override suspend fun save(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val file = File(directory, "render-${UUID.randomUUID()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    }

    override suspend fun load(path: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    override fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private companion object {
        const val DIRECTORY = "outfits"
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

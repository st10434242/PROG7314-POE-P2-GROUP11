package com.example

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

// The paid engine, kept behind the same interface as the free one (IIE, 2026).
// Replicate takes images as data URIs and hands back a URL to download.

class ReplicateTryOn(
    private val client: ReplicateClient = ReplicateClient(),
) : TryOnEngine {

    override val name: String get() = "Replicate"

    override val isConfigured: Boolean get() = client.isConfigured

    override suspend fun dress(
        person: ByteArray,
        garment: ByteArray,
        description: String,
        category: String,
    ): ByteArray {
        val url = client.run(
            model = MODEL,
            input = buildJsonObject {
                put("human_img", JsonPrimitive(person.asDataUri()))
                put("garm_img", JsonPrimitive(garment.asDataUri()))
                put("garment_des", JsonPrimitive(description))
                put("category", JsonPrimitive(category))
                // Crops to the ratio the model was trained on, which matters for a
                // phone photo that is much taller than it is wide.
                put("crop", JsonPrimitive(true))
                put("steps", JsonPrimitive(STEPS))
            },
        )
        return client.download(url)
    }

    override fun close() = client.close()

    private fun ByteArray.asDataUri(): String =
        "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(this)

    private companion object {
        // Non-commercial licence, which suits a university project and would not
        // suit a released product.
        const val MODEL = "cuuupid/idm-vton"
        const val STEPS = 30
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/

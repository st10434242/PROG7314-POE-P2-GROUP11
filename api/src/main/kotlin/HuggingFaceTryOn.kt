package com.example

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

// Renders a try-on on a public Hugging Face Space (IIE, 2026; Hugging Face, 2026).
// The Space runs the same IDM-VTON model as the paid option and costs nothing, at
// the price of waiting in a shared queue.
//
// The Space speaks Gradio 4's HTTP protocol, which is three steps:
//   1. POST /upload            - hand over the files, get server-side paths back
//   2. POST /call/tryon        - start the job, get an event id back
//   3. GET  /call/tryon/{id}   - an event stream that ends in the result

class HuggingFaceTryOn(
    private val space: String = System.getenv(SPACE_ENV)?.takeIf { it.isNotBlank() } ?: DEFAULT_SPACE,
    // Anonymous calls work; a token mainly helps when the Space is busy.
    private val token: String? = System.getenv(TOKEN_ENV),
    // Each Space names its own function; a mirror may not call it "tryon".
    private val endpoint: String =
        System.getenv(ENDPOINT_ENV)?.takeIf { it.isNotBlank() } ?: DEFAULT_ENDPOINT,
) : TryOnEngine {

    override val name: String get() = "Hugging Face Space $space"

    // A public Space needs no credentials, so there is nothing to be missing.
    override val isConfigured: Boolean get() = true

    private val base = "https://$space"
    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = REQUEST_TIMEOUT_MS
            }
        }
    }

    override suspend fun dress(
        person: ByteArray,
        garment: ByteArray,
        description: String,
        category: String,
    ): ByteArray {
        val personPath = upload(person, "person.jpg")
        val garmentPath = upload(garment, "garment.jpg")

        val eventId = start(personPath, garmentPath, description)
        val imageUrl = awaitResult(eventId)

        return client.get(imageUrl) { authorise() }.readRawBytes()
    }

    // Step one. The Space stores the file and answers with the path it stored it at.
    private suspend fun upload(bytes: ByteArray, fileName: String): String {
        val response = client.post("$base/upload?upload_id=${UUID.randomUUID()}") {
            authorise()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("files", bytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                )
            )
        }

        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw ImageServiceException("The image service would not accept the photo: ${body.take(200)}")
        }

        return runCatching { json.parseToJsonElement(body).jsonArray.first().jsonPrimitive.content }
            .getOrNull()
            ?: throw ImageServiceException("The image service returned no upload path")
    }

    // Step two. The seven arguments are the Space's own inputs, in its own order.
    private suspend fun start(personPath: String, garmentPath: String, description: String): String {
        val payload = buildJsonObject {
            put("data", buildJsonArray {
                // The human input is an image editor, so it arrives as a small
                // object rather than a bare file: no hand-drawn mask, just the photo.
                add(buildJsonObject {
                    putJsonObject("background") { putFileData(personPath) }
                    put("layers", JsonArray(emptyList()))
                    put("composite", JsonNull)
                })
                add(buildJsonObject { putFileData(garmentPath) })
                add(kotlinx.serialization.json.JsonPrimitive(description))
                add(kotlinx.serialization.json.JsonPrimitive(true))   // mask automatically
                add(kotlinx.serialization.json.JsonPrimitive(true))   // crop to the model's ratio
                add(kotlinx.serialization.json.JsonPrimitive(STEPS))
                add(kotlinx.serialization.json.JsonPrimitive(SEED))
            })
        }

        val response = client.post("$base/call/$endpoint") {
            authorise()
            contentType(ContentType.Application.Json)
            setBody(payload.toString())
        }

        val body = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw ImageServiceException("The image service refused the job: ${body.take(200)}")
        }

        return runCatching { json.parseToJsonElement(body).jsonObject["event_id"]?.jsonPrimitive?.content }
            .getOrNull()
            ?: throw ImageServiceException("The image service did not start the job")
    }

    // Step three. The stream stays open until the render finishes, so reading it to
    // the end is the wait; no polling loop is needed.
    private suspend fun awaitResult(eventId: String): String {
        val stream = client.get("$base/call/$endpoint/$eventId") { authorise() }.bodyAsText()

        if (stream.contains("event: error")) {
            // The Space sends its errors with no reason attached, so the whole
            // stream goes to the server console: that is the only place the cause
            // can be seen at all.
            println("Hugging Face Space refused the render. Stream was: ${stream.take(600)}")
            throw ImageServiceException(
                if (token.isNullOrBlank()) {
                    "The free image service refused the render and gives no reason. It is most often its " +
                        "shared GPU quota, which runs out quickly without a Hugging Face token. Set " +
                        "$TOKEN_ENV on the server, or try again later."
                } else {
                    "The image service refused the render and gives no reason. The Space may be busy or " +
                        "down; check that $SPACE_ENV points at a Space that is awake."
                }
            )
        }

        val payload = stream.substringAfterLast("event: complete", "")
            .substringAfter("data:", "")
            .trim()
            .takeIf { it.isNotBlank() }
            ?: throw ImageServiceException("The render did not finish in time. The free service was busy; try again.")

        // The Space returns the dressed image first and the mask it used second.
        return runCatching {
            json.parseToJsonElement(payload).jsonArray.first()
                .jsonObject["url"]?.jsonPrimitive?.content
        }.getOrNull()
            ?: throw ImageServiceException("The image service returned no image")
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putFileData(path: String) {
        put("path", path)
        putJsonObject("meta") { put("_type", "gradio.FileData") }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorise() {
        token?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
    }

    override fun close() = client.close()

    private companion object {
        const val DEFAULT_SPACE = "yisol-idm-vton.hf.space"
        const val SPACE_ENV = "HF_TRYON_SPACE"
        const val TOKEN_ENV = "HUGGINGFACE_API_TOKEN"
        const val ENDPOINT_ENV = "HF_TRYON_ENDPOINT"
        const val DEFAULT_ENDPOINT = "tryon"

        // Fewer steps than the paid model uses: this queue is shared, so finishing
        // matters more than the last few percent of quality.
        const val STEPS = 20
        const val SEED = 42

        // A busy Space can queue for minutes before it starts.
        const val REQUEST_TIMEOUT_MS = 300_000L
        const val CONNECT_TIMEOUT_MS = 20_000L
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Hugging Face, 2026. Querying Gradio apps with the API. [online] Available at: <https://www.gradio.app/guides/querying-gradio-apps-with-curl> [Accessed 15 September 2026].
*/

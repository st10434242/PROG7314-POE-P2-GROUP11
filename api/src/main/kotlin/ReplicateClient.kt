package com.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

// Calls Replicate's prediction API (IIE, 2026; Replicate, 2026).
// The token is read from the environment and never appears in the repository or
// in anything the Android app can see: the phone talks to this API, this API
// talks to Replicate.

@Serializable
private data class PredictionRequest(
    // Community models are run by version rather than by name.
    val version: String,
    val input: JsonObject,
)

@Serializable
private data class ModelResponse(val latest_version: ModelVersion? = null)

@Serializable
private data class ModelVersion(val id: String = "")

@Serializable
private data class PredictionResponse(
    val id: String = "",
    val status: String = "",
    val output: JsonElement? = null,
    val error: String? = null,
    val urls: PredictionUrls? = null,
)

@Serializable
private data class PredictionUrls(val get: String? = null)

class ReplicateClient(
    private val token: String? = System.getenv(TOKEN_ENV),
) {

    val isConfigured: Boolean get() = !token.isNullOrBlank()

    private val json = Json { ignoreUnknownKeys = true }

    // Model name to version hash, so the lookup happens once per server run.
    private val cachedVersions = mutableMapOf<String, String>()

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                // A render takes around twenty seconds; the ceiling is for a queue.
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = REQUEST_TIMEOUT_MS
            }
        }
    }

    // Runs one model and returns the URL of the image it produced.
    suspend fun run(model: String, input: JsonObject): String {
        val key = token?.takeIf { it.isNotBlank() }
            ?: throw ImageServiceException("The image service is not configured on this server")

        val response: HttpResponse = client.post("$BASE_URL/predictions") {
            header("Authorization", "Bearer $key")
            // Asks Replicate to hold the connection open until the render finishes,
            // which usually avoids polling altogether.
            header("Prefer", "wait")
            contentType(ContentType.Application.Json)
            setBody(PredictionRequest(version = latestVersionOf(model, key), input = input))
        }

        if (!response.status.isSuccessOrAccepted()) {
            throw ImageServiceException(describeFailure(response.status, response.bodyAsText()))
        }

        var prediction = response.body<PredictionResponse>()
        var waited = 0L
        while (prediction.status in IN_PROGRESS && waited < REQUEST_TIMEOUT_MS) {
            delay(POLL_INTERVAL_MS)
            waited += POLL_INTERVAL_MS
            val pollUrl = prediction.urls?.get ?: break
            prediction = client.get(pollUrl) { header("Authorization", "Bearer $key") }.body()
        }

        if (prediction.status != "succeeded") {
            throw ImageServiceException(prediction.error ?: "The render did not finish in time")
        }

        return prediction.output.firstUrl()
            ?: throw ImageServiceException("The image service returned no image")
    }

    // Community models are created by version hash, not by name: the by-name
    // endpoint only serves Replicate's own official models and answers 404 for
    // everything else. The version rarely changes, so it is looked up once.
    private suspend fun latestVersionOf(model: String, key: String): String {
        cachedVersions[model]?.let { return it }

        val response = client.get("$BASE_URL/models/$model") {
            header("Authorization", "Bearer $key")
        }
        if (!response.status.isSuccessOrAccepted()) {
            throw ImageServiceException(describeFailure(response.status, response.bodyAsText()))
        }

        val version = response.body<ModelResponse>().latest_version?.id
            ?.takeIf { it.isNotBlank() }
            ?: throw ImageServiceException("The image model '$model' has no published version")

        cachedVersions[model] = version
        return version
    }

    // Fetches the finished image so the app receives bytes rather than a link that
    // expires while the user is looking at it.
    suspend fun download(url: String): ByteArray = client.get(url).readRawBytes()

    fun close() = client.close()

    // The output is either a single URL or an array of them, depending on the model.
    private fun JsonElement?.firstUrl(): String? = when (this) {
        null -> null
        is JsonPrimitive -> contentOrNullSafe()
        else -> runCatching { jsonArray.firstOrNull()?.jsonPrimitive?.contentOrNullSafe() }.getOrNull()
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? = content.takeIf { it.startsWith("http") }

    private fun HttpStatusCode.isSuccessOrAccepted(): Boolean = value in 200..299

    // Replicate's own wording is passed through where it is useful, because
    // "payment required" and "model not found" need different actions.
    private fun describeFailure(status: HttpStatusCode, body: String): String = when (status.value) {
        401, 403 -> "The image service rejected this server's credentials"
        402 -> "The image service account has no credit left"
        404 -> "The image model could not be found"
        429 -> "The image service is rate limiting this account; try again shortly"
        else -> "The image service failed (${status.value}): ${body.take(200)}"
    }

    companion object {
        const val TOKEN_ENV = "REPLICATE_API_TOKEN"
        private const val BASE_URL = "https://api.replicate.com/v1"
        private val IN_PROGRESS = setOf("starting", "processing")
        private const val POLL_INTERVAL_MS = 2_000L
        private const val REQUEST_TIMEOUT_MS = 180_000L
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Replicate, 2026. HTTP API reference. [online] Available at: <https://replicate.com/docs/reference/http> [Accessed 15 September 2026].
*/

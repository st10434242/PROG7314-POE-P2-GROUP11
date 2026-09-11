package com.example

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

// Unit tests for the Runway REST API (IIE, 2026).
// Built on the Ktor server framework (Ktor, 2026).

class ServerTest {
    @Test
    fun `create rejects a blank name`() {
        val body = CreateItemRequest(name = "   ", category = "TOP")
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `create rejects a negative price`() {
        val body = CreateItemRequest(name = "Coat", category = "OUTERWEAR", purchasePrice = -1.0)
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `create accepts a valid item`() {
        CreateItemRequest(name = "Black wool coat", category = "OUTERWEAR", purchasePrice = 1299.0)
            .validate()
    }

    @Test
    fun `update rejects an empty patch`() {
        // A PATCH with no fields is a request that cannot mean anything.
        assertFailsWith<ValidationException> { UpdateItemRequest().validate() }
    }

    @Test
    fun `settings reject an unknown theme`() {
        assertFailsWith<ValidationException> { SettingsDto(theme = "NEON").validate() }
    }

    @Test
    fun `cost per wear divides price by wears and rounds to cents`() {
        assertEquals(92.79, costPerWear(1299.0, 14))
    }

    @Test
    fun `cost per wear is undefined before the first wear`() {
        assertNull(costPerWear(1299.0, 0))
    }

    @Test
    fun `cost per wear is undefined when no price was recorded`() {
        assertNull(costPerWear(null, 5))
    }

    @Test
    fun `health check needs no token`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
*/

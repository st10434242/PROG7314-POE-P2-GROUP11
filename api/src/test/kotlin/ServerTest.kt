package com.example

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `an item may not have a wear limit of zero`() {
        val body = CreateItemRequest(name = "Jeans", category = "BOTTOM", wearLimit = 0)
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `an item may not have a wear limit above the maximum`() {
        val body = CreateItemRequest(name = "Jeans", category = "BOTTOM", wearLimit = 61)
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `leaving the wear limit out is allowed`() {
        // Means "use the limit from the user's settings".
        CreateItemRequest(name = "Jeans", category = "BOTTOM").validate()
    }

    @Test
    fun `settings reject a wear limit outside the allowed range`() {
        assertFailsWith<ValidationException> { SettingsDto(defaultWearLimit = 0).validate() }
        assertFailsWith<ValidationException> { SettingsDto(defaultWearLimit = 99).validate() }
    }

    @Test
    fun `an item needs a wash once it reaches its limit`() {
        assertFalse(needsWash(wearCount = 2, wearLimit = 3))
        assertTrue(needsWash(wearCount = 3, wearLimit = 3))
        assertTrue(needsWash(wearCount = 4, wearLimit = 3))
    }

    @Test
    fun `an item with no limit never needs a wash`() {
        assertFalse(needsWash(wearCount = 40, wearLimit = 0))
    }

    @Test
    fun `an outfit patch rejects a blank name`() {
        assertFailsWith<ValidationException> { UpdateOutfitRequest(name = "  ").validate() }
    }

    @Test
    fun `an outfit patch rejects a placement with no item`() {
        val body = UpdateOutfitRequest(items = listOf(OutfitItemDto(clothingItemId = "")))
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `an outfit patch with no fields is rejected`() {
        assertFailsWith<ValidationException> { UpdateOutfitRequest().validate() }
    }

    @Test
    fun `an outfit patch accepts a rename`() {
        UpdateOutfitRequest(name = "Friday dinner").validate()
    }

    @Test
    fun `an outfit patch accepts replacing the placements`() {
        UpdateOutfitRequest(items = listOf(OutfitItemDto(clothingItemId = "item-1"))).validate()
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

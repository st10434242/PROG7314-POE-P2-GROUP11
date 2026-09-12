package com.example

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

// Wardrobe-wide endpoints, as opposed to the single-item ones in ItemRoutes (IIE, 2026; Rouse, 2020).
// Built on the Ktor server framework (Ktor, 2026).
fun Route.wardrobeRoutes(service: ItemService = ItemService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/wardrobe") {
            // Counts and totals for the home and profile screens.
            get("/summary") {
                call.respond(service.summary(call.uid()))
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
*/

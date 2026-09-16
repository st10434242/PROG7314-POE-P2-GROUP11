package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

// Virtual try-on endpoint (IIE, 2026; Rouse, 2020).
// Built on the Ktor server framework (Ktor, 2026).

fun Route.tryOnRoutes(service: TryOnService = TryOnService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/tryon") {
            post {
                // A missing token is a server configuration problem, not the
                // caller's fault, so it is a 503 rather than a 400 or a 500.
                if (!service.isConfigured) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiError("image_service_unconfigured", "The image service is not set up on this server"),
                    )
                    return@post
                }

                val body = call.receive<TryOnRequest>()
                call.respond(service.render(body))
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 15 September 2026].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
*/

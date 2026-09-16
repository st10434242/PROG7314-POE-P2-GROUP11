package com.example

import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

// Virtual model endpoints for the Runway REST API (IIE, 2026; Rouse, 2020).
// Built on the Ktor server framework (Ktor, 2026).
// The model belongs to the token holder, so no id appears in the path (OWASP, 2026).

fun Route.modelRoutes(service: ModelService = ModelService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/users/me/model") {
            get {
                call.respond(service.get(call.uid()))
            }

            put {
                val body = call.receive<ModelProfileDto>()
                call.respond(service.save(call.uid(), body))
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 14 September 2026].
OWASP, 2026. Broken access control. [online] Available at: <https://owasp.org/Top10/A01_2021-Broken_Access_Control/> [Accessed 14 September 2026].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
*/

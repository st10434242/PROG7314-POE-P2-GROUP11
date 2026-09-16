package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore

// Firestore data access for the user's virtual model (IIE, 2026).
// A fixed document id means reading the model is one fetch, not a query (Google, 2026).

class ModelService(firestore: Firestore? = null) {

    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val users get() = db.collection(Collections.USERS)

    // Defaults rather than 404: a user who has never chosen a model still has one.
    suspend fun get(uid: String): ModelProfileDto {
        val snapshot = ref(uid).get().await()
        return if (snapshot.exists()) {
            snapshot.toObject(ModelProfileDocument::class.java)!!.toDto()
        } else {
            ModelProfileDto()
        }
    }

    // PUT semantics: the selection screen always sends the whole profile.
    suspend fun save(uid: String, body: ModelProfileDto): ModelProfileDto {
        body.validate()
        val document = body.toDocument().apply { updatedAt = Timestamp.now() }
        ref(uid).set(document).await()
        return document.toDto()
    }

    private fun ref(uid: String) =
        users.document(uid)
            .collection(Collections.MODEL)
            .document(Collections.MODEL_DOC)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 14 September 2026].
*/

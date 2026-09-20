package com.example

import com.google.cloud.firestore.Firestore

// Used by anything that hangs off an outfit (ratings, plans) before it reads or writes.
// Missing and someone-else's both come back as 404, so the answer doesn't reveal the outfit exists.
suspend fun requireOwnedOutfit(db: Firestore, uid: String, outfitId: String) {
    val snapshot = db.collection(Collections.OUTFITS).document(outfitId).get().await()
    if (!snapshot.exists()) throw NotFoundException("Outfit $outfitId was not found")

    val outfit = snapshot.toObject(OutfitDocument::class.java)
        ?: throw NotFoundException("Outfit $outfitId was not found")

    if (outfit.ownerUid != uid || outfit.deleted) throw NotFoundException("Outfit $outfitId was not found")
}

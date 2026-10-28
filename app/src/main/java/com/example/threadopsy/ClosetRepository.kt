package com.example.threadopsy

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ClosetRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    /**
     * Stores the user's account profile in Firestore when they sign up or log in.
     */
    fun saveUserProfile(userId: String, email: String) {
        if (userId.isBlank()) return
        val profileData = mapOf(
            "userId" to userId,
            "email" to email,
            "lastActive" to System.currentTimeMillis()
        )
        usersCollection.document(userId).set(profileData)
    }

    /**
     * Streams only the logged-in user's personal closet items.
     */
    fun getClosetStream(userId: String): Flow<List<ClothingItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userCloset = usersCollection.document(userId).collection("closet_items")
        val subscription = userCloset
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ClosetRepository", "Firestore listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ClothingItem::class.java)?.copy(id = doc.id)
                    }
                    trySend(items)
                }
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Saves a garment into the logged-in user's personal cloud closet.
     */
    fun saveGarment(
        userId: String,
        item: ClothingItem,
        onResult: (Boolean, String) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(false, "Sign in required to save items.")
            return
        }

        val userCloset = usersCollection.document(userId).collection("closet_items")
        val docId = item.id.ifBlank { userCloset.document().id }
        val itemWithId = item.copy(id = docId)

        userCloset.document(docId)
            .set(itemWithId)
            .addOnSuccessListener {
                onResult(true, "Saved '${item.garmentName}' to your Closet!")
            }
            .addOnFailureListener { e ->
                onResult(false, "Could not save: ${e.localizedMessage ?: "Error"}")
            }
    }

    /**
     * Increments wear count (+1) in the user's personal closet.
     */
    fun incrementWearCount(
        userId: String,
        item: ClothingItem,
        onError: (String) -> Unit = {}
    ) {
        if (userId.isBlank() || item.id.isBlank()) return
        val newWearCount = item.timesWorn + 1

        usersCollection.document(userId)
            .collection("closet_items")
            .document(item.id)
            .update("timesWorn", newWearCount)
            .addOnFailureListener { e ->
                onError("Could not update wear count: ${e.localizedMessage}")
            }
    }

    /**
     * Deletes a garment from the user's personal closet.
     */
    fun deleteGarment(
        userId: String,
        itemId: String,
        onError: (String) -> Unit = {}
    ) {
        if (userId.isBlank() || itemId.isBlank()) return
        usersCollection.document(userId)
            .collection("closet_items")
            .document(itemId)
            .delete()
            .addOnFailureListener { e ->
                onError("Could not delete item: ${e.localizedMessage}")
            }
    }
}
package cricket.player.auction.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import cricket.player.auction.data.local.DatabaseSchema
import cricket.player.auction.data.local.LocalDatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseCricketRepository(private val dbManager: LocalDatabaseManager) {

    private val databaseUrl = "https://cricket-auction-app-24750-default-rtdb.firebaseio.com"
    private val database: FirebaseDatabase by lazy {
        val instance = FirebaseDatabase.getInstance(databaseUrl)
        try {
            instance.setPersistenceEnabled(true)
        } catch (_: Exception) {
            // Persistence may already be enabled
        }
        instance
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeListener: ValueEventListener? = null
    private var activeRef: DatabaseReference? = null

    init {
        ensureAuthAndListen()
    }

    private fun ensureAuthAndListen() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener {
                attachRealtimeListener()
            }
        } else {
            attachRealtimeListener()
        }
    }

    fun getActiveUserUid(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "default_user"
    }

    var onSchemaRemoteUpdated: ((DatabaseSchema) -> Unit)? = null

    @Synchronized
    fun attachRealtimeListener() {
        if (activeListener != null) return

        val rootRef = database.reference.child("database_schema")
        activeRef = rootRef

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jsonString = snapshot.value as? String
                if (!jsonString.isNullOrBlank()) {
                    scope.launch {
                        val schema = dbManager.importFromJsonString(jsonString)
                        if (schema != null) {
                            if (onSchemaRemoteUpdated != null) {
                                onSchemaRemoteUpdated?.invoke(schema)
                            } else {
                                dbManager.saveDatabase(schema)
                            }
                        }
                    }
                } else {
                    // Firebase database node is empty/null! Push current local schema to Firebase immediately!
                    scope.launch {
                        val localSchema = dbManager.loadDatabase().sanitized()
                        syncDatabaseToFirebase(localSchema)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        }

        activeListener = listener
        rootRef.addValueEventListener(listener)
    }

    private fun detachListener() {
        activeListener?.let { listener ->
            activeRef?.removeEventListener(listener)
        }
        activeListener = null
        activeRef = null
    }

    fun syncDatabaseToFirebase(schema: DatabaseSchema, userUid: String = getActiveUserUid()) {
        scope.launch {
            try {
                val json = dbManager.exportToJsonString(schema)
                // Write to both root database_schema AND users/default_user/database_schema
                database.reference.child("database_schema").setValue(json)
                database.reference.child("users").child("default_user").child("database_schema").setValue(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUserDatabase(userUid: String = getActiveUserUid()) {
        scope.launch {
            try {
                database.reference.child("database_schema").removeValue()
                database.reference.child("users").child("default_user").removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

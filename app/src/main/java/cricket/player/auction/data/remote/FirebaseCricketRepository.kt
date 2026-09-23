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
        FirebaseDatabase.getInstance(databaseUrl)
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeListener: ValueEventListener? = null
    private var activeUserRef: DatabaseReference? = null
    private var activeUid: String = ""

    init {
        val currentUid = getActiveUserUid()
        attachRealtimeListener(currentUid)
    }

    fun getActiveUserUid(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) return uid
        return "default_user"
    }

    @Synchronized
    fun attachRealtimeListener(userUid: String) {
        val uid = if (userUid.isBlank()) getActiveUserUid() else userUid
        if (activeUid == uid && activeListener != null) return

        detachListener()
        activeUid = uid
        val userRef = database.reference.child("users").child(uid).child("database_schema")
        activeUserRef = userRef

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jsonString = snapshot.value as? String
                if (!jsonString.isNullOrBlank()) {
                    scope.launch {
                        val schema = dbManager.importFromJsonString(jsonString)
                        if (schema != null) {
                            dbManager.saveDatabase(schema)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        }

        activeListener = listener
        userRef.addValueEventListener(listener)
    }

    private fun detachListener() {
        activeListener?.let { listener ->
            activeUserRef?.removeEventListener(listener)
        }
        activeListener = null
        activeUserRef = null
    }

    fun syncDatabaseToFirebase(schema: DatabaseSchema, userUid: String = getActiveUserUid()) {
        scope.launch {
            try {
                val uid = if (userUid.isBlank()) getActiveUserUid() else userUid
                val json = dbManager.exportToJsonString(schema)
                database.reference.child("users").child(uid).child("database_schema").setValue(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUserDatabase(userUid: String = getActiveUserUid()) {
        scope.launch {
            try {
                val uid = if (userUid.isBlank()) getActiveUserUid() else userUid
                database.reference.child("users").child(uid).removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

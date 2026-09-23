package cricket.player.auction.data.remote

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
    private val dbRef: DatabaseReference by lazy {
        database.reference.child("database_schema")
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        attachRealtimeListener()
    }

    private fun attachRealtimeListener() {
        try {
            dbRef.addValueEventListener(object : ValueEventListener {
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
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncDatabaseToFirebase(schema: DatabaseSchema) {
        scope.launch {
            try {
                val json = dbManager.exportToJsonString(schema)
                dbRef.setValue(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

package cricket.player.auction.data.remote

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import cricket.player.auction.data.local.DatabaseSchema
import cricket.player.auction.data.local.LocalDatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirebaseCricketRepository(
    private val context: Context,
    private val dbManager: LocalDatabaseManager
) {

    private val databaseUrl = "https://cricket-auction-app-24750-default-rtdb.firebaseio.com"
    private val database: FirebaseDatabase by lazy {
        val instance = FirebaseDatabase.getInstance(databaseUrl)
        try {
            instance.setPersistenceEnabled(true)
        } catch (_: Exception) {
            // Persistence may already be initialized
        }
        instance
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeListener: ValueEventListener? = null
    private var activeRef: DatabaseReference? = null

    val deviceId: String by lazy {
        try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                "android_$androidId"
            } else {
                getPersistentFallbackDeviceId()
            }
        } catch (_: Exception) {
            getPersistentFallbackDeviceId()
        }
    }

    private fun getPersistentFallbackDeviceId(): String {
        val prefs = context.getSharedPreferences("cricket_device_prefs", Context.MODE_PRIVATE)
        var savedId = prefs.getString("device_unique_id", null)
        if (savedId.isNullOrBlank()) {
            savedId = "android_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString("device_unique_id", savedId).apply()
        }
        return savedId
    }

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

    fun registerDeviceInfo() {
        scope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val deviceInfo = mapOf(
                    "deviceId" to deviceId,
                    "platform" to "Android",
                    "manufacturer" to Build.MANUFACTURER,
                    "model" to Build.MODEL,
                    "androidVersion" to Build.VERSION.RELEASE,
                    "sdkInt" to Build.VERSION.SDK_INT,
                    "appVersion" to "2.1",
                    "lastSeenTimestamp" to System.currentTimeMillis(),
                    "lastSeenDate" to dateFormat.format(Date())
                )
                // Store device telemetry only under devices/<deviceId>
                database.reference.child("devices").child(deviceId).setValue(deviceInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var onSchemaRemoteUpdated: ((DatabaseSchema) -> Unit)? = null

    private fun isSchemaEmpty(schema: DatabaseSchema?): Boolean {
        if (schema == null) return true
        return schema.tournaments.isEmpty() && schema.teams.isEmpty() && schema.players.isEmpty()
    }

    @Synchronized
    fun attachRealtimeListener() {
        if (activeListener != null) return

        registerDeviceInfo()

        // Clean up legacy duplicate nodes to keep Realtime Database clean
        scope.launch {
            try {
                database.reference.child("active_devices").removeValue()
                database.reference.child("devices").child(deviceId).child("database_schema").removeValue()
                database.reference.child("devices").child(deviceId).child("device_info").removeValue()
                database.reference.child("users").removeValue()
            } catch (_: Exception) {}
        }

        val rootRef = database.reference.child("database_schema")
        activeRef = rootRef

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jsonString = snapshot.value as? String
                if (!jsonString.isNullOrBlank()) {
                    val schema = dbManager.importFromJsonString(jsonString)
                    if (schema != null && !isSchemaEmpty(schema)) {
                        scope.launch {
                            onSchemaRemoteUpdated?.invoke(schema) ?: dbManager.saveDatabase(schema)
                        }
                        return
                    }
                }

                // If cloud is empty, but local has data, sync local to cloud:
                scope.launch {
                    val localSchema = dbManager.loadDatabase().sanitized()
                    if (!isSchemaEmpty(localSchema)) {
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
                // Never push empty schema to cloud to prevent wiping out data on fresh install
                if (isSchemaEmpty(schema)) {
                    return@launch
                }
                val json = dbManager.exportToJsonString(schema)
                // Single canonical database_schema node
                database.reference.child("database_schema").setValue(json)
                // Update device last synced timestamp
                database.reference.child("devices").child(deviceId).child("lastSyncedTimestamp").setValue(System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUserDatabase(userUid: String = getActiveUserUid()) {
        scope.launch {
            try {
                database.reference.child("database_schema").removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

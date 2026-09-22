package cricket.player.auction.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import cricket.player.auction.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class DatabaseSchema(
    val tournaments: List<Tournament> = emptyList(),
    val teams: List<Team> = emptyList(),
    val players: List<Player> = emptyList(),
    val bids: List<Bid> = emptyList(),
    val captainInvites: List<CaptainInvite> = emptyList(),
    val matches: List<Match> = emptyList(),
    val ballRecords: List<BallRecord> = emptyList(),
    val activeTournamentId: String = "",
    val activePlayerId: String? = null,
    val activeMatchId: String? = null
)

class LocalDatabaseManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dbFile = File(context.filesDir, "cricket_database_v3.json")
    private val mutex = Mutex()

    suspend fun loadDatabase(): DatabaseSchema = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!dbFile.exists()) {
                val seedData = createFreshDatabase()
                saveDatabaseInternal(seedData)
                seedData
            } else {
                try {
                    val json = dbFile.readText()
                    gson.fromJson(json, DatabaseSchema::class.java) ?: createFreshDatabase()
                } catch (e: Exception) {
                    e.printStackTrace()
                    val seed = createFreshDatabase()
                    saveDatabaseInternal(seed)
                    seed
                }
            }
        }
    }

    suspend fun saveDatabase(schema: DatabaseSchema) = withContext(Dispatchers.IO) {
        mutex.withLock {
            saveDatabaseInternal(schema)
        }
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (dbFile.exists()) {
                dbFile.delete()
            }
            saveDatabaseInternal(createFreshDatabase())
        }
    }

    private fun saveDatabaseInternal(schema: DatabaseSchema) {
        try {
            val json = gson.toJson(schema)
            dbFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToJsonString(schema: DatabaseSchema): String {
        return gson.toJson(schema)
    }

    fun importFromJsonString(json: String): DatabaseSchema? {
        return try {
            gson.fromJson(json, DatabaseSchema::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun createFreshDatabase(): DatabaseSchema {
        return DatabaseSchema(
            tournaments = emptyList(),
            teams = emptyList(),
            players = emptyList(),
            bids = emptyList(),
            captainInvites = emptyList(),
            matches = emptyList(),
            ballRecords = emptyList(),
            activeTournamentId = "",
            activePlayerId = null,
            activeMatchId = null
        )
    }
}

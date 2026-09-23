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
) {
    fun sanitized(): DatabaseSchema {
        val safeTournaments = (tournaments ?: emptyList()).mapNotNull { it?.sanitized() }
        val safeTeams = (teams ?: emptyList()).mapNotNull { it?.sanitized() }
        val safePlayers = (players ?: emptyList()).mapNotNull { it?.sanitized() }
        val safeBids = (bids ?: emptyList()).mapNotNull { it?.sanitized() }
        val safeInvites = (captainInvites ?: emptyList()).mapNotNull { it?.sanitized() }
        val safeMatches = (matches ?: emptyList()).mapNotNull { it?.sanitized() }
        val safeBalls = (ballRecords ?: emptyList()).mapNotNull { it?.sanitized() }

        val activeTourneyId = if (activeTournamentId.isNotBlank() && safeTournaments.any { it.id == activeTournamentId }) {
            activeTournamentId
        } else {
            safeTournaments.firstOrNull()?.id ?: ""
        }

        val activePId = if (activePlayerId != null && safePlayers.any { it.id == activePlayerId }) {
            activePlayerId
        } else {
            safePlayers.firstOrNull()?.id
        }

        val activeMId = if (activeMatchId != null && safeMatches.any { it.id == activeMatchId }) {
            activeMatchId
        } else {
            safeMatches.firstOrNull()?.id
        }

        return DatabaseSchema(
            tournaments = safeTournaments,
            teams = safeTeams,
            players = safePlayers,
            bids = safeBids,
            captainInvites = safeInvites,
            matches = safeMatches,
            ballRecords = safeBalls,
            activeTournamentId = activeTourneyId,
            activePlayerId = activePId,
            activeMatchId = activeMId
        )
    }
}

class LocalDatabaseManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dbFile = File(context.filesDir, "cricket_database_v3.json")
    private val mutex = Mutex()

    suspend fun loadDatabase(): DatabaseSchema = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!dbFile.exists()) {
                val seedData = createFreshDatabase().sanitized()
                saveDatabaseInternal(seedData)
                seedData
            } else {
                try {
                    val json = dbFile.readText()
                    val raw = gson.fromJson(json, DatabaseSchema::class.java)
                    val safeSchema = raw?.sanitized() ?: createFreshDatabase().sanitized()
                    saveDatabaseInternal(safeSchema)
                    safeSchema
                } catch (e: Exception) {
                    e.printStackTrace()
                    val seed = createFreshDatabase().sanitized()
                    saveDatabaseInternal(seed)
                    seed
                }
            }
        }
    }

    suspend fun saveDatabase(schema: DatabaseSchema) = withContext(Dispatchers.IO) {
        mutex.withLock {
            saveDatabaseInternal(schema.sanitized())
        }
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (dbFile.exists()) {
                dbFile.delete()
            }
            saveDatabaseInternal(createFreshDatabase().sanitized())
        }
    }

    private fun saveDatabaseInternal(schema: DatabaseSchema) {
        try {
            val json = gson.toJson(schema.sanitized())
            dbFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToJsonString(schema: DatabaseSchema): String {
        return gson.toJson(schema.sanitized())
    }

    fun importFromJsonString(json: String): DatabaseSchema? {
        return try {
            gson.fromJson(json, DatabaseSchema::class.java)?.sanitized()
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


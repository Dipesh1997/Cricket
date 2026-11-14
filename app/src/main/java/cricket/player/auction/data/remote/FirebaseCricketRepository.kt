package cricket.player.auction.data.remote

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import cricket.player.auction.data.local.DatabaseSchema
import cricket.player.auction.data.local.LocalDatabaseManager
import cricket.player.auction.model.*
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

    var onSchemaRemoteUpdated: ((DatabaseSchema) -> Unit)? = null
    var onAuctionStateRemoteUpdated: ((tournamentId: String, readyCaptains: Map<String, Boolean>, isBiddingStarted: Boolean) -> Unit)? = null

    init {
        ensureAuthAndListen()
    }

    private fun ensureAuthAndListen() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    attachRealtimeListener()
                } else {
                    attachRealtimeListener()
                }
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
                    "appVersion" to "2.2",
                    "lastSeenTimestamp" to System.currentTimeMillis(),
                    "lastSeenDate" to dateFormat.format(Date())
                )
                database.reference.child("devices").child(deviceId).updateChildren(deviceInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    fun attachRealtimeListener() {
        if (activeListener != null) return

        registerDeviceInfo()

        // Clean up legacy flat/monolithic nodes
        scope.launch {
            try {
                database.reference.child("active_devices").removeValue()
                database.reference.child("users").removeValue()
                database.reference.child("database_schema").removeValue()
                database.reference.child("devices").child(deviceId).child("database_schema").removeValue()
                database.reference.child("devices").child(deviceId).child("device_info").removeValue()
            } catch (_: Exception) {}
        }

        // Listen to structured vertical "tournaments" branch
        val tourneysRef = database.reference.child("tournaments")
        activeRef = tourneysRef

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // Check if local has data to sync
                    scope.launch {
                        val localSchema = dbManager.loadDatabase().sanitized()
                        if (localSchema.tournaments.isNotEmpty()) {
                            syncDatabaseToFirebase(localSchema)
                        }
                    }
                    return
                }

                val tournamentsList = mutableListOf<Tournament>()
                val teamsList = mutableListOf<Team>()
                val playersList = mutableListOf<Player>()
                val matchesList = mutableListOf<Match>()
                val ballRecordsList = mutableListOf<BallRecord>()
                val bidsList = mutableListOf<Bid>()

                for (tSnap in snapshot.children) {
                    val tId = tSnap.key ?: continue
                    val infoSnap = tSnap.child("info")
                    val t = if (infoSnap.exists()) {
                        parseTournament(infoSnap, tId)
                    } else {
                        parseTournament(tSnap, tId)
                    }
                    if (t != null) tournamentsList.add(t)

                    // Teams
                    val teamsSnap = tSnap.child("teams")
                    for (c in teamsSnap.children) {
                        parseTeam(c, tId)?.let { teamsList.add(it) }
                    }

                    // Players
                    val playersSnap = tSnap.child("players")
                    for (c in playersSnap.children) {
                        parsePlayer(c, tId)?.let { playersList.add(it) }
                    }

                    // Matches
                    val matchesSnap = tSnap.child("matches")
                    for (c in matchesSnap.children) {
                        parseMatch(c, tId)?.let { matchesList.add(it) }
                    }

                    // Ball Records
                    val ballsSnap = tSnap.child("ballRecords")
                    for (c in ballsSnap.children) {
                        parseBallRecord(c)?.let { ballRecordsList.add(it) }
                    }

                    // Bids
                    val bidsSnap = tSnap.child("bids")
                    for (c in bidsSnap.children) {
                        parseBid(c)?.let { bidsList.add(it) }
                    }

                    // Auction State
                    val auctionSnap = tSnap.child("auctionState")
                    if (auctionSnap.exists()) {
                        val readyMap = mutableMapOf<String, Boolean>()
                        val readySnap = auctionSnap.child("readyCaptains")
                        for (rChild in readySnap.children) {
                            val teamK = rChild.key
                            val isR = rChild.getValue(Boolean::class.java) ?: false
                            if (teamK != null) readyMap[teamK] = isR
                        }
                        val isStarted = auctionSnap.child("isBiddingStarted").getValue(Boolean::class.java) ?: false
                        onAuctionStateRemoteUpdated?.invoke(tId, readyMap, isStarted)
                    }
                }

                if (tournamentsList.isNotEmpty() || teamsList.isNotEmpty() || playersList.isNotEmpty()) {
                    val remoteSchema = DatabaseSchema(
                        tournaments = tournamentsList,
                        teams = teamsList,
                        players = playersList,
                        matches = matchesList,
                        ballRecords = ballRecordsList,
                        bids = bidsList,
                        captainInvites = emptyList(),
                        activeTournamentId = tournamentsList.firstOrNull()?.id ?: ""
                    )
                    scope.launch {
                        onSchemaRemoteUpdated?.invoke(remoteSchema)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        }

        activeListener = listener
        tourneysRef.addValueEventListener(listener)
    }

    fun syncDatabaseToFirebase(schema: DatabaseSchema) {
        scope.launch {
            try {
                if (schema.tournaments.isEmpty() && schema.teams.isEmpty() && schema.players.isEmpty()) {
                    return@launch
                }

                // Push each tournament as a vertical, expandable object hierarchy
                for (tourney in schema.tournaments) {
                    val tRef = database.reference.child("tournaments").child(tourney.id)

                    // 1. Info node
                    val infoMap = mapOf(
                        "id" to tourney.id,
                        "name" to tourney.name,
                        "adminCode" to tourney.adminCode,
                        "scorerInviteCode" to tourney.scorerInviteCode,
                        "defaultPurse" to tourney.defaultPurse,
                        "maxSlots" to tourney.maxSlots,
                        "maxOverseas" to tourney.maxOverseas,
                        "createdAt" to tourney.createdAt,
                        "creatorDeviceId" to deviceId
                    )
                    tRef.child("info").setValue(infoMap)

                    // 2. Teams
                    val tourneyTeams = schema.teams.filter { it.tournamentId == tourney.id }
                    if (tourneyTeams.isNotEmpty()) {
                        val teamsMap = tourneyTeams.associate { team ->
                            team.id to mapOf(
                                "id" to team.id,
                                "tournamentId" to team.tournamentId,
                                "name" to team.name,
                                "shortCode" to team.shortCode,
                                "primaryColorHex" to team.primaryColorHex,
                                "totalPurse" to team.totalPurse,
                                "spentPurse" to team.spentPurse,
                                "inviteCode" to team.inviteCode,
                                "captainEmail" to (team.captainEmail ?: "")
                            )
                        }
                        tRef.child("teams").setValue(teamsMap)
                    }

                    // 3. Players
                    val tourneyPlayers = schema.players.filter { it.tournamentId == tourney.id }
                    if (tourneyPlayers.isNotEmpty()) {
                        val playersMap = tourneyPlayers.associate { player ->
                            player.id to mapOf(
                                "id" to player.id,
                                "tournamentId" to player.tournamentId,
                                "name" to player.name,
                                "role" to player.role.name,
                                "country" to player.country,
                                "isOverseas" to player.isOverseas,
                                "basePrice" to player.basePrice,
                                "currentBid" to player.currentBid,
                                "highestBidderTeamId" to (player.highestBidderTeamId ?: ""),
                                "status" to player.status.name,
                                "soldToTeamId" to (player.soldToTeamId ?: ""),
                                "soldPrice" to (player.soldPrice ?: 0.0),
                                "imageUrl" to player.imageUrl,
                                "stats" to player.stats,
                                "setName" to player.setName,
                                "isFixedSet" to player.isFixedSet
                            )
                        }
                        tRef.child("players").setValue(playersMap)
                    }

                    // 4. Matches
                    val tourneyMatches = schema.matches.filter { it.tournamentId == tourney.id }
                    if (tourneyMatches.isNotEmpty()) {
                        val matchesMap = tourneyMatches.associate { match ->
                            match.id to mapOf(
                                "id" to match.id,
                                "tournamentId" to match.tournamentId,
                                "teamAId" to match.teamAId,
                                "teamBId" to match.teamBId,
                                "totalOvers" to match.totalOvers,
                                "teamARuns" to match.teamARuns,
                                "teamAWickets" to match.teamAWickets,
                                "teamAOversBatted" to match.teamAOversBatted,
                                "teamBRuns" to match.teamBRuns,
                                "teamBWickets" to match.teamBWickets,
                                "teamBOversBatted" to match.teamBOversBatted,
                                "currentInnings" to match.currentInnings,
                                "status" to match.status.name,
                                "winnerTeamId" to (match.winnerTeamId ?: ""),
                                "resultSummary" to match.resultSummary,
                                "isLastPlayerBattingAllowed" to match.isLastPlayerBattingAllowed,
                                "tossWinnerTeamId" to (match.tossWinnerTeamId ?: ""),
                                "electedTo" to (match.tossDecision ?: "BAT")
                            )
                        }
                        tRef.child("matches").setValue(matchesMap)

                        // 5. Ball Records
                        val matchIds = tourneyMatches.map { it.id }.toSet()
                        val tourneyBalls = schema.ballRecords.filter { it.matchId in matchIds }
                        if (tourneyBalls.isNotEmpty()) {
                            val ballsMap = tourneyBalls.associate { ball ->
                                ball.id to mapOf(
                                    "id" to ball.id,
                                    "matchId" to ball.matchId,
                                    "innings" to ball.innings,
                                    "overNumber" to ball.overNumber,
                                    "ballNumber" to ball.ballNumber,
                                    "strikerId" to ball.strikerId,
                                    "nonStrikerId" to (ball.nonStrikerId ?: ""),
                                    "bowlerId" to ball.bowlerId,
                                    "runsScored" to ball.runsScored,
                                    "isExtra" to (ball.extraType != ExtraType.NONE),
                                    "extraType" to ball.extraType.name,
                                    "extraRuns" to ball.extraRuns,
                                    "isWicket" to ball.isWicket,
                                    "dismissalType" to (ball.dismissalType?.name ?: ""),
                                    "dismissedPlayerId" to (ball.dismissedPlayerId ?: ""),
                                    "commentary" to ball.commentary,
                                    "timestamp" to ball.timestamp
                                )
                            }
                            tRef.child("ballRecords").setValue(ballsMap)
                        }
                    }

                    // 6. Bids
                    val tourneyBids = schema.bids.filter { bid ->
                        tourneyPlayers.any { it.id == bid.playerId }
                    }
                    if (tourneyBids.isNotEmpty()) {
                        val bidsMap = tourneyBids.associate { bid ->
                            bid.id to mapOf(
                                "id" to bid.id,
                                "playerId" to bid.playerId,
                                "teamId" to bid.teamId,
                                "teamName" to bid.teamName,
                                "amount" to bid.amount,
                                "timestamp" to bid.timestamp,
                                "bidderEmail" to (bid.bidderEmail ?: "")
                            )
                        }
                        tRef.child("bids").setValue(bidsMap)
                    }

                    // Record on device
                    database.reference.child("devices").child(deviceId)
                        .child("myTournaments").child(tourney.id)
                        .setValue(mapOf(
                            "id" to tourney.id,
                            "name" to tourney.name,
                            "role" to "ADMIN",
                            "lastUpdated" to System.currentTimeMillis()
                        ))
                }

                // Update device telemetry lastSynced
                database.reference.child("devices").child(deviceId)
                    .child("lastSyncedTimestamp").setValue(System.currentTimeMillis())

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCaptainReady(tournamentId: String, teamId: String, isReady: Boolean) {
        if (tournamentId.isBlank() || teamId.isBlank()) return
        scope.launch {
            try {
                database.reference.child("tournaments")
                    .child(tournamentId)
                    .child("auctionState")
                    .child("readyCaptains")
                    .child(teamId)
                    .setValue(isReady)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBiddingStarted(tournamentId: String, started: Boolean) {
        if (tournamentId.isBlank()) return
        scope.launch {
            try {
                database.reference.child("tournaments")
                    .child(tournamentId)
                    .child("auctionState")
                    .child("isBiddingStarted")
                    .setValue(started)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registerDeviceTournament(tournamentId: String, role: String, tourneyName: String) {
        if (tournamentId.isBlank()) return
        scope.launch {
            try {
                database.reference.child("devices").child(deviceId)
                    .child("myTournaments").child(tournamentId)
                    .setValue(mapOf(
                        "id" to tournamentId,
                        "name" to tourneyName,
                        "role" to role,
                        "joinedAt" to System.currentTimeMillis()
                    ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearUserDatabase() {
        scope.launch {
            try {
                database.reference.child("tournaments").removeValue()
                database.reference.child("database_schema").removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- SNAPSHOT PARSING HELPERS ---
    private fun parseTournament(snap: DataSnapshot, defaultId: String): Tournament? {
        val id = snap.child("id").getValue(String::class.java) ?: defaultId
        val name = snap.child("name").getValue(String::class.java) ?: return null
        val defaultPurse = snap.child("defaultPurse").getValue(Double::class.java) ?: 100.0
        val maxSlots = snap.child("maxSlots").getValue(Int::class.java) ?: 25
        val maxOverseas = snap.child("maxOverseas").getValue(Int::class.java) ?: 8
        val adminCode = snap.child("adminCode").getValue(String::class.java) ?: ""
        val scorerCode = snap.child("scorerInviteCode").getValue(String::class.java) ?: ""
        val createdAt = snap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

        return Tournament(
            id = id,
            name = name,
            defaultPurse = defaultPurse,
            maxSlots = maxSlots,
            maxOverseas = maxOverseas,
            adminCode = adminCode,
            scorerInviteCode = scorerCode,
            createdAt = createdAt
        )
    }

    private fun parseTeam(snap: DataSnapshot, tournamentId: String): Team? {
        val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: return null
        val name = snap.child("name").getValue(String::class.java) ?: return null
        val shortCode = snap.child("shortCode").getValue(String::class.java) ?: name.take(3).uppercase()
        val primaryColorHex = snap.child("primaryColorHex").getValue(String::class.java) ?: "#004BA0"
        val totalPurse = snap.child("totalPurse").getValue(Double::class.java) ?: 100.0
        val spentPurse = snap.child("spentPurse").getValue(Double::class.java) ?: 0.0
        val inviteCode = snap.child("inviteCode").getValue(String::class.java) ?: ""
        val captainEmail = snap.child("captainEmail").getValue(String::class.java)

        return Team(
            id = id,
            tournamentId = tournamentId,
            name = name,
            shortCode = shortCode,
            primaryColorHex = primaryColorHex,
            totalPurse = totalPurse,
            spentPurse = spentPurse,
            inviteCode = inviteCode,
            captainEmail = if (captainEmail.isNullOrBlank()) null else captainEmail
        )
    }

    private fun parsePlayer(snap: DataSnapshot, tournamentId: String): Player? {
        val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: return null
        val name = snap.child("name").getValue(String::class.java) ?: return null
        val roleStr = snap.child("role").getValue(String::class.java) ?: "BATSMAN"
        val role = try { PlayerRole.valueOf(roleStr) } catch (_: Exception) { PlayerRole.BATSMAN }
        val country = snap.child("country").getValue(String::class.java) ?: "India"
        val isOverseas = snap.child("isOverseas").getValue(Boolean::class.java) ?: false
        val basePrice = snap.child("basePrice").getValue(Double::class.java) ?: 2.0
        val currentBid = snap.child("currentBid").getValue(Double::class.java) ?: basePrice
        val highestBidderTeamId = snap.child("highestBidderTeamId").getValue(String::class.java)
        val statusStr = snap.child("status").getValue(String::class.java) ?: "UP_NEXT"
        val status = try { PlayerStatus.valueOf(statusStr) } catch (_: Exception) { PlayerStatus.UP_NEXT }
        val soldToTeamId = snap.child("soldToTeamId").getValue(String::class.java)
        val soldPrice = snap.child("soldPrice").getValue(Double::class.java)
        val imageUrl = snap.child("imageUrl").getValue(String::class.java) ?: ""
        val stats = snap.child("stats").getValue(String::class.java) ?: ""
        val setName = snap.child("setName").getValue(String::class.java) ?: "Set 1"
        val isFixedSet = snap.child("isFixedSet").getValue(Boolean::class.java) ?: false

        return Player(
            id = id,
            tournamentId = tournamentId,
            name = name,
            role = role,
            country = country,
            isOverseas = isOverseas,
            basePrice = basePrice,
            currentBid = currentBid,
            highestBidderTeamId = if (highestBidderTeamId.isNullOrBlank()) null else highestBidderTeamId,
            status = status,
            soldToTeamId = if (soldToTeamId.isNullOrBlank()) null else soldToTeamId,
            soldPrice = soldPrice,
            imageUrl = imageUrl,
            stats = stats,
            setName = setName,
            isFixedSet = isFixedSet
        )
    }

    private fun parseMatch(snap: DataSnapshot, tournamentId: String): Match? {
        val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: return null
        val teamAId = snap.child("teamAId").getValue(String::class.java) ?: return null
        val teamBId = snap.child("teamBId").getValue(String::class.java) ?: return null
        val totalOvers = snap.child("totalOvers").getValue(Int::class.java) ?: 20
        val isLastPlayer = snap.child("isLastPlayerBattingAllowed").getValue(Boolean::class.java) ?: true
        val statusStr = snap.child("status").getValue(String::class.java) ?: "SCHEDULED"
        val status = try { MatchStatus.valueOf(statusStr) } catch (_: Exception) { MatchStatus.SCHEDULED }
        val currentInnings = snap.child("currentInnings").getValue(Int::class.java) ?: 1
        val teamARuns = snap.child("teamARuns").getValue(Int::class.java) ?: 0
        val teamAWkts = snap.child("teamAWickets").getValue(Int::class.java) ?: 0
        val teamAOvers = snap.child("teamAOversBatted").getValue(Double::class.java) ?: 0.0
        val teamBRuns = snap.child("teamBRuns").getValue(Int::class.java) ?: 0
        val teamBWkts = snap.child("teamBWickets").getValue(Int::class.java) ?: 0
        val teamBOvers = snap.child("teamBOversBatted").getValue(Double::class.java) ?: 0.0
        val winnerTeamId = snap.child("winnerTeamId").getValue(String::class.java)
        val resultSummary = snap.child("resultSummary").getValue(String::class.java) ?: ""

        return Match(
            id = id,
            tournamentId = tournamentId,
            teamAId = teamAId,
            teamBId = teamBId,
            totalOvers = totalOvers,
            isLastPlayerBattingAllowed = isLastPlayer,
            status = status,
            currentInnings = currentInnings,
            teamARuns = teamARuns,
            teamAWickets = teamAWkts,
            teamAOversBatted = teamAOvers,
            teamBRuns = teamBRuns,
            teamBWickets = teamBWkts,
            teamBOversBatted = teamBOvers,
            winnerTeamId = if (winnerTeamId.isNullOrBlank()) null else winnerTeamId,
            resultSummary = resultSummary
        )
    }

    private fun parseBallRecord(snap: DataSnapshot): BallRecord? {
        val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: return null
        val matchId = snap.child("matchId").getValue(String::class.java) ?: return null
        val innings = snap.child("innings").getValue(Int::class.java) ?: 1
        val overNumber = snap.child("overNumber").getValue(Int::class.java) ?: 0
        val ballNumber = snap.child("ballNumber").getValue(Int::class.java) ?: 1
        val strikerId = snap.child("strikerId").getValue(String::class.java) ?: ""
        val nonStrikerId = snap.child("nonStrikerId").getValue(String::class.java)
        val bowlerId = snap.child("bowlerId").getValue(String::class.java) ?: ""
        val runsScored = snap.child("runsScored").getValue(Int::class.java) ?: 0
        val extraTypeStr = snap.child("extraType").getValue(String::class.java) ?: "NONE"
        val extraType = try { ExtraType.valueOf(extraTypeStr) } catch (_: Exception) { ExtraType.NONE }
        val extraRuns = snap.child("extraRuns").getValue(Int::class.java) ?: 0
        val isWicket = snap.child("isWicket").getValue(Boolean::class.java) ?: false
        val dismissalTypeStr = snap.child("dismissalType").getValue(String::class.java)
        val dismissalType = if (!dismissalTypeStr.isNullOrBlank()) {
            try { DismissalType.valueOf(dismissalTypeStr) } catch (_: Exception) { null }
        } else null
        val dismissedPlayerId = snap.child("dismissedPlayerId").getValue(String::class.java)
        val commentary = snap.child("commentary").getValue(String::class.java) ?: ""
        val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

        return BallRecord(
            id = id,
            matchId = matchId,
            innings = innings,
            overNumber = overNumber,
            ballNumber = ballNumber,
            strikerId = strikerId,
            nonStrikerId = if (nonStrikerId.isNullOrBlank()) null else nonStrikerId,
            bowlerId = bowlerId,
            runsScored = runsScored,
            extraType = extraType,
            extraRuns = extraRuns,
            isWicket = isWicket,
            dismissalType = dismissalType,
            dismissedPlayerId = if (dismissedPlayerId.isNullOrBlank()) null else dismissedPlayerId,
            commentary = commentary,
            timestamp = timestamp
        )
    }

    private fun parseBid(snap: DataSnapshot): Bid? {
        val id = snap.child("id").getValue(String::class.java) ?: snap.key ?: return null
        val playerId = snap.child("playerId").getValue(String::class.java) ?: return null
        val teamId = snap.child("teamId").getValue(String::class.java) ?: return null
        val teamName = snap.child("teamName").getValue(String::class.java) ?: ""
        val amount = snap.child("amount").getValue(Double::class.java) ?: 0.0
        val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
        val bidderEmail = snap.child("bidderEmail").getValue(String::class.java)

        return Bid(
            id = id,
            playerId = playerId,
            teamId = teamId,
            teamName = teamName,
            amount = amount,
            timestamp = timestamp,
            bidderEmail = bidderEmail
        )
    }
}

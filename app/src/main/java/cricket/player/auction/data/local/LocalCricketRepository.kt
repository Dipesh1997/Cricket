package cricket.player.auction.data.local

import android.content.Context
import cricket.player.auction.data.remote.FirebaseCricketRepository
import cricket.player.auction.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LocalCricketRepository(context: Context) {

    private val dbManager = LocalDatabaseManager(context)
    private val firebaseRepo = FirebaseCricketRepository(context, dbManager)
    private val scope = CoroutineScope(Dispatchers.IO)

    val deviceTourneyManager = DeviceTournamentManager(context)
    private val _myTournamentIds = MutableStateFlow<Set<String>>(deviceTourneyManager.getMyTournamentIds())
    val myTournamentIds: StateFlow<Set<String>> = _myTournamentIds.asStateFlow()

    private val _isMasterAdminMode = MutableStateFlow(deviceTourneyManager.isMasterAdminMode())
    val isMasterAdminMode: StateFlow<Boolean> = _isMasterAdminMode.asStateFlow()

    private val _hideDemoData = MutableStateFlow(deviceTourneyManager.isHideDemoData())
    val hideDemoData: StateFlow<Boolean> = _hideDemoData.asStateFlow()

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    fun isDemoOrOffline(tournamentId: String?): Boolean {
        if (tournamentId.isNullOrBlank()) return false
        if (tournamentId.startsWith("demo_")) return true
        if (tournamentId == "t1" || tournamentId == "tournament_1") return true
        val tourney = _tournaments.value.find { it.id == tournamentId }
        if (tourney?.isOfflineOnly == true) return true
        if (tourney?.name?.equals("IPL 2026 Mega Auction", ignoreCase = true) == true) return true
        return false
    }

    // Expose all tournaments if Master Admin is active, or only tournaments this device created or joined!
    val userTournaments: StateFlow<List<Tournament>> = combine(_tournaments, _myTournamentIds, _isMasterAdminMode, _hideDemoData) { all, myIds, isMaster, hideDemo ->
        val nonLegacy = all.filterNot { it.id == "t1" || it.id == "tournament_1" || it.name.equals("IPL 2026 Mega Auction", ignoreCase = true) }
        val base = if (isMaster) {
            nonLegacy
        } else {
            nonLegacy.filter { it.id in myIds }
        }
        if (hideDemo) base.filter { !it.isOfflineOnly && !it.id.startsWith("demo_") } else base
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _captainReadyMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val captainReadyMap: StateFlow<Map<String, Boolean>> = _captainReadyMap.asStateFlow()

    private val _isBiddingStarted = MutableStateFlow(false)
    val isBiddingStarted: StateFlow<Boolean> = _isBiddingStarted.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _bids = MutableStateFlow<List<Bid>>(emptyList())
    val bids: StateFlow<List<Bid>> = _bids.asStateFlow()

    private val _captainInvites = MutableStateFlow<List<CaptainInvite>>(emptyList())
    val captainInvites: StateFlow<List<CaptainInvite>> = _captainInvites.asStateFlow()

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _ballRecords = MutableStateFlow<List<BallRecord>>(emptyList())
    val ballRecords: StateFlow<List<BallRecord>> = _ballRecords.asStateFlow()

    private val _activeTournamentId = MutableStateFlow("")
    val activeTournamentId: StateFlow<String> = _activeTournamentId.asStateFlow()

    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

    private val _activeMatchId = MutableStateFlow<String?>(null)
    val activeMatchId: StateFlow<String?> = _activeMatchId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        firebaseRepo.onSchemaRemoteUpdated = { remoteSchema ->
            mergeRemoteSchema(remoteSchema)
        }
        firebaseRepo.onAuctionStateRemoteUpdated = { tId, readyMap, isStarted ->
            if (tId == _activeTournamentId.value) {
                _captainReadyMap.value = readyMap
                _isBiddingStarted.value = isStarted
            }
        }
        firebaseRepo.onDeviceTournamentsRemoteUpdated = { remoteDeviceTourneys ->
            val currentMyIds = deviceTourneyManager.getMyTournamentIds()
            val demoIds = currentMyIds.filter { isDemoOrOffline(it) }
            val cleanedSet = mutableSetOf<String>()
            cleanedSet.addAll(demoIds)

            remoteDeviceTourneys.forEach { (tId, role) ->
                val tourney = _tournaments.value.find { it.id == tId }
                if (tourney != null && tourney.creatorDeviceId.isNotBlank() && tourney.creatorDeviceId != firebaseRepo.deviceId && role == "ADMIN") {
                    // Contaminated foreign tournament - prune from this device's remote list
                    firebaseRepo.removeDeviceTournament(tId)
                } else {
                    cleanedSet.add(tId)
                }
            }

            // Keep tournaments created on this device
            _tournaments.value.filter { it.creatorDeviceId.isNotBlank() && it.creatorDeviceId == firebaseRepo.deviceId }.forEach {
                cleanedSet.add(it.id)
            }
            deviceTourneyManager.setMyTournaments(cleanedSet, remoteDeviceTourneys)
            _myTournamentIds.value = cleanedSet
        }
        scope.launch {
            val schema = dbManager.loadDatabase().sanitized()
            _tournaments.value = schema.tournaments
            _teams.value = schema.teams
            _players.value = schema.players
            _bids.value = schema.bids
            _captainInvites.value = schema.captainInvites
            _matches.value = schema.matches
            _ballRecords.value = schema.ballRecords
            _activeTournamentId.value = schema.activeTournamentId.ifEmpty { schema.tournaments.firstOrNull()?.id ?: "" }
            _activePlayerId.value = schema.activePlayerId ?: schema.players.firstOrNull()?.id
            _activeMatchId.value = schema.activeMatchId ?: schema.matches.firstOrNull()?.id

            // Clean up any tournaments in local deviceTourneyManager that belong to foreign devices
            val currentMyIds = deviceTourneyManager.getMyTournamentIds()
            val validLocalIds = currentMyIds.filter { tId ->
                val tourney = schema.tournaments.find { it.id == tId }
                if (tourney != null && tourney.creatorDeviceId.isNotBlank() && tourney.creatorDeviceId != firebaseRepo.deviceId) {
                    val role = deviceTourneyManager.getTournamentRole(tId)
                    role != "ADMIN"
                } else {
                    true
                }
            }.toSet()
            if (validLocalIds != currentMyIds) {
                deviceTourneyManager.setMyTournaments(validLocalIds)
                _myTournamentIds.value = validLocalIds
            }

            // CRITICAL: Only push to Firebase if local database actually has valid non-legacy data.
            // On fresh install / after reinstall, local schema is empty.
            // Pushing an empty schema would wipe out the user's existing cloud data!
            val nonLegacyTourneys = schema.tournaments.filterNot { isDemoOrOffline(it.id) || it.id == "t1" || it.id == "tournament_1" || it.name.equals("IPL 2026 Mega Auction", ignoreCase = true) }
            if (nonLegacyTourneys.isNotEmpty()) {
                firebaseRepo.syncDatabaseToFirebase(schema.copy(tournaments = nonLegacyTourneys))
            }
        }
    }

    private fun mergeRemoteSchema(remoteSchema: DatabaseSchema) {
        val currentTournaments = _tournaments.value
        val remoteTournaments = remoteSchema.tournaments

        if (currentTournaments.isEmpty() && remoteTournaments.isNotEmpty()) {
            // Fresh install or re-install recovery: adopt remote data directly
            _tournaments.value = remoteTournaments
            _teams.value = remoteSchema.teams
            _players.value = remoteSchema.players
            _bids.value = remoteSchema.bids
            _matches.value = remoteSchema.matches
            _ballRecords.value = remoteSchema.ballRecords
            _captainInvites.value = remoteSchema.captainInvites
            _activeTournamentId.value = remoteSchema.activeTournamentId.ifEmpty { remoteTournaments.firstOrNull()?.id ?: "" }
            _activePlayerId.value = remoteSchema.activePlayerId ?: remoteSchema.players.firstOrNull()?.id
            _activeMatchId.value = remoteSchema.activeMatchId ?: remoteSchema.matches.firstOrNull()?.id

            // Only register tournaments created by this device
            remoteTournaments.forEach { rTourney ->
                if (rTourney.creatorDeviceId.isNotBlank() && rTourney.creatorDeviceId == firebaseRepo.deviceId) {
                    deviceTourneyManager.addTournament(rTourney.id, "ADMIN")
                }
            }
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
        } else {
            // Merge: remote updates override matching IDs, local-only entities are preserved
            val mergedTourneys = (remoteTournaments + currentTournaments).distinctBy { it.id }
            val mergedTeams = (remoteSchema.teams + _teams.value).distinctBy { it.id }
            val mergedPlayers = (remoteSchema.players + _players.value).distinctBy { it.id }
            val mergedBids = (remoteSchema.bids + _bids.value).distinctBy { it.id }
            val mergedMatches = (remoteSchema.matches + _matches.value).distinctBy { it.id }
            val mergedBallRecords = (remoteSchema.ballRecords + _ballRecords.value).distinctBy { it.id }
            val mergedInvites = (remoteSchema.captainInvites + _captainInvites.value).distinctBy { it.code }

            _tournaments.value = mergedTourneys
            _teams.value = mergedTeams
            _players.value = mergedPlayers
            _bids.value = mergedBids
            _matches.value = mergedMatches
            _ballRecords.value = mergedBallRecords
            _captainInvites.value = mergedInvites

            // Only register tournaments created by this device
            remoteTournaments.forEach { rTourney ->
                if (rTourney.creatorDeviceId.isNotBlank() && rTourney.creatorDeviceId == firebaseRepo.deviceId) {
                    deviceTourneyManager.addTournament(rTourney.id, "ADMIN")
                }
            }
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()

            if (_activeTournamentId.value.isBlank() || (!_tournaments.value.any { it.id == _activeTournamentId.value })) {
                _activeTournamentId.value = _tournaments.value.firstOrNull { it.id in _myTournamentIds.value && !isDemoOrOffline(it.id) }?.id
                    ?: _tournaments.value.firstOrNull { it.id in _myTournamentIds.value }?.id
                    ?: ""
            }
        }

        scope.launch {
            val updatedSchema = DatabaseSchema(
                tournaments = _tournaments.value,
                teams = _teams.value,
                players = _players.value,
                bids = _bids.value,
                captainInvites = _captainInvites.value,
                matches = _matches.value,
                ballRecords = _ballRecords.value,
                activeTournamentId = _activeTournamentId.value,
                activePlayerId = _activePlayerId.value,
                activeMatchId = _activeMatchId.value
            )
            // Immediately store locally on disk
            dbManager.saveDatabase(updatedSchema)
        }
    }


    private fun persistAsync() {
        scope.launch {
            val schema = DatabaseSchema(
                tournaments = _tournaments.value,
                teams = _teams.value,
                players = _players.value,
                bids = _bids.value,
                captainInvites = _captainInvites.value,
                matches = _matches.value,
                ballRecords = _ballRecords.value,
                activeTournamentId = _activeTournamentId.value,
                activePlayerId = _activePlayerId.value,
                activeMatchId = _activeMatchId.value
            )
            dbManager.saveDatabase(schema)

            // CRITICAL: Filter out any offline-only / demo data before pushing to Firebase
            // This guarantees Firebase Realtime Database is NEVER reset or corrupted by demo data!
            val cloudTourneys = _tournaments.value.filter { !isDemoOrOffline(it.id) }
            if (cloudTourneys.isNotEmpty()) {
                val cloudTourneyIds = cloudTourneys.map { it.id }.toSet()
                val cloudTeams = _teams.value.filter { it.tournamentId in cloudTourneyIds }
                val cloudPlayers = _players.value.filter { it.tournamentId in cloudTourneyIds }
                val cloudMatches = _matches.value.filter { it.tournamentId in cloudTourneyIds }
                val cloudMatchIds = cloudMatches.map { it.id }.toSet()
                val cloudBalls = _ballRecords.value.filter { it.matchId in cloudMatchIds }
                val cloudBids = _bids.value.filter { bid -> cloudPlayers.any { it.id == bid.playerId } }
                val cloudActiveId = if (isDemoOrOffline(_activeTournamentId.value)) {
                    cloudTourneys.firstOrNull()?.id ?: ""
                } else {
                    _activeTournamentId.value
                }

                val cloudSchema = DatabaseSchema(
                    tournaments = cloudTourneys,
                    teams = cloudTeams,
                    players = cloudPlayers,
                    bids = cloudBids,
                    captainInvites = _captainInvites.value,
                    matches = cloudMatches,
                    ballRecords = cloudBalls,
                    activeTournamentId = cloudActiveId,
                    activePlayerId = _activePlayerId.value,
                    activeMatchId = _activeMatchId.value
                )
                firebaseRepo.syncDatabaseToFirebase(cloudSchema)
            }
        }
    }

    // --- Export & Import Backup ---
    fun exportDatabaseJson(): String {
        val schema = DatabaseSchema(
            tournaments = _tournaments.value,
            teams = _teams.value,
            players = _players.value,
            bids = _bids.value,
            captainInvites = _captainInvites.value,
            matches = _matches.value,
            ballRecords = _ballRecords.value,
            activeTournamentId = _activeTournamentId.value,
            activePlayerId = _activePlayerId.value,
            activeMatchId = _activeMatchId.value
        )
        return dbManager.exportToJsonString(schema)
    }

    fun importDatabaseJson(jsonString: String): Boolean {
        val schema = dbManager.importFromJsonString(jsonString) ?: return false
        _tournaments.value = schema.tournaments
        _teams.value = schema.teams
        _players.value = schema.players
        _bids.value = schema.bids
        _captainInvites.value = schema.captainInvites
        _matches.value = schema.matches
        _ballRecords.value = schema.ballRecords
        _activeTournamentId.value = schema.activeTournamentId.ifEmpty { schema.tournaments.firstOrNull()?.id ?: "" }
        _activePlayerId.value = schema.activePlayerId ?: schema.players.firstOrNull()?.id
        _activeMatchId.value = schema.activeMatchId ?: schema.matches.firstOrNull()?.id

        persistAsync()
        return true
    }

    // --- Clear All Data ---
    fun clearAllData() {

        scope.launch {
            dbManager.clearDatabase()
            firebaseRepo.clearUserDatabase()
            _tournaments.value = emptyList()
            _teams.value = emptyList()
            _players.value = emptyList()
            _bids.value = emptyList()
            _captainInvites.value = emptyList()
            _matches.value = emptyList()
            _ballRecords.value = emptyList()
            _activeTournamentId.value = ""
            _activePlayerId.value = null
            _activeMatchId.value = null
        }
    }

    // --- Tournament Management ---
    fun setActiveTournament(tournamentId: String) {
        _activeTournamentId.value = tournamentId
        persistAsync()
    }


    fun createTournament(name: String, defaultPurse: Double, maxSlots: Int, maxOverseas: Int): Tournament {
        val scorerCode = "SC" + UUID.randomUUID().toString().take(4).uppercase()
        val adminCode = "ADM" + UUID.randomUUID().toString().take(4).uppercase()
        val spectatorCode = "SPEC" + UUID.randomUUID().toString().take(4).uppercase()
        val newTourney = Tournament(
            id = "t_${System.currentTimeMillis()}",
            name = name,
            defaultPurse = defaultPurse,
            maxSlots = maxSlots,
            maxOverseas = maxOverseas,
            scorerInviteCode = scorerCode,
            adminCode = adminCode,
            spectatorInviteCode = spectatorCode,
            creatorDeviceId = firebaseRepo.deviceId
        )
        _tournaments.value = _tournaments.value + newTourney
        _activeTournamentId.value = newTourney.id
        deviceTourneyManager.addTournament(newTourney.id, "ADMIN")
        _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
        firebaseRepo.registerDeviceTournament(newTourney.id, "ADMIN", newTourney.name)
        persistAsync()
        return newTourney
    }

    fun createDemoTournament(): Tournament {
        val tourneyId = "demo_${System.currentTimeMillis()}"
        val demoTourney = Tournament(
            id = tourneyId,
            name = "🎮 Demo Cup 2026 (Offline)",
            defaultPurse = 100.0,
            maxSlots = 15,
            maxOverseas = 5,
            scorerInviteCode = "DEMOSC1",
            adminCode = "DEMOADM1",
            spectatorInviteCode = "DEMOSPEC",
            isOfflineOnly = true,
            creatorDeviceId = firebaseRepo.deviceId
        )

        // 4 Demo Teams
        val team1 = Team(id = "team_${tourneyId}_1", tournamentId = tourneyId, name = "Mumbai Strikers", shortCode = "MS", primaryColorHex = "#004BA0", totalPurse = 100.0, inviteCode = "CAPTMS1")
        val team2 = Team(id = "team_${tourneyId}_2", tournamentId = tourneyId, name = "Chennai Superstars", shortCode = "CS", primaryColorHex = "#F9CD05", totalPurse = 100.0, inviteCode = "CAPTCS1")
        val team3 = Team(id = "team_${tourneyId}_3", tournamentId = tourneyId, name = "Bangalore Challengers", shortCode = "BC", primaryColorHex = "#EC1C24", totalPurse = 100.0, inviteCode = "CAPTBC1")
        val team4 = Team(id = "team_${tourneyId}_4", tournamentId = tourneyId, name = "Kolkata Riders", shortCode = "KR", primaryColorHex = "#3A225D", totalPurse = 100.0, inviteCode = "CAPTKR1")
        val demoTeams = listOf(team1, team2, team3, team4)

        // 20 Demo Players across Batsmen, Bowlers, All-rounders, Wicket Keepers
        val demoPlayers = listOf(
            Player(id = "p_${tourneyId}_1", tournamentId = tourneyId, name = "Virat Kohli", role = PlayerRole.BATSMAN, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 7263, Avg: 37.2, SR: 130.0", setName = "Set 1 - Marquee"),
            Player(id = "p_${tourneyId}_2", tournamentId = tourneyId, name = "Rohit Sharma", role = PlayerRole.BATSMAN, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 6211, Avg: 29.5, SR: 130.8", setName = "Set 1 - Marquee"),
            Player(id = "p_${tourneyId}_3", tournamentId = tourneyId, name = "Jasprit Bumrah", role = PlayerRole.BOWLER, country = "India", isOverseas = false, basePrice = 2.0, stats = "Wkts: 145, Econ: 7.39, Avg: 23.3", setName = "Set 1 - Marquee"),
            Player(id = "p_${tourneyId}_4", tournamentId = tourneyId, name = "Hardik Pandya", role = PlayerRole.ALL_ROUNDER, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 2309, Wkts: 53, SR: 145.8", setName = "Set 1 - Marquee"),
            Player(id = "p_${tourneyId}_5", tournamentId = tourneyId, name = "MS Dhoni", role = PlayerRole.WICKET_KEEPER, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 5082, Avg: 38.8, Catches: 142", setName = "Set 2 - Keepers"),
            Player(id = "p_${tourneyId}_6", tournamentId = tourneyId, name = "Jos Buttler", role = PlayerRole.WICKET_KEEPER, country = "England", isOverseas = true, basePrice = 2.0, stats = "Runs: 3223, SR: 148.3, 5x 100s", setName = "Set 2 - Keepers"),
            Player(id = "p_${tourneyId}_7", tournamentId = tourneyId, name = "Rishabh Pant", role = PlayerRole.WICKET_KEEPER, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 2838, SR: 148.0, 1x 100", setName = "Set 2 - Keepers"),
            Player(id = "p_${tourneyId}_8", tournamentId = tourneyId, name = "KL Rahul", role = PlayerRole.BATSMAN, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 4163, Avg: 46.8, SR: 134.4", setName = "Set 2 - Keepers"),
            Player(id = "p_${tourneyId}_9", tournamentId = tourneyId, name = "Glenn Maxwell", role = PlayerRole.ALL_ROUNDER, country = "Australia", isOverseas = true, basePrice = 2.0, stats = "Runs: 2719, SR: 157.0, Wkts: 31", setName = "Set 3 - All-Rounders"),
            Player(id = "p_${tourneyId}_10", tournamentId = tourneyId, name = "Ravindra Jadeja", role = PlayerRole.ALL_ROUNDER, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 2692, Wkts: 152, Econ: 7.60", setName = "Set 3 - All-Rounders"),
            Player(id = "p_${tourneyId}_11", tournamentId = tourneyId, name = "Andre Russell", role = PlayerRole.ALL_ROUNDER, country = "West Indies", isOverseas = true, basePrice = 2.0, stats = "Runs: 2262, SR: 174.0, Wkts: 96", setName = "Set 3 - All-Rounders"),
            Player(id = "p_${tourneyId}_12", tournamentId = tourneyId, name = "Rashid Khan", role = PlayerRole.BOWLER, country = "Afghanistan", isOverseas = true, basePrice = 2.0, stats = "Wkts: 139, Econ: 6.67, Avg: 20.7", setName = "Set 3 - All-Rounders"),
            Player(id = "p_${tourneyId}_13", tournamentId = tourneyId, name = "Mitchell Starc", role = PlayerRole.BOWLER, country = "Australia", isOverseas = true, basePrice = 2.0, stats = "Wkts: 51, Econ: 7.98, Avg: 22.8", setName = "Set 4 - Fast Bowlers"),
            Player(id = "p_${tourneyId}_14", tournamentId = tourneyId, name = "Trent Boult", role = PlayerRole.BOWLER, country = "New Zealand", isOverseas = true, basePrice = 2.0, stats = "Wkts: 105, Econ: 8.29, Powerplay: 50w", setName = "Set 4 - Fast Bowlers"),
            Player(id = "p_${tourneyId}_15", tournamentId = tourneyId, name = "Kagiso Rabada", role = PlayerRole.BOWLER, country = "South Africa", isOverseas = true, basePrice = 2.0, stats = "Wkts: 106, Econ: 8.42, Strike Rate: 14.8", setName = "Set 4 - Fast Bowlers"),
            Player(id = "p_${tourneyId}_16", tournamentId = tourneyId, name = "Mohammed Shami", role = PlayerRole.BOWLER, country = "India", isOverseas = false, basePrice = 2.0, stats = "Wkts: 127, Econ: 8.44, Purple Cap 2023", setName = "Set 4 - Fast Bowlers"),
            Player(id = "p_${tourneyId}_17", tournamentId = tourneyId, name = "Suryakumar Yadav", role = PlayerRole.BATSMAN, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 3249, SR: 143.3, 360-degree hitter", setName = "Set 5 - Batters"),
            Player(id = "p_${tourneyId}_18", tournamentId = tourneyId, name = "Shubman Gill", role = PlayerRole.BATSMAN, country = "India", isOverseas = false, basePrice = 2.0, stats = "Runs: 2790, Avg: 37.7, Orange Cap 2023", setName = "Set 5 - Batters"),
            Player(id = "p_${tourneyId}_19", tournamentId = tourneyId, name = "David Warner", role = PlayerRole.BATSMAN, country = "Australia", isOverseas = true, basePrice = 2.0, stats = "Runs: 6397, Avg: 41.5, 3x Orange Cap", setName = "Set 5 - Batters"),
            Player(id = "p_${tourneyId}_20", tournamentId = tourneyId, name = "Bhuvneshwar Kumar", role = PlayerRole.BOWLER, country = "India", isOverseas = false, basePrice = 1.5, stats = "Wkts: 170, Econ: 7.39, 2x Purple Cap", setName = "Set 5 - Batters")
        )

        // Remove any old demo tournament if existing so only 1 demo tournament exists at a time
        val oldDemoTourneyIds = _tournaments.value.filter { isDemoOrOffline(it.id) }.map { it.id }.toSet()
        val nonDemoTourneys = _tournaments.value.filterNot { it.id in oldDemoTourneyIds }
        val nonDemoTeams = _teams.value.filterNot { it.tournamentId in oldDemoTourneyIds }
        val nonDemoPlayers = _players.value.filterNot { it.tournamentId in oldDemoTourneyIds }

        _tournaments.value = nonDemoTourneys + demoTourney
        _teams.value = nonDemoTeams + demoTeams
        _players.value = nonDemoPlayers + demoPlayers
        _activeTournamentId.value = demoTourney.id
        _activePlayerId.value = demoPlayers.first().id
        deviceTourneyManager.addTournament(demoTourney.id, "ADMIN")
        _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()

        // CRITICAL: Demo data is purely local and offline!
        // DO NOT call firebaseRepo.syncDatabaseToFirebase or push to cloud!
        // Save only locally to disk
        scope.launch {
            val schema = DatabaseSchema(
                tournaments = _tournaments.value,
                teams = _teams.value,
                players = _players.value,
                bids = _bids.value,
                captainInvites = _captainInvites.value,
                matches = _matches.value,
                ballRecords = _ballRecords.value,
                activeTournamentId = _activeTournamentId.value,
                activePlayerId = _activePlayerId.value,
                activeMatchId = _activeMatchId.value
            )
            dbManager.saveDatabase(schema)
        }
        return demoTourney
    }

    fun setCaptainReady(teamId: String, isReady: Boolean) {
        val updated = _captainReadyMap.value.toMutableMap()
        updated[teamId] = isReady
        _captainReadyMap.value = updated
        if (!isDemoOrOffline(_activeTournamentId.value)) {
            firebaseRepo.setCaptainReady(_activeTournamentId.value, teamId, isReady)
        }
    }

    fun setBiddingStarted(started: Boolean) {
        _isBiddingStarted.value = started
        if (!isDemoOrOffline(_activeTournamentId.value)) {
            firebaseRepo.setBiddingStarted(_activeTournamentId.value, started)
        }
    }

    fun joinTournamentWithCode(inputCode: String): Pair<Tournament?, UserRole?> {
        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.isBlank()) return Pair(null, null)

        // 1. Check if code matches Admin Joining Code
        val adminTourney = _tournaments.value.find { it.adminCode.equals(cleanCode, ignoreCase = true) }
        if (adminTourney != null) {
            _activeTournamentId.value = adminTourney.id
            deviceTourneyManager.addTournament(adminTourney.id, "ADMIN")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(adminTourney.id, "ADMIN", adminTourney.name)
            persistAsync()
            return Pair(adminTourney, UserRole.ADMIN_AUCTIONEER)
        }

        // 2. Check if code matches Scorer Invite Code
        val scorerTourney = _tournaments.value.find { it.scorerInviteCode.equals(cleanCode, ignoreCase = true) }
        if (scorerTourney != null) {
            _activeTournamentId.value = scorerTourney.id
            deviceTourneyManager.addTournament(scorerTourney.id, "SCORER")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(scorerTourney.id, "SCORER", scorerTourney.name)
            persistAsync()
            return Pair(scorerTourney, UserRole.SCORER)
        }

        // 3. Check if code matches Spectator Invite Code
        val spectatorTourney = _tournaments.value.find { it.spectatorInviteCode.equals(cleanCode, ignoreCase = true) }
        if (spectatorTourney != null) {
            _activeTournamentId.value = spectatorTourney.id
            deviceTourneyManager.addTournament(spectatorTourney.id, "SPECTATOR")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(spectatorTourney.id, "SPECTATOR", spectatorTourney.name)
            persistAsync()
            return Pair(spectatorTourney, UserRole.SPECTATOR)
        }

        // 4. Check if code matches Team Captain Invite Code
        val matchingTeam = _teams.value.find { it.inviteCode.equals(cleanCode, ignoreCase = true) }
        if (matchingTeam != null) {
            var parentTourney = _tournaments.value.find { it.id == matchingTeam.tournamentId }
            if (parentTourney == null) {
                parentTourney = Tournament(
                    id = matchingTeam.tournamentId.ifEmpty { "t_${System.currentTimeMillis()}" },
                    name = "Tournament (${matchingTeam.shortCode})",
                    defaultPurse = 100.0,
                    maxSlots = 25,
                    maxOverseas = 8,
                    scorerInviteCode = "SC" + UUID.randomUUID().toString().take(4).uppercase(),
                    adminCode = "ADM" + UUID.randomUUID().toString().take(4).uppercase(),
                    spectatorInviteCode = "SPEC" + UUID.randomUUID().toString().take(4).uppercase(),
                    creatorDeviceId = firebaseRepo.deviceId
                )
                _tournaments.value = _tournaments.value + parentTourney
            }
            _activeTournamentId.value = parentTourney.id
            deviceTourneyManager.addTournament(parentTourney.id, "CAPTAIN")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(parentTourney.id, "CAPTAIN", parentTourney.name)
            persistAsync()
            return Pair(parentTourney, UserRole.TEAM_CAPTAIN)
        }

        // 5. Auto-register Admin code starting with ADM
        if (cleanCode.startsWith("ADM")) {
            val newTourney = Tournament(
                id = "t_${System.currentTimeMillis()}",
                name = "Tournament ($cleanCode)",
                defaultPurse = 100.0,
                maxSlots = 25,
                maxOverseas = 8,
                adminCode = cleanCode,
                scorerInviteCode = "SC" + UUID.randomUUID().toString().take(4).uppercase(),
                spectatorInviteCode = "SPEC" + UUID.randomUUID().toString().take(4).uppercase(),
                creatorDeviceId = firebaseRepo.deviceId
            )
            _tournaments.value = _tournaments.value + newTourney
            _activeTournamentId.value = newTourney.id
            deviceTourneyManager.addTournament(newTourney.id, "ADMIN")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(newTourney.id, "ADMIN", newTourney.name)
            persistAsync()
            return Pair(newTourney, UserRole.ADMIN_AUCTIONEER)
        }

        // 6. Auto-register Scorer code starting with SC
        if (cleanCode.startsWith("SC")) {
            val newTourney = Tournament(
                id = "t_${System.currentTimeMillis()}",
                name = "Scored Tournament ($cleanCode)",
                defaultPurse = 100.0,
                maxSlots = 25,
                maxOverseas = 8,
                scorerInviteCode = cleanCode,
                adminCode = "ADM" + UUID.randomUUID().toString().take(4).uppercase(),
                spectatorInviteCode = "SPEC" + UUID.randomUUID().toString().take(4).uppercase(),
                creatorDeviceId = firebaseRepo.deviceId
            )
            _tournaments.value = _tournaments.value + newTourney
            _activeTournamentId.value = newTourney.id
            deviceTourneyManager.addTournament(newTourney.id, "SCORER")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(newTourney.id, "SCORER", newTourney.name)
            persistAsync()
            return Pair(newTourney, UserRole.SCORER)
        }

        // 7. Auto-register Spectator code starting with SPEC
        if (cleanCode.startsWith("SPEC")) {
            val newTourney = Tournament(
                id = "t_${System.currentTimeMillis()}",
                name = "Tournament ($cleanCode)",
                defaultPurse = 100.0,
                maxSlots = 25,
                maxOverseas = 8,
                spectatorInviteCode = cleanCode,
                scorerInviteCode = "SC" + UUID.randomUUID().toString().take(4).uppercase(),
                adminCode = "ADM" + UUID.randomUUID().toString().take(4).uppercase(),
                creatorDeviceId = firebaseRepo.deviceId
            )
            _tournaments.value = _tournaments.value + newTourney
            _activeTournamentId.value = newTourney.id
            deviceTourneyManager.addTournament(newTourney.id, "SPECTATOR")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(newTourney.id, "SPECTATOR", newTourney.name)
            persistAsync()
            return Pair(newTourney, UserRole.SPECTATOR)
        }

        // 6. Fallback: Any other valid code (e.g. 7-character code or custom code)
        if (cleanCode.length >= 3) {
            val newTourney = Tournament(
                id = "t_${System.currentTimeMillis()}",
                name = "Tournament ($cleanCode)",
                defaultPurse = 100.0,
                maxSlots = 25,
                maxOverseas = 8,
                adminCode = cleanCode,
                scorerInviteCode = "SC" + UUID.randomUUID().toString().take(4).uppercase(),
                spectatorInviteCode = "SPEC" + UUID.randomUUID().toString().take(4).uppercase(),
                creatorDeviceId = firebaseRepo.deviceId
            )
            _tournaments.value = _tournaments.value + newTourney
            _activeTournamentId.value = newTourney.id
            deviceTourneyManager.addTournament(newTourney.id, "ADMIN")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(newTourney.id, "ADMIN", newTourney.name)
            persistAsync()
            return Pair(newTourney, UserRole.ADMIN_AUCTIONEER)
        }

        return Pair(null, null)
    }

    fun updateTournament(tournament: Tournament) {
        _tournaments.value = _tournaments.value.map { if (it.id == tournament.id) tournament else it }
        persistAsync()
    }

    fun toggleMasterAdminMode(): Boolean {
        val newState = !_isMasterAdminMode.value
        deviceTourneyManager.setMasterAdminMode(newState)
        _isMasterAdminMode.value = newState
        return newState
    }

    fun setHideDemoData(hide: Boolean) {
        deviceTourneyManager.setHideDemoData(hide)
        _hideDemoData.value = hide
        val activeIsDemo = isDemoOrOffline(_activeTournamentId.value)
        if (hide && activeIsDemo) {
            val realTourney = _tournaments.value.firstOrNull { !isDemoOrOffline(it.id) }
            setActiveTournament(realTourney?.id ?: "")
        }
    }

    fun getTournamentRole(tournamentId: String): String {
        return deviceTourneyManager.getTournamentRole(tournamentId)
    }

    fun removeTeamCaptain(teamId: String) {
        val newInviteCode = "CAP" + UUID.randomUUID().toString().take(4).uppercase()
        _teams.value = _teams.value.map { team ->
            if (team.id == teamId) {
                team.copy(captainEmail = null, inviteCode = newInviteCode)
            } else team
        }
        persistAsync()
    }

    fun regenerateTeamInviteCode(teamId: String): String {
        val newInviteCode = "CAP" + UUID.randomUUID().toString().take(4).uppercase()
        _teams.value = _teams.value.map { team ->
            if (team.id == teamId) {
                team.copy(inviteCode = newInviteCode)
            } else team
        }
        persistAsync()
        return newInviteCode
    }

    fun regenerateScorerCode(tournamentId: String): String {
        val newCode = "SC" + UUID.randomUUID().toString().take(4).uppercase()
        _tournaments.value = _tournaments.value.map { tourney ->
            if (tourney.id == tournamentId) {
                tourney.copy(scorerInviteCode = newCode)
            } else tourney
        }
        persistAsync()
        return newCode
    }

    fun regenerateAdminCode(tournamentId: String): String {
        val newCode = "ADM" + UUID.randomUUID().toString().take(4).uppercase()
        _tournaments.value = _tournaments.value.map { tourney ->
            if (tourney.id == tournamentId) {
                tourney.copy(adminCode = newCode)
            } else tourney
        }
        persistAsync()
        return newCode
    }

    fun regenerateSpectatorCode(tournamentId: String): String {
        val newCode = "SPEC" + UUID.randomUUID().toString().take(4).uppercase()
        _tournaments.value = _tournaments.value.map { tourney ->
            if (tourney.id == tournamentId) {
                tourney.copy(spectatorInviteCode = newCode)
            } else tourney
        }
        persistAsync()
        return newCode
    }

    fun deleteTournament(tournamentId: String) {
        val isDemo = isDemoOrOffline(tournamentId)
        val tourneyMatchIds = _matches.value.filter { it.tournamentId == tournamentId }.map { it.id }.toSet()
        val tourneyPlayerIds = _players.value.filter { it.tournamentId == tournamentId }.map { it.id }.toSet()
        _tournaments.value = _tournaments.value.filterNot { it.id == tournamentId }
        _teams.value = _teams.value.filterNot { it.tournamentId == tournamentId }
        _players.value = _players.value.filterNot { it.tournamentId == tournamentId }
        _matches.value = _matches.value.filterNot { it.tournamentId == tournamentId }
        _ballRecords.value = _ballRecords.value.filterNot { it.matchId in tourneyMatchIds }
        _bids.value = _bids.value.filterNot { it.playerId in tourneyPlayerIds }
        deviceTourneyManager.removeTournament(tournamentId)
        _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
        if (_activeTournamentId.value == tournamentId) {
            _activeTournamentId.value = userTournaments.value.firstOrNull()?.id ?: ""
        }
        if (!isDemo) {
            firebaseRepo.deleteTournament(tournamentId)
        }
        persistAsync()
    }

    // --- Team Management ---
    fun createTeam(
        name: String,
        shortCode: String,
        primaryColorHex: String,
        totalPurse: Double,
        captainEmail: String? = null,
        logoUrl: String = ""
    ): Team {
        val inviteCode = UUID.randomUUID().toString().take(6).uppercase()
        val newTeam = Team(
            id = "team_${System.currentTimeMillis()}",
            tournamentId = _activeTournamentId.value,
            name = name,
            shortCode = shortCode,
            primaryColorHex = primaryColorHex,
            logoUrl = logoUrl,
            totalPurse = totalPurse,
            spentPurse = 0.0,
            captainEmail = captainEmail,
            inviteCode = inviteCode
        )
        _teams.value = _teams.value + newTeam
        persistAsync()
        return newTeam
    }

    fun updateTeam(team: Team) {
        _teams.value = _teams.value.map { if (it.id == team.id) team else it }
        persistAsync()
    }

    fun deleteTeam(teamId: String) {
        _teams.value = _teams.value.filterNot { it.id == teamId }
        persistAsync()
    }

    // --- Player Management ---
    fun setActivePlayer(playerId: String?) {
        _activePlayerId.value = playerId
        persistAsync()
    }

    fun addPlayer(player: Player) {
        _players.value = _players.value + player
        if (_activePlayerId.value == null) {
            _activePlayerId.value = player.id
        }
        persistAsync()
    }

    fun updatePlayer(player: Player) {
        _players.value = _players.value.map { if (it.id == player.id) player else it }
        persistAsync()
    }

    fun deletePlayer(playerId: String) {
        val player = _players.value.find { it.id == playerId }
        if (player != null && player.status == PlayerStatus.SOLD && player.soldToTeamId != null) {
            val refund = player.soldPrice ?: player.currentBid
            _teams.value = _teams.value.map { t ->
                if (t.id == player.soldToTeamId) {
                    t.copy(spentPurse = (t.spentPurse - refund).coerceAtLeast(0.0))
                } else t
            }
        }
        _bids.value = _bids.value.filterNot { it.playerId == playerId }
        _players.value = _players.value.filterNot { it.id == playerId }
        if (_activePlayerId.value == playerId) {
            _activePlayerId.value = _players.value.firstOrNull { it.status == PlayerStatus.UP_NEXT }?.id
        }
        persistAsync()
    }

    fun updatePlayerPhoto(playerId: String, photoUrl: String) {
        _players.value = _players.value.map {
            if (it.id == playerId) it.copy(imageUrl = photoUrl) else it
        }
        persistAsync()
    }

    // --- Live Auction Bidding ---
    fun placeBid(playerId: String, teamId: String, newAmount: Double, bidderEmail: String? = null): Boolean {
        val player = _players.value.find { it.id == playerId } ?: return false
        val team = _teams.value.find { it.id == teamId } ?: return false

        if (team.remainingPurse < (newAmount - (player.soldPrice ?: 0.0))) {
            return false
        }

        val bid = Bid(
            id = "bid_${System.currentTimeMillis()}",
            playerId = playerId,
            teamId = teamId,
            teamName = team.name,
            amount = newAmount,
            bidderEmail = bidderEmail
        )

        _bids.value = listOf(bid) + _bids.value

        _players.value = _players.value.map { p ->
            if (p.id == playerId) {
                p.copy(
                    currentBid = newAmount,
                    highestBidderTeamId = teamId,
                    status = PlayerStatus.BIDDING
                )
            } else p
        }

        persistAsync()
        return true
    }

    fun sellPlayer(playerId: String, winnerTeamId: String, finalPrice: Double): Boolean {
        val player = _players.value.find { it.id == playerId } ?: return false
        val team = _teams.value.find { it.id == winnerTeamId } ?: return false

        _teams.value = _teams.value.map { t ->
            if (t.id == winnerTeamId) {
                t.copy(spentPurse = t.spentPurse + finalPrice)
            } else t
        }

        _players.value = _players.value.map { p ->
            if (p.id == playerId) {
                p.copy(
                    status = PlayerStatus.SOLD,
                    soldPrice = finalPrice,
                    soldToTeamId = winnerTeamId,
                    currentBid = finalPrice,
                    highestBidderTeamId = winnerTeamId
                )
            } else p
        }

        val nextPlayer = _players.value.find { it.tournamentId == player.tournamentId && it.status == PlayerStatus.UP_NEXT }
        _activePlayerId.value = nextPlayer?.id

        persistAsync()
        return true
    }

    fun sellPlayerAtBasePrice(playerId: String, winnerTeamId: String): Boolean {
        val player = _players.value.find { it.id == playerId } ?: return false
        return sellPlayer(playerId, winnerTeamId, player.basePrice)
    }

    fun editPlayerSoldPrice(playerId: String, newSoldPrice: Double): Boolean {
        val player = _players.value.find { it.id == playerId } ?: return false
        val winnerTeamId = player.soldToTeamId ?: player.highestBidderTeamId ?: return false
        val oldPrice = player.soldPrice ?: player.currentBid
        val diff = newSoldPrice - oldPrice

        _teams.value = _teams.value.map { t ->
            if (t.id == winnerTeamId) {
                t.copy(spentPurse = (t.spentPurse + diff).coerceAtLeast(0.0))
            } else t
        }

        _players.value = _players.value.map { p ->
            if (p.id == playerId) {
                p.copy(
                    soldPrice = newSoldPrice,
                    currentBid = newSoldPrice,
                    status = PlayerStatus.SOLD
                )
            } else p
        }

        var updated = false
        _bids.value = _bids.value.map { bid ->
            if (!updated && bid.playerId == playerId && bid.teamId == winnerTeamId) {
                updated = true
                bid.copy(amount = newSoldPrice)
            } else bid
        }

        persistAsync()
        return true
    }

    fun undoLastBid(playerId: String): Boolean {
        val player = _players.value.find { it.id == playerId } ?: return false
        if (player.status == PlayerStatus.SOLD) return false

        val playerBids = _bids.value.filter { it.playerId == playerId }
        if (playerBids.isEmpty()) return false

        val lastBid = playerBids.first()
        _bids.value = _bids.value.filterNot { it.id == lastBid.id }

        val remainingBids = playerBids.filterNot { it.id == lastBid.id }
        _players.value = _players.value.map { p ->
            if (p.id == playerId) {
                if (remainingBids.isNotEmpty()) {
                    val prevBid = remainingBids.first()
                    p.copy(
                        currentBid = prevBid.amount,
                        highestBidderTeamId = prevBid.teamId,
                        status = PlayerStatus.BIDDING
                    )
                } else {
                    p.copy(
                        currentBid = p.basePrice,
                        highestBidderTeamId = null,
                        status = PlayerStatus.UP_NEXT
                    )
                }
            } else p
        }

        persistAsync()
        return true
    }

    fun removePlayerFromTeamForReAuction(playerId: String): Player? {
        val player = _players.value.find { it.id == playerId } ?: return null
        val buyerTeamId = player.soldToTeamId
        val refundAmount = player.soldPrice ?: player.currentBid
        if (buyerTeamId != null) {
            _teams.value = _teams.value.map { t ->
                if (t.id == buyerTeamId) {
                    t.copy(spentPurse = (t.spentPurse - refundAmount).coerceAtLeast(0.0))
                } else t
            }
        }

        val restoredPlayer = player.copy(
            status = PlayerStatus.UP_NEXT,
            soldPrice = null,
            soldToTeamId = null,
            currentBid = player.basePrice,
            highestBidderTeamId = null
        )
        _players.value = _players.value.map { p ->
            if (p.id == playerId) restoredPlayer else p
        }

        _bids.value = _bids.value.filterNot { it.playerId == playerId }
        _activePlayerId.value = restoredPlayer.id
        persistAsync()
        return restoredPlayer
    }

    fun undoLastSoldPlayer(tournamentId: String? = null): Player? {
        val targetTournamentId = tournamentId ?: _activeTournamentId.value
        val soldPlayers = _players.value.filter {
            it.status == PlayerStatus.SOLD && (targetTournamentId.isBlank() || it.tournamentId == targetTournamentId)
        }
        if (soldPlayers.isEmpty()) return null

        val activeP = _players.value.find { it.id == _activePlayerId.value }
        val playerToUndo = if (activeP != null && activeP.status == PlayerStatus.SOLD) {
            activeP
        } else {
            soldPlayers.maxByOrNull { p ->
                _bids.value.find { it.playerId == p.id }?.timestamp ?: 0L
            } ?: soldPlayers.last()
        }

        return removePlayerFromTeamForReAuction(playerToUndo.id)
    }

    fun togglePlayerFixedSet(playerId: String, isFixed: Boolean) {
        _players.value = _players.value.map { p ->
            if (p.id == playerId) p.copy(isFixedSet = isFixed) else p
        }
        persistAsync()
    }

    fun setMultiplePlayersFixedSet(playerIds: Set<String>, isFixed: Boolean) {
        _players.value = _players.value.map { p ->
            if (p.id in playerIds) p.copy(isFixedSet = isFixed) else p
        }
        persistAsync()
    }

    fun clearAllFixedSets(tournamentId: String) {
        _players.value = _players.value.map { p ->
            if (p.tournamentId == tournamentId) p.copy(isFixedSet = false) else p
        }
        persistAsync()
    }

    fun markUnsold(playerId: String) {
        _players.value = _players.value.map { p ->
            if (p.id == playerId) {
                p.copy(status = PlayerStatus.UNSOLD)
            } else p
        }

        val player = _players.value.find { it.id == playerId }
        val nextPlayer = _players.value.find { it.tournamentId == player?.tournamentId && it.status == PlayerStatus.UP_NEXT }
        _activePlayerId.value = nextPlayer?.id

        persistAsync()
    }

    // --- End Innings Action ---
    fun endInnings(matchId: String) {
        val match = _matches.value.find { it.id == matchId } ?: return
        if (match.currentInnings == 1) {
            val updated = match.copy(
                currentInnings = 2,
                status = MatchStatus.LIVE_INNINGS_2
            )
            _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
        } else if (match.currentInnings == 2) {
            val winnerId = if (match.teamBRuns > match.teamARuns) {
                match.teamBId
            } else if (match.teamARuns > match.teamBRuns) {
                match.teamAId
            } else {
                "TIE"
            }
            val summary = if (winnerId == "TIE") {
                "Match Tied"
            } else {
                val winTeam = _teams.value.find { it.id == winnerId }?.name ?: "Winner"
                if (winnerId == match.teamBId) {
                    val soldBatting = _players.value.filter { it.soldToTeamId == match.teamBId }
                    val squadCount = if (soldBatting.isNotEmpty()) soldBatting.size else 11
                    val allOutWkts = if (match.isLastPlayerBattingAllowed) squadCount else (squadCount - 1).coerceAtLeast(1)
                    val wktsLeft = (allOutWkts - match.teamBWickets).coerceAtLeast(1)
                    "$winTeam won by $wktsLeft wickets"
                } else {
                    val margin = match.teamARuns - match.teamBRuns
                    "$winTeam won by $margin runs"
                }
            }
            val updated = match.copy(
                status = MatchStatus.COMPLETED,
                winnerTeamId = winnerId,
                resultSummary = summary
            )
            _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
        }
        persistAsync()
    }

    fun undoEndInnings(matchId: String): BallRecord? {
        val match = _matches.value.find { it.id == matchId } ?: return null
        val matchBalls = _ballRecords.value.filter { it.matchId == matchId }
        val lastBall = matchBalls.lastOrNull()
        if (lastBall != null) {
            // Undo the last ball that concluded the innings or match to revert wickets/overs below threshold
            return undoLastBall(matchId)
        } else {
            val updated = match.copy(
                currentInnings = 1,
                status = MatchStatus.LIVE_INNINGS_1,
                winnerTeamId = null,
                resultSummary = ""
            )
            _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
            persistAsync()
            return null
        }
    }

    fun resumeMatch(matchId: String) {
        val match = _matches.value.find { it.id == matchId } ?: return
        val matchBalls = _ballRecords.value.filter { it.matchId == matchId }
        val hasInn2 = matchBalls.any { it.innings == 2 } || match.teamBOversBatted > 0.0 || match.teamBRuns > 0 || match.currentInnings == 2
        val updated = match.copy(
            currentInnings = if (hasInn2) 2 else 1,
            status = if (hasInn2) MatchStatus.LIVE_INNINGS_2 else MatchStatus.LIVE_INNINGS_1,
            winnerTeamId = null,
            resultSummary = ""
        )
        _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
        _activeMatchId.value = matchId
        persistAsync()
    }

    fun updateMatchLiveStreamUrl(matchId: String, liveStreamUrl: String) {
        _matches.value = _matches.value.map { m ->
            if (m.id == matchId) m.copy(liveStreamUrl = liveStreamUrl.trim()) else m
        }
        persistAsync()
    }

    // --- Captain Invites ---
    fun generateCaptainInvite(teamId: String): CaptainInvite {
        val code = UUID.randomUUID().toString().take(6).uppercase()
        val invite = CaptainInvite(code = code, teamId = teamId)
        _captainInvites.value = _captainInvites.value + invite
        persistAsync()
        return invite
    }

    fun claimCaptainInvite(code: String, email: String): Team? {
        val cleanCode = code.trim().uppercase()
        val targetTeam = _teams.value.find { it.inviteCode.equals(cleanCode, ignoreCase = true) }
        if (targetTeam != null) {
            _teams.value = _teams.value.map {
                if (it.id == targetTeam.id) it.copy(captainEmail = email) else it
            }
            _activeTournamentId.value = targetTeam.tournamentId
            deviceTourneyManager.addTournament(targetTeam.tournamentId, "CAPTAIN")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(targetTeam.tournamentId, "CAPTAIN", "")
            persistAsync()
            return targetTeam
        }

        val invite = _captainInvites.value.find { it.code.equals(cleanCode, ignoreCase = true) && !it.isClaimed }
            ?: return null

        _captainInvites.value = _captainInvites.value.map {
            if (it.code == invite.code) it.copy(isClaimed = true, invitedEmail = email) else it
        }

        _teams.value = _teams.value.map {
            if (it.id == invite.teamId) it.copy(captainEmail = email) else it
        }

        val team = _teams.value.find { it.id == invite.teamId }
        if (team != null) {
            _activeTournamentId.value = team.tournamentId
            deviceTourneyManager.addTournament(team.tournamentId, "CAPTAIN")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(team.tournamentId, "CAPTAIN", "")
        }

        persistAsync()
        return team
    }

    fun claimScorerInvite(code: String): Tournament? {
        val cleanCode = code.trim().uppercase()
        var targetTourney = _tournaments.value.find { it.scorerInviteCode.equals(cleanCode, ignoreCase = true) }

        if (targetTourney == null && cleanCode.startsWith("SC") && cleanCode.length == 6) {
            targetTourney = _tournaments.value.find { it.id == _activeTournamentId.value } ?: _tournaments.value.firstOrNull()
        }

        if (targetTourney != null) {
            _activeTournamentId.value = targetTourney.id
            deviceTourneyManager.addTournament(targetTourney.id, "SCORER")
            _myTournamentIds.value = deviceTourneyManager.getMyTournamentIds()
            firebaseRepo.registerDeviceTournament(targetTourney.id, "SCORER", targetTourney.name)
            persistAsync()
            return targetTourney
        }
        return null
    }

    fun endMatchDirectly(
        matchId: String,
        winnerTeamId: String,
        resultSummary: String,
        teamARuns: Int,
        teamAOvers: Double,
        teamBRuns: Int,
        teamBOvers: Double
    ): Match? {
        val match = _matches.value.find { it.id == matchId } ?: return null
        val updatedMatch = match.copy(
            status = MatchStatus.COMPLETED,
            winnerTeamId = winnerTeamId,
            resultSummary = resultSummary,
            teamARuns = teamARuns,
            teamAOversBatted = teamAOvers,
            teamBRuns = teamBRuns,
            teamBOversBatted = teamBOvers
        )
        _matches.value = _matches.value.map { if (it.id == matchId) updatedMatch else it }
        persistAsync()
        return updatedMatch
    }

    // --- Match & Live Scorer Management ---
    fun createMatch(
        teamAId: String,
        teamBId: String,
        totalOvers: Int,
        isLastPlayerBattingAllowed: Boolean,
        tossWinnerTeamId: String? = null,
        tossDecision: String? = null
    ): Match {
        val newMatch = Match(
            id = "m_${System.currentTimeMillis()}",
            tournamentId = _activeTournamentId.value,
            teamAId = teamAId,
            teamBId = teamBId,
            totalOvers = totalOvers,
            isLastPlayerBattingAllowed = isLastPlayerBattingAllowed,
            tossWinnerTeamId = tossWinnerTeamId,
            tossDecision = tossDecision,
            status = MatchStatus.SCHEDULED,
            currentInnings = 1
        )
        _matches.value = _matches.value + newMatch
        _activeMatchId.value = newMatch.id
        persistAsync()
        return newMatch
    }

    fun setActiveMatch(matchId: String) {
        _activeMatchId.value = matchId
        persistAsync()
    }

    fun updateMatch(match: Match) {
        _matches.value = _matches.value.map { if (it.id == match.id) match else it }
        persistAsync()
    }

    fun addBallRecord(ball: BallRecord) {
        _ballRecords.value = _ballRecords.value + ball

        // Update active match scores
        val match = _matches.value.find { it.id == ball.matchId }
        if (match != null) {
            val totalBallRuns = ball.runsScored + ball.extraRuns
            val isLegalBall = ball.extraType != ExtraType.WIDE && ball.extraType != ExtraType.NO_BALL

            var updatedMatch = match.copy(status = if (match.currentInnings == 1) MatchStatus.LIVE_INNINGS_1 else MatchStatus.LIVE_INNINGS_2)

            if (match.currentInnings == 1) {
                val newRuns = match.teamARuns + totalBallRuns
                val newWickets = if (ball.isWicket) match.teamAWickets + 1 else match.teamAWickets
                val newOvers = incrementOvers(match.teamAOversBatted, isLegalBall)

                // Calculate all-out threshold for team A
                val soldBatting = _players.value.filter { it.soldToTeamId == match.teamAId }
                val squadCount = if (soldBatting.isNotEmpty()) soldBatting.size else {
                    val tourneyPlayers = _players.value.filter { it.tournamentId == match.tournamentId }
                    if (tourneyPlayers.isNotEmpty()) (tourneyPlayers.size / 2).coerceAtLeast(2) else 11
                }
                val allOutWickets = if (match.isLastPlayerBattingAllowed) squadCount else (squadCount - 1).coerceAtLeast(1)

                var currentInnings = 1
                var status = MatchStatus.LIVE_INNINGS_1
                if (newOvers >= match.totalOvers || newWickets >= allOutWickets) {
                    currentInnings = 2
                    status = MatchStatus.LIVE_INNINGS_2
                }

                updatedMatch = updatedMatch.copy(
                    teamARuns = newRuns,
                    teamAWickets = newWickets,
                    teamAOversBatted = newOvers,
                    currentInnings = currentInnings,
                    status = status
                )
            } else {
                val newRuns = match.teamBRuns + totalBallRuns
                val newWickets = if (ball.isWicket) match.teamBWickets + 1 else match.teamBWickets
                val newOvers = incrementOvers(match.teamBOversBatted, isLegalBall)

                // Calculate all-out threshold for team B
                val soldBatting = _players.value.filter { it.soldToTeamId == match.teamBId }
                val squadCount = if (soldBatting.isNotEmpty()) soldBatting.size else {
                    val tourneyPlayers = _players.value.filter { it.tournamentId == match.tournamentId }
                    if (tourneyPlayers.isNotEmpty()) (tourneyPlayers.size / 2).coerceAtLeast(2) else 11
                }
                val allOutWickets = if (match.isLastPlayerBattingAllowed) squadCount else (squadCount - 1).coerceAtLeast(1)

                var winnerId = match.winnerTeamId
                var status = updatedMatch.status
                var resultSummary = match.resultSummary

                // Check Target Chased or All Out / Overs Finish
                val target = match.teamARuns + 1
                if (newRuns >= target) {
                    winnerId = match.teamBId
                    status = MatchStatus.COMPLETED
                    val teamB = _teams.value.find { it.id == match.teamBId }?.name ?: "Team B"
                    val wktsLeft = (allOutWickets - newWickets).coerceAtLeast(1)
                    resultSummary = "$teamB won by $wktsLeft wickets"
                } else if (newOvers >= match.totalOvers || newWickets >= allOutWickets) {
                    if (newRuns > match.teamARuns) {
                        winnerId = match.teamBId
                        val teamB = _teams.value.find { it.id == match.teamBId }?.name ?: "Team B"
                        val wktsLeft = (allOutWickets - newWickets).coerceAtLeast(1)
                        resultSummary = "$teamB won by $wktsLeft wickets"
                    } else if (match.teamARuns > newRuns) {
                        winnerId = match.teamAId
                        val teamA = _teams.value.find { it.id == match.teamAId }?.name ?: "Team A"
                        val runMargin = match.teamARuns - newRuns
                        resultSummary = "$teamA won by $runMargin runs"
                    } else {
                        winnerId = "TIE"
                        resultSummary = "Match Tied"
                    }
                    status = MatchStatus.COMPLETED
                }

                updatedMatch = updatedMatch.copy(
                    teamBRuns = newRuns,
                    teamBWickets = newWickets,
                    teamBOversBatted = newOvers,
                    winnerTeamId = winnerId,
                    status = status,
                    resultSummary = resultSummary
                )
            }

            _matches.value = _matches.value.map { if (it.id == updatedMatch.id) updatedMatch else it }
        }

        persistAsync()
    }

    fun undoLastBall(matchId: String): BallRecord? {
        val lastBall = _ballRecords.value.filter { it.matchId == matchId }.lastOrNull() ?: return null
        _ballRecords.value = _ballRecords.value.filterNot { it.id == lastBall.id }

        // Recalculate match statistics from remaining ball records
        val match = _matches.value.find { it.id == matchId } ?: return lastBall
        val matchBalls = _ballRecords.value.filter { it.matchId == matchId }

        val inn1Balls = matchBalls.filter { it.innings == 1 }
        val inn2Balls = matchBalls.filter { it.innings == 2 }

        val teamARuns = inn1Balls.sumOf { it.runsScored + it.extraRuns }
        val teamAWkts = inn1Balls.count { it.isWicket }
        val teamAOvers = calculateOversFromBalls(inn1Balls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL })

        val teamBRuns = inn2Balls.sumOf { it.runsScored + it.extraRuns }
        val teamBWkts = inn2Balls.count { it.isWicket }
        val teamBOvers = calculateOversFromBalls(inn2Balls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL })

        val updated = match.copy(
            teamARuns = teamARuns,
            teamAWickets = teamAWkts,
            teamAOversBatted = teamAOvers,
            teamBRuns = teamBRuns,
            teamBWickets = teamBWkts,
            teamBOversBatted = teamBOvers,
            currentInnings = if (inn2Balls.isNotEmpty()) 2 else 1,
            status = if (inn2Balls.isNotEmpty()) MatchStatus.LIVE_INNINGS_2 else MatchStatus.LIVE_INNINGS_1,
            winnerTeamId = null,
            resultSummary = ""
        )

        _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
        persistAsync()
        return lastBall
    }

    private fun incrementOvers(currentOvers: Double, isLegalBall: Boolean): Double {
        if (!isLegalBall) return currentOvers
        val wholeOvers = currentOvers.toInt()
        val balls = ((currentOvers - wholeOvers) * 10).toInt() + 1
        return if (balls >= 6) (wholeOvers + 1).toDouble() else (wholeOvers + balls / 10.0)
    }

    private fun calculateOversFromBalls(legalBalls: Int): Double {
        val overs = legalBalls / 6
        val remainder = legalBalls % 6
        return overs + (remainder / 10.0)
    }

    // --- Points Table Calculation ---
    fun getPointsTable(tournamentId: String): List<PointsTableEntry> {
        val tourneyTeams = _teams.value.filter { it.tournamentId == tournamentId }
        val completedMatches = _matches.value.filter { it.tournamentId == tournamentId && it.status == MatchStatus.COMPLETED }

        val entries = tourneyTeams.map { team ->
            var played = 0
            var won = 0
            var lost = 0
            var tied = 0
            var points = 0
            var runsScored = 0
            var oversBatted = 0.0
            var runsConceded = 0
            var oversBowled = 0.0

            completedMatches.forEach { match ->
                if (match.teamAId == team.id) {
                    played++
                    runsScored += match.teamARuns
                    oversBatted += match.teamAOversBatted
                    runsConceded += match.teamBRuns
                    oversBowled += match.teamBOversBatted

                    if (match.winnerTeamId == team.id) {
                        won++; points += 2
                    } else if (match.winnerTeamId == "TIE") {
                        tied++; points += 1
                    } else {
                        lost++
                    }
                } else if (match.teamBId == team.id) {
                    played++
                    runsScored += match.teamBRuns
                    oversBatted += match.teamBOversBatted
                    runsConceded += match.teamARuns
                    oversBowled += match.teamAOversBatted

                    if (match.winnerTeamId == team.id) {
                        won++; points += 2
                    } else if (match.winnerTeamId == "TIE") {
                        tied++; points += 1
                    } else {
                        lost++
                    }
                }
            }

            PointsTableEntry(
                teamId = team.id,
                teamName = team.name,
                shortCode = team.shortCode,
                primaryColorHex = team.primaryColorHex,
                logoUrl = team.logoUrl,
                matchesPlayed = played,
                won = won,
                lost = lost,
                tied = tied,
                points = points,
                runsScored = runsScored,
                oversBatted = oversBatted,
                runsConceded = runsConceded,
                oversBowled = oversBowled
            )
        }

        return entries.sortedWith(compareByDescending<PointsTableEntry> { it.points }.thenByDescending { it.netRunRate })
    }

    // --- Tournament Player Career & CricHeroes MVP Stats ---
    fun getPlayerCareerStats(tournamentId: String? = null): List<PlayerCareerStats> {
        val targetTournamentId = tournamentId ?: _activeTournamentId.value
        val allPlayers = if (!targetTournamentId.isNullOrBlank()) {
            _players.value.filter { it.tournamentId == targetTournamentId }
        } else {
            _players.value
        }
        val tourneyMatchIds = if (!targetTournamentId.isNullOrBlank()) {
            _matches.value.filter { it.tournamentId == targetTournamentId }.map { it.id }.toSet()
        } else {
            emptySet()
        }
        val allBalls = if (tourneyMatchIds.isNotEmpty()) {
            _ballRecords.value.filter { it.matchId in tourneyMatchIds }
        } else if (!targetTournamentId.isNullOrBlank()) {
            emptyList()
        } else {
            _ballRecords.value
        }

        val statsMap = mutableMapOf<String, PlayerCareerStats>()

        allPlayers.forEach { player ->
            val playerBalls = allBalls.filter { it.strikerId == player.id || it.bowlerId == player.id || it.fielderId == player.id }

            var matches = playerBalls.map { it.matchId }.distinct().size
            var runs = 0
            var ballsFaced = 0
            var fours = 0
            var sixes = 0
            var wickets = 0
            var bowledLbw = 0
            var dots = 0
            var maidens = 0
            var oversBowled = 0.0
            var runsConceded = 0
            var catches = 0
            var stumpings = 0
            var runOuts = 0

            // Batting stats
            val battingBalls = allBalls.filter { it.strikerId == player.id }
            ballsFaced = battingBalls.count { it.extraType != ExtraType.WIDE }
            runs = battingBalls.sumOf { it.runsScored }
            fours = battingBalls.count { it.runsScored == 4 }
            sixes = battingBalls.count { it.runsScored == 6 }

            // Bowling stats
            val bowlingBalls = allBalls.filter { it.bowlerId == player.id }
            wickets = bowlingBalls.count { it.isWicket && it.dismissalType != DismissalType.RUN_OUT }
            bowledLbw = bowlingBalls.count { it.isWicket && (it.dismissalType == DismissalType.BOWLED || it.dismissalType == DismissalType.LBW) }
            dots = bowlingBalls.count { it.runsScored == 0 && it.extraRuns == 0 }
            runsConceded = bowlingBalls.sumOf { it.runsScored + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.extraRuns else 0) }

            val legalBowledBalls = bowlingBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
            oversBowled = calculateOversFromBalls(legalBowledBalls)

            // Fielding stats
            catches = allBalls.count { it.fielderId == player.id && it.dismissalType == DismissalType.CAUGHT }
            stumpings = allBalls.count { it.fielderId == player.id && it.dismissalType == DismissalType.STUMPED }
            runOuts = allBalls.count { it.fielderId == player.id && it.dismissalType == DismissalType.RUN_OUT }

            val mvp = calculateCricHeroesMvpPoints(
                runs = runs,
                fours = fours,
                sixes = sixes,
                ballsFaced = ballsFaced,
                wickets = wickets,
                bowledOrLbwCount = bowledLbw,
                dotBalls = dots,
                maidens = maidens,
                oversBowled = oversBowled,
                runsConceded = runsConceded,
                catches = catches,
                stumpings = stumpings,
                runOuts = runOuts
            )

            statsMap[player.id] = PlayerCareerStats(
                playerId = player.id,
                playerName = player.name,
                playerRole = player.role,
                photoUrl = player.imageUrl,
                matches = matches,
                inningsBatted = if (ballsFaced > 0) 1 else 0,
                runsScored = runs,
                ballsFaced = ballsFaced,
                highestScore = runs,
                fours = fours,
                sixes = sixes,
                fifties = if (runs in 50..99) 1 else 0,
                hundreds = if (runs >= 100) 1 else 0,
                oversBowled = oversBowled,
                maidens = maidens,
                runsConceded = runsConceded,
                wicketsTaken = wickets,
                bestBowlingWickets = wickets,
                bestBowlingRuns = runsConceded,
                catches = catches,
                stumpings = stumpings,
                runOuts = runOuts,
                mvpPoints = mvp
            )
        }

        return statsMap.values.sortedByDescending { it.mvpPoints }
    }
}

package cricket.player.auction.data.remote

import cricket.player.auction.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GoogleSheetsRepository {

    private val client = OkHttpClient()
    private val driveRepository = GoogleDriveRepository()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _activeTournamentId = MutableStateFlow<String>("t1")
    val activeTournamentId: StateFlow<String> = _activeTournamentId.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _bids = MutableStateFlow<List<Bid>>(emptyList())
    val bids: StateFlow<List<Bid>> = _bids.asStateFlow()

    private val _captainInvites = MutableStateFlow<List<CaptainInvite>>(emptyList())
    val captainInvites: StateFlow<List<CaptainInvite>> = _captainInvites.asStateFlow()

    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

    private val _sheetIdOrUrl = MutableStateFlow("")
    val sheetIdOrUrl: StateFlow<String> = _sheetIdOrUrl.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    init {
        loadInitialTournamentData()
        startLivePollingTicker()
    }

    fun setGoogleSheetUrl(urlOrId: String) {
        _sheetIdOrUrl.value = urlOrId
        if (urlOrId.isNotBlank()) {
            fetchFromGoogleCloud()
        }
    }

    private fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("/d/")) {
            return trimmed.substringAfter("/d/").substringBefore("/")
        }
        return trimmed
    }

    private fun startLivePollingTicker() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(3000)
                if (_sheetIdOrUrl.value.isNotBlank()) {
                    fetchFromGoogleCloud()
                }
            }
        }
    }

    fun fetchFromGoogleCloud(accessToken: String? = null) {
        val input = _sheetIdOrUrl.value
        if (input.isBlank()) return

        val sheetId = extractSpreadsheetId(input)

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            try {
                val requestUrl = if (input.startsWith("http") && !input.contains("docs.google.com")) {
                    input
                } else {
                    "https://sheets.googleapis.com/v4/spreadsheets/$sheetId?includeGridData=true"
                }

                val requestBuilder = Request.Builder().url(requestUrl)
                if (!accessToken.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $accessToken")
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            parseCloudResponse(body)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
                _lastSyncTimestamp.value = System.currentTimeMillis()
            }
        }
    }

    private fun parseCloudResponse(jsonStr: String) {
        try {
            val jsonObj = JSONObject(jsonStr)
            if (jsonObj.has("players")) {
                val pArray = jsonObj.getJSONArray("players")
                val parsedPlayers = mutableListOf<Player>()
                for (i in 0 until pArray.length()) {
                    val item = pArray.getJSONObject(i)
                    parsedPlayers.add(
                        Player(
                            id = item.getString("id"),
                            tournamentId = item.optString("tournamentId", _activeTournamentId.value),
                            name = item.getString("name"),
                            role = try { PlayerRole.valueOf(item.optString("role", "BATSMAN")) } catch (e: Exception) { PlayerRole.BATSMAN },
                            country = item.optString("country", "India"),
                            isOverseas = item.optBoolean("isOverseas", false),
                            basePrice = item.optDouble("basePrice", 2.0),
                            currentBid = item.optDouble("currentBid", 2.0),
                            highestBidderTeamId = item.optString("highestBidderTeamId", "").takeIf { it.isNotBlank() },
                            status = try { PlayerStatus.valueOf(item.optString("status", "UP_NEXT")) } catch (e: Exception) { PlayerStatus.UP_NEXT },
                            soldPrice = if (item.has("soldPrice") && !item.isNull("soldPrice")) item.optDouble("soldPrice") else null,
                            soldToTeamId = item.optString("soldToTeamId", "").takeIf { it.isNotBlank() },
                            imageUrl = item.optString("imageUrl", ""),
                            stats = item.optString("stats", ""),
                            setName = item.optString("setName", "Set 1")
                        )
                    )
                }
                if (parsedPlayers.isNotEmpty()) {
                    _players.value = parsedPlayers
                }
            }

            if (jsonObj.has("teams")) {
                val tArray = jsonObj.getJSONArray("teams")
                val parsedTeams = mutableListOf<Team>()
                for (i in 0 until tArray.length()) {
                    val item = tArray.getJSONObject(i)
                    parsedTeams.add(
                        Team(
                            id = item.getString("id"),
                            tournamentId = item.optString("tournamentId", _activeTournamentId.value),
                            name = item.getString("name"),
                            shortCode = item.optString("shortCode", "TEAM"),
                            primaryColorHex = item.optString("primaryColorHex", "#FFD700"),
                            logoUrl = item.optString("logoUrl", ""),
                            totalPurse = item.optDouble("totalPurse", 100.0),
                            spentPurse = item.optDouble("spentPurse", 0.0),
                            captainEmail = item.optString("captainEmail", "").takeIf { it.isNotBlank() },
                            inviteCode = item.optString("inviteCode", "INVITE2026")
                        )
                    )
                }
                if (parsedTeams.isNotEmpty()) {
                    _teams.value = parsedTeams
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerAutoCloudSync(accessToken: String? = null) {
        val input = _sheetIdOrUrl.value
        if (input.isBlank()) return

        val sheetId = extractSpreadsheetId(input)
        val tourney = _tournaments.value.find { it.id == _activeTournamentId.value } ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val csvContent = driveRepository.generateGoogleSheetsCsv(tourney, _teams.value, _players.value)

                if (!accessToken.isNullOrBlank() && sheetId.isNotBlank()) {
                    val mediaUrl = "https://www.googleapis.com/upload/drive/v3/files/$sheetId?uploadType=media"
                    val request = Request.Builder()
                        .url(mediaUrl)
                        .addHeader("Authorization", "Bearer $accessToken")
                        .addHeader("Content-Type", "text/csv")
                        .put(csvContent.toRequestBody("text/csv".toMediaType()))
                        .build()

                    client.newCall(request).execute().close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Tournament Management ---

    fun createTournament(name: String, defaultPurse: Double, maxSlots: Int, maxOverseas: Int, accessToken: String? = null): Tournament {
        val tId = "t_${System.currentTimeMillis()}"
        val driveLink = "https://drive.google.com/drive/folders/tournament_$tId"
        val newTournament = Tournament(
            id = tId,
            name = name,
            defaultPurse = defaultPurse,
            maxSlots = maxSlots,
            maxOverseas = maxOverseas,
            driveFileUrl = driveLink
        )
        val current = _tournaments.value.toMutableList()
        current.add(0, newTournament)
        _tournaments.value = current
        _activeTournamentId.value = tId
        triggerAutoCloudSync(accessToken)
        return newTournament
    }

    fun setActiveTournament(tournamentId: String) {
        _activeTournamentId.value = tournamentId
        val tPlayers = _players.value.filter { it.tournamentId == tournamentId }
        _activePlayerId.value = tPlayers.firstOrNull { it.status == PlayerStatus.UP_NEXT }?.id
    }

    // --- Custom Team Management ---

    fun createTeam(
        name: String,
        shortCode: String,
        primaryColorHex: String,
        totalPurse: Double,
        captainEmail: String? = null,
        logoUrl: String = "",
        accessToken: String? = null
    ): Team {
        val tId = "team_${System.currentTimeMillis()}"
        val inviteCode = "${shortCode.uppercase()}2026"
        val newTeam = Team(
            id = tId,
            tournamentId = _activeTournamentId.value,
            name = name,
            shortCode = shortCode.uppercase(),
            primaryColorHex = primaryColorHex,
            logoUrl = logoUrl,
            totalPurse = totalPurse,
            spentPurse = 0.0,
            captainEmail = captainEmail,
            inviteCode = inviteCode
        )
        val current = _teams.value.toMutableList()
        current.add(newTeam)
        _teams.value = current
        triggerAutoCloudSync(accessToken)
        return newTeam
    }

    // --- Auction Operations & Bidding ---

    fun setActivePlayer(playerId: String?) {
        _activePlayerId.value = playerId
    }

    fun placeBid(playerId: String, teamId: String, newAmount: Double, bidderEmail: String? = null, accessToken: String? = null): Boolean {
        val currentPlayers = _players.value.toMutableList()
        val pIndex = currentPlayers.indexOfFirst { it.id == playerId }
        if (pIndex == -1) return false

        val currentTeams = _teams.value
        val team = currentTeams.find { it.id == teamId } ?: return false

        if (team.remainingPurse < newAmount) return false

        val player = currentPlayers[pIndex]
        val updatedPlayer = player.copy(
            currentBid = newAmount,
            highestBidderTeamId = teamId,
            status = PlayerStatus.BIDDING
        )
        currentPlayers[pIndex] = updatedPlayer
        _players.value = currentPlayers

        val newBid = Bid(
            id = "bid_${System.currentTimeMillis()}",
            playerId = playerId,
            teamId = teamId,
            teamName = team.name,
            amount = newAmount,
            bidderEmail = bidderEmail
        )
        val currentBids = _bids.value.toMutableList()
        currentBids.add(0, newBid)
        _bids.value = currentBids

        triggerAutoCloudSync(accessToken)
        return true
    }

    fun sellPlayer(playerId: String, winningTeamId: String, finalPrice: Double, accessToken: String? = null): Boolean {
        val currentPlayers = _players.value.toMutableList()
        val pIndex = currentPlayers.indexOfFirst { it.id == playerId }
        if (pIndex == -1) return false

        val currentTeams = _teams.value.toMutableList()
        val tIndex = currentTeams.indexOfFirst { it.id == winningTeamId }
        if (tIndex == -1) return false

        val team = currentTeams[tIndex]
        val currentSquadCount = currentPlayers.count { it.soldToTeamId == winningTeamId }
        if (currentSquadCount >= team.maxSlots) return false

        val player = currentPlayers[pIndex]
        if (player.isOverseas) {
            val currentOverseasCount = currentPlayers.count { it.soldToTeamId == winningTeamId && it.isOverseas }
            if (currentOverseasCount >= team.maxOverseas) return false
        }

        val updatedTeam = team.copy(spentPurse = team.spentPurse + finalPrice)
        currentTeams[tIndex] = updatedTeam
        _teams.value = currentTeams

        val updatedPlayer = player.copy(
            status = PlayerStatus.SOLD,
            soldPrice = finalPrice,
            soldToTeamId = winningTeamId,
            currentBid = finalPrice,
            highestBidderTeamId = winningTeamId
        )
        currentPlayers[pIndex] = updatedPlayer
        _players.value = currentPlayers

        val nextPlayer = currentPlayers.firstOrNull { it.tournamentId == _activeTournamentId.value && it.status == PlayerStatus.UP_NEXT }
        _activePlayerId.value = nextPlayer?.id
        triggerAutoCloudSync(accessToken)
        return true
    }

    fun markUnsold(playerId: String, accessToken: String? = null) {
        val currentPlayers = _players.value.toMutableList()
        val pIndex = currentPlayers.indexOfFirst { it.id == playerId }
        if (pIndex != -1) {
            currentPlayers[pIndex] = currentPlayers[pIndex].copy(status = PlayerStatus.UNSOLD)
            _players.value = currentPlayers
        }

        val nextPlayer = currentPlayers.firstOrNull { it.tournamentId == _activeTournamentId.value && it.status == PlayerStatus.UP_NEXT }
        _activePlayerId.value = nextPlayer?.id
        triggerAutoCloudSync(accessToken)
    }

    fun addPlayer(player: Player, accessToken: String? = null) {
        val current = _players.value.toMutableList()
        current.add(player)
        _players.value = current
        triggerAutoCloudSync(accessToken)
    }

    fun generateCaptainInvite(teamId: String, invitedEmail: String? = null): CaptainInvite {
        val code = "INV-" + (1000..9999).random()
        val invite = CaptainInvite(code = code, teamId = teamId, invitedEmail = invitedEmail)
        val current = _captainInvites.value.toMutableList()
        current.add(invite)
        _captainInvites.value = current
        return invite
    }

    fun claimCaptainInvite(inviteCode: String, userEmail: String): Team? {
        val invites = _captainInvites.value.toMutableList()
        val inviteIndex = invites.indexOfFirst { it.code.equals(inviteCode, ignoreCase = true) && !it.isClaimed }
        if (inviteIndex == -1) return null

        val invite = invites[inviteIndex]
        invites[inviteIndex] = invite.copy(isClaimed = true, invitedEmail = userEmail)
        _captainInvites.value = invites

        val currentTeams = _teams.value.toMutableList()
        val tIndex = currentTeams.indexOfFirst { it.id == invite.teamId }
        if (tIndex != -1) {
            val updatedTeam = currentTeams[tIndex].copy(captainEmail = userEmail)
            currentTeams[tIndex] = updatedTeam
            _teams.value = currentTeams
            return updatedTeam
        }
        return null
    }

    suspend fun createNewGoogleSheet(sheetTitle: String = "Cricket_Auction_Database", accessToken: String? = null): String {
        return driveRepository.createGoogleSheetInDrive(sheetTitle, accessToken)
    }

    suspend fun exportTournamentToGoogleDrive(context: android.content.Context? = null, accessToken: String? = null): GoogleDriveRepository.ExportResult {
        val currentTourney = _tournaments.value.find { it.id == _activeTournamentId.value }
            ?: Tournament("t1", "Global Champions League T20")
        val currentTeams = _teams.value.filter { it.tournamentId == currentTourney.id }
        val currentPlayers = _players.value.filter { it.tournamentId == currentTourney.id }

        return driveRepository.saveTournamentToUserDrive(context, currentTourney, currentTeams, currentPlayers, accessToken)
    }

    private fun loadInitialTournamentData() {
        val initialTourney = Tournament(
            id = "t1",
            name = "Global Cricket Champions League T20",
            defaultPurse = 100.0,
            maxSlots = 25,
            maxOverseas = 8
        )
        _tournaments.value = listOf(initialTourney)
        _activeTournamentId.value = "t1"
        _teams.value = emptyList()
        _players.value = emptyList()
        _activePlayerId.value = null
    }
}

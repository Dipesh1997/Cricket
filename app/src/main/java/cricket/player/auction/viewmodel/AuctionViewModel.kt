package cricket.player.auction.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cricket.player.auction.data.local.LocalCricketRepository
import cricket.player.auction.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuctionViewModel(application: Application) : AndroidViewModel(application) {

    val localRepository = LocalCricketRepository(application)

    val tournaments: StateFlow<List<Tournament>> = localRepository.tournaments
    val activeTournamentId: StateFlow<String> = localRepository.activeTournamentId

    val activeTournament: StateFlow<Tournament?> = combine(tournaments, activeTournamentId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val teams: StateFlow<List<Team>> = combine(localRepository.teams, activeTournamentId) { list, tourneyId ->
        list.filter { it.tournamentId == tourneyId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val players: StateFlow<List<Player>> = combine(localRepository.players, activeTournamentId) { list, tourneyId ->
        list.filter { it.tournamentId == tourneyId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bids: StateFlow<List<Bid>> = localRepository.bids
    val captainInvites: StateFlow<List<CaptainInvite>> = localRepository.captainInvites
    val activePlayerId: StateFlow<String?> = localRepository.activePlayerId
    val matches: StateFlow<List<Match>> = localRepository.matches
    val activeMatchId: StateFlow<String?> = localRepository.activeMatchId
    val ballRecords: StateFlow<List<BallRecord>> = localRepository.ballRecords

    private val _currentUser = MutableStateFlow(
        UserProfile(
            email = "admin@cricket.app",
            displayName = "Tournament Admin",
            role = UserRole.ADMIN_AUCTIONEER
        )
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
    val isSignedIn: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    // Computed Active Player
    val activePlayer: StateFlow<Player?> = combine(players, activePlayerId) { playerList, id ->
        playerList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Computed Highest Bidder Team
    val highestBidderTeam: StateFlow<Team?> = combine(teams, activePlayer) { teamList, player ->
        teamList.find { it.id == player?.highestBidderTeamId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Computed Active Match
    val activeMatch: StateFlow<Match?> = combine(matches, activeMatchId) { matchlist, id ->
        matchlist.find { it.id == id } ?: matchlist.lastOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Points Table StateFlow
    val pointsTable: StateFlow<List<PointsTableEntry>> = combine(activeTournamentId, matches) { tourneyId, _ ->
        localRepository.getPointsTable(tourneyId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // CricHeroes MVP Leaderboard StateFlow
    val mvpLeaderboard: StateFlow<List<PlayerCareerStats>> = combine(players, ballRecords) { _, _ ->
        localRepository.getPlayerCareerStats()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Theme State
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Bidding Countdown Timer
    private val _timerSeconds = MutableStateFlow(15)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        startBidTimer()
    }

    private fun startBidTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_timerSeconds.value > 0 && activePlayer.value?.status == PlayerStatus.BIDDING) {
                    _timerSeconds.value -= 1
                }
            }
        }
    }

    fun resetBidTimer() {
        _timerSeconds.value = 15
    }

    // --- Tournament Actions ---
    fun createTournament(name: String, defaultPurse: Double, maxSlots: Int, maxOverseas: Int): Tournament {
        return localRepository.createTournament(name, defaultPurse, maxSlots, maxOverseas)
    }

    fun selectTournament(tournamentId: String) {
        localRepository.setActiveTournament(tournamentId)
    }

    fun updateTournament(tournament: Tournament) {
        localRepository.updateTournament(tournament)
    }

    fun deleteTournament(tournamentId: String) {
        localRepository.deleteTournament(tournamentId)
    }

    // --- Team Actions ---
    fun createTeam(
        name: String,
        shortCode: String,
        primaryColorHex: String,
        totalPurse: Double,
        captainEmail: String? = null,
        logoUrl: String = ""
    ): Team {
        return localRepository.createTeam(name, shortCode, primaryColorHex, totalPurse, captainEmail, logoUrl)
    }

    fun updateTeam(team: Team) {
        localRepository.updateTeam(team)
    }

    fun deleteTeam(teamId: String) {
        localRepository.deleteTeam(teamId)
    }

    // --- Player Actions ---
    fun selectActivePlayer(playerId: String) {
        localRepository.setActivePlayer(playerId)
        resetBidTimer()
    }

    fun updatePlayer(player: Player) {
        localRepository.updatePlayer(player)
    }

    fun deletePlayer(playerId: String) {
        localRepository.deletePlayer(playerId)
    }


    fun addNewPlayer(
        name: String,
        role: PlayerRole,
        country: String,
        isOverseas: Boolean,
        basePrice: Double,
        driveImageUrl: String,
        stats: String,
        setName: String
    ) {
        val currentTourneyId = activeTournamentId.value
        val newPlayer = Player(
            id = "p_${System.currentTimeMillis()}",
            tournamentId = currentTourneyId,
            name = name,
            role = role,
            country = country,
            isOverseas = isOverseas,
            basePrice = basePrice,
            currentBid = basePrice,
            imageUrl = driveImageUrl,
            stats = stats,
            setName = setName
        )
        localRepository.addPlayer(newPlayer)
    }

    fun updatePlayerPhoto(playerId: String, photoUrl: String) {
        localRepository.updatePlayerPhoto(playerId, photoUrl)
    }

    // --- Bidding Actions ---
    fun placeBid(teamId: String, increment: Double): Boolean {
        val player = activePlayer.value ?: return false
        val newAmount = player.currentBid + increment
        val userEmail = currentUser.value?.email

        val success = localRepository.placeBid(
            playerId = player.id,
            teamId = teamId,
            newAmount = newAmount,
            bidderEmail = userEmail
        )
        if (success) {
            resetBidTimer()
        }
        return success
    }

    fun sellActivePlayer(): Boolean {
        val player = activePlayer.value ?: return false
        val winnerTeamId = player.highestBidderTeamId ?: return false
        val finalPrice = player.currentBid
        return localRepository.sellPlayer(player.id, winnerTeamId, finalPrice)
    }

    fun sellActivePlayerAtBasePrice(teamId: String): Boolean {
        val player = activePlayer.value ?: return false
        return localRepository.sellPlayerAtBasePrice(player.id, teamId)
    }

    fun togglePlayerFixedSet(playerId: String, isFixed: Boolean) {
        localRepository.togglePlayerFixedSet(playerId, isFixed)
    }

    fun markActivePlayerUnsold() {
        val player = activePlayer.value ?: return
        localRepository.markUnsold(player.id)
    }

    // --- Match & Live Scorer Actions ---
    fun createMatch(
        teamAId: String,
        teamBId: String,
        totalOvers: Int,
        isLastPlayerBattingAllowed: Boolean,
        tossWinnerTeamId: String? = null,
        tossDecision: String? = null
    ): Match {
        return localRepository.createMatch(teamAId, teamBId, totalOvers, isLastPlayerBattingAllowed, tossWinnerTeamId, tossDecision)
    }

    fun selectActiveMatch(matchId: String) {
        localRepository.setActiveMatch(matchId)
    }

    fun addBallRecord(ball: BallRecord) {
        localRepository.addBallRecord(ball)
    }

    fun undoLastBall(matchId: String) {
        localRepository.undoLastBall(matchId)
    }

    fun endActiveInnings() {
        val matchId = activeMatchId.value ?: activeMatch.value?.id ?: return
        localRepository.endInnings(matchId)
    }

    // --- Captain Invite & Role Switch ---
    fun generateCaptainInvite(teamId: String): CaptainInvite {
        return localRepository.generateCaptainInvite(teamId)
    }

    fun claimCaptainInvite(code: String): Team? {
        val email = currentUser.value?.email ?: "captain@tournament.com"
        val team = localRepository.claimCaptainInvite(code, email)
        if (team != null) {
            _currentUser.value = _currentUser.value.copy(role = UserRole.TEAM_CAPTAIN, assignedTeamId = team.id)
        }
        return team
    }

    fun claimScorerInvite(code: String): Tournament? {
        val tourney = localRepository.claimScorerInvite(code)
        if (tourney != null) {
            _currentUser.value = _currentUser.value.copy(role = UserRole.SCORER, assignedTeamId = null)
        }
        return tourney
    }

    fun joinWithCode(inputCode: String): Pair<Boolean, String> {
        val (tourney, role) = localRepository.joinTournamentWithCode(inputCode)
        if (tourney != null && role != null) {
            val teamId = if (role == UserRole.TEAM_CAPTAIN) {
                val cleanCode = inputCode.trim().uppercase()
                teams.value.find { it.inviteCode.equals(cleanCode, ignoreCase = true) }?.id
            } else null

            _currentUser.value = _currentUser.value.copy(
                role = role,
                assignedTeamId = teamId
            )
            val roleName = when (role) {
                UserRole.ADMIN_AUCTIONEER -> "Admin / Auctioneer"
                UserRole.SCORER -> "Live Scorer"
                UserRole.TEAM_CAPTAIN -> "Team Captain / Owner"
                UserRole.SPECTATOR -> "Spectator"
            }
            return Pair(true, "Successfully joined '${tourney.name}' as $roleName!")
        }
        return Pair(false, "Invalid joining code. Please check your admin or invite code.")
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
        return localRepository.endMatchDirectly(matchId, winnerTeamId, resultSummary, teamARuns, teamAOvers, teamBRuns, teamBOvers)
    }

    fun switchUserRole(role: UserRole, teamId: String? = null) {
        _currentUser.value = _currentUser.value.copy(role = role, assignedTeamId = teamId)
    }

    fun exportDatabaseJson(): String {
        return localRepository.exportDatabaseJson()
    }


    fun importDatabaseJson(jsonString: String): Boolean {
        return localRepository.importDatabaseJson(jsonString)
    }

    fun clearAllData() {
        localRepository.clearAllData()
    }
}




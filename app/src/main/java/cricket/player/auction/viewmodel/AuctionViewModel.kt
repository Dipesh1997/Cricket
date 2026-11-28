package cricket.player.auction.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cricket.player.auction.data.local.LocalCricketRepository
import cricket.player.auction.data.remote.GoogleAuthManager
import cricket.player.auction.data.remote.YouTubeBroadcastResult
import cricket.player.auction.data.remote.YouTubeLiveStreamManager
import cricket.player.auction.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuctionViewModel(application: Application) : AndroidViewModel(application) {

    val localRepository = LocalCricketRepository(application)
    val googleAuthManager = GoogleAuthManager(application)

    val isYouTubeConnected: StateFlow<Boolean> = googleAuthManager.isYouTubeConnected
    val connectedGoogleEmail: StateFlow<String?> = googleAuthManager.connectedGoogleEmail

    fun getYouTubeSignInIntent(activityContext: Context): Intent {
        return googleAuthManager.getYouTubeSignInIntent(activityContext)
    }

    fun handleGoogleYouTubeSignInResult(data: Intent?, onComplete: (Boolean, String) -> Unit) {
        googleAuthManager.handleSignInResult(
            data = data,
            onSuccess = { profile ->
                viewModelScope.launch {
                    val token = googleAuthManager.fetchYouTubeAccessToken()
                    if (token != null) {
                        onComplete(true, "Connected as ${profile.email}")
                    } else {
                        onComplete(true, "Signed in as ${profile.email}")
                    }
                }
            },
            onError = { msg -> onComplete(false, msg) }
        )
    }

    fun startDirectYouTubeBroadcast(
        title: String,
        description: String,
        onResult: (YouTubeBroadcastResult) -> Unit
    ) {
        viewModelScope.launch {
            val token = googleAuthManager.fetchYouTubeAccessToken()
            if (token.isNullOrBlank()) {
                onResult(
                    YouTubeBroadcastResult(
                        success = false,
                        errorMessage = "Please tap 'Sign in with Google' first to connect your YouTube channel."
                    )
                )
                return@launch
            }
            val result = YouTubeLiveStreamManager.createAndBindLiveBroadcast(
                accessToken = token,
                title = title,
                description = description
            )
            onResult(result)
        }
    }

    fun endDirectYouTubeBroadcast(broadcastId: String) {
        if (broadcastId.isBlank()) return
        viewModelScope.launch {
            val token = googleAuthManager.fetchYouTubeAccessToken() ?: return@launch
            YouTubeLiveStreamManager.transitionBroadcast(token, broadcastId, "complete")
        }
    }

    fun monitorAndGoLive(
        broadcastId: String,
        streamId: String,
        onStatusUpdate: (isLive: Boolean, statusText: String) -> Unit
    ): Job {
        return viewModelScope.launch {
            val token = googleAuthManager.fetchYouTubeAccessToken() ?: return@launch
            repeat(20) {
                delay(3000L)
                val lifeCycle = YouTubeLiveStreamManager.getBroadcastLifeCycleStatus(token, broadcastId)
                if (lifeCycle.equals("live", ignoreCase = true)) {
                    onStatusUpdate(true, "ON AIR • YOUTUBE LIVE")
                    return@launch
                }
                val streamStatus = YouTubeLiveStreamManager.getStreamStatus(token, streamId)
                if (streamStatus.equals("active", ignoreCase = true)) {
                    val transitioned = YouTubeLiveStreamManager.transitionBroadcast(token, broadcastId, "live")
                    if (transitioned || YouTubeLiveStreamManager.getBroadcastLifeCycleStatus(token, broadcastId).equals("live", ignoreCase = true)) {
                        onStatusUpdate(true, "ON AIR • YOUTUBE LIVE")
                        return@launch
                    } else {
                        onStatusUpdate(false, "RTMP Active • Switching YouTube to LIVE...")
                    }
                } else {
                    onStatusUpdate(false, "RTMP Streaming • Waiting for YouTube (${streamStatus.ifBlank { "connecting" }})...")
                }
            }
        }
    }

    fun signOutGoogleAccount(onComplete: () -> Unit = {}) {
        googleAuthManager.signOutGoogleAndYouTube(onComplete)
    }

    fun updateTournamentAuctionStreamUrl(tournamentId: String, liveStreamUrl: String) {
        localRepository.updateTournamentAuctionStreamUrl(tournamentId, liveStreamUrl)
    }

    // Scoped only to tournaments this device created or joined!
    val tournaments: StateFlow<List<Tournament>> = localRepository.userTournaments
    val allTournaments: StateFlow<List<Tournament>> = localRepository.tournaments
    val isMasterAdminMode: StateFlow<Boolean> = localRepository.isMasterAdminMode
    val activeTournamentId: StateFlow<String> = localRepository.activeTournamentId

    val captainReadyMap: StateFlow<Map<String, Boolean>> = localRepository.captainReadyMap
    val isBiddingStarted: StateFlow<Boolean> = localRepository.isBiddingStarted

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
    val hideDemoData: StateFlow<Boolean> = localRepository.hideDemoData

    fun setHideDemoData(hide: Boolean) {
        localRepository.setHideDemoData(hide)
    }

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
    val mvpLeaderboard: StateFlow<List<PlayerCareerStats>> = combine(players, ballRecords, activeTournamentId) { _, _, tId ->
        localRepository.getPlayerCareerStats(tId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Theme State - Default is Light Theme unless manually changed in preferences
    private val _isDarkTheme = MutableStateFlow(localRepository.deviceTourneyManager.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        localRepository.deviceTourneyManager.setDarkTheme(newTheme)
    }

    // Bidding Countdown Timer
    private val _timerSeconds = MutableStateFlow(15)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    // Joined Captain Teams in Active Tournament
    val joinedCaptainTeams: StateFlow<List<Team>> = combine(teams, captainInvites) { teamList, invites ->
        teamList.filter { team ->
            !team.captainEmail.isNullOrBlank() || invites.any { it.teamId == team.id && it.isClaimed }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Are all joined captains ready? (If no captains joined, true for solo admin mode)
    val areAllCaptainsReady: StateFlow<Boolean> = combine(joinedCaptainTeams, captainReadyMap) { captains, readyMap ->
        if (captains.isEmpty()) true
        else captains.all { readyMap[it.id] == true }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setMyCaptainReady(isReady: Boolean) {
        val myTeamId = _currentUser.value.assignedTeamId ?: return
        localRepository.setCaptainReady(myTeamId, isReady)
    }

    fun startBidding() {
        localRepository.setBiddingStarted(true)
    }

    fun resetBiddingState() {
        localRepository.setBiddingStarted(false)
    }

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

    val lastAutoAssignedMessage: StateFlow<String?> = localRepository.lastAutoAssignedMessage

    fun clearAutoAssignedMessage() {
        localRepository.clearAutoAssignedMessage()
    }

    // --- Tournament Actions ---
    fun createTournament(
        name: String,
        defaultPurse: Double,
        maxSlots: Int,
        maxOverseas: Int,
        autoAssignLastPlayerAtAvg: Boolean = false
    ): Tournament {
        return localRepository.createTournament(name, defaultPurse, maxSlots, maxOverseas, autoAssignLastPlayerAtAvg)
    }

    fun setTournamentAutoAssignRule(tournamentId: String, enabled: Boolean) {
        localRepository.setTournamentAutoAssignRule(tournamentId, enabled)
    }

    fun setEnforceEqualSquadsRule(tournamentId: String, enabled: Boolean) {
        localRepository.setEnforceEqualSquadsRule(tournamentId, enabled)
    }

    fun advanceSpinTurn(tournamentId: String) {
        localRepository.advanceSpinTurn(tournamentId)
    }

    fun ensureCaptainQuizIfNeeded(tournamentId: String) {
        localRepository.ensureCaptainQuizIfNeeded(tournamentId)
    }

    fun submitCaptainQuizResult(tournamentId: String, teamId: String, correctCount: Int, timeTakenMs: Long) {
        localRepository.submitCaptainQuizResult(tournamentId, teamId, correctCount, timeTakenMs)
    }

    fun getSetQuotaPerTeam(tournamentId: String): Int {
        return localRepository.getSetQuotaPerTeam(tournamentId)
    }

    fun getTeamSetPickedCount(tournamentId: String, teamId: String): Int {
        return localRepository.getTeamSetPickedCount(tournamentId, teamId)
    }

    fun createDemoTournament(): Tournament {
        val demo = localRepository.createDemoTournament()
        _currentUser.value = _currentUser.value.copy(role = UserRole.ADMIN_AUCTIONEER)
        return demo
    }

    fun selectTournament(tournamentId: String) {
        localRepository.setActiveTournament(tournamentId)
        if (localRepository.isMasterAdminMode.value) {
            _currentUser.value = _currentUser.value.copy(role = UserRole.ADMIN_AUCTIONEER)
        } else {
            val roleStr = localRepository.getTournamentRole(tournamentId)
            val role = when (roleStr) {
                "ADMIN" -> UserRole.ADMIN_AUCTIONEER
                "SCORER" -> UserRole.SCORER
                "CAPTAIN" -> UserRole.TEAM_CAPTAIN
                "SPECTATOR" -> UserRole.SPECTATOR
                else -> UserRole.ADMIN_AUCTIONEER
            }
            _currentUser.value = _currentUser.value.copy(role = role)
        }
    }

    fun removeTeamCaptain(teamId: String) {
        localRepository.removeTeamCaptain(teamId)
    }

    fun regenerateTeamInviteCode(teamId: String): String {
        return localRepository.regenerateTeamInviteCode(teamId)
    }

    fun regenerateScorerCode(tournamentId: String): String {
        return localRepository.regenerateScorerCode(tournamentId)
    }

    fun regenerateAdminCode(tournamentId: String): String {
        return localRepository.regenerateAdminCode(tournamentId)
    }

    fun regenerateSpectatorCode(tournamentId: String): String {
        return localRepository.regenerateSpectatorCode(tournamentId)
    }

    fun toggleMasterAdminMode(): Boolean {
        val newState = localRepository.toggleMasterAdminMode()
        if (newState) {
            _currentUser.value = _currentUser.value.copy(role = UserRole.ADMIN_AUCTIONEER)
        }
        return newState
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

    fun setMultiplePlayersFixedSet(playerIds: Set<String>, isFixed: Boolean) {
        localRepository.setMultiplePlayersFixedSet(playerIds, isFixed)
    }

    fun clearAllFixedSets(tournamentId: String) {
        localRepository.clearAllFixedSets(tournamentId)
    }

    fun markActivePlayerUnsold() {
        val player = activePlayer.value ?: return
        localRepository.markUnsold(player.id)
    }

    fun editPlayerSoldPrice(playerId: String, newSoldPrice: Double): Boolean {
        return localRepository.editPlayerSoldPrice(playerId, newSoldPrice)
    }

    fun undoLastBid(): Boolean {
        val player = activePlayer.value ?: return false
        val success = localRepository.undoLastBid(player.id)
        if (success) {
            resetBidTimer()
        }
        return success
    }

    fun undoLastSoldPlayer(): Player? {
        val restored = localRepository.undoLastSoldPlayer(activeTournament.value?.id)
        if (restored != null) {
            resetBidTimer()
        }
        return restored
    }

    fun removePlayerFromTeamForReAuction(playerId: String): Player? {
        val restored = localRepository.removePlayerFromTeamForReAuction(playerId)
        if (restored != null) {
            resetBidTimer()
        }
        return restored
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

    fun undoLastBall(matchId: String): BallRecord? {
        return localRepository.undoLastBall(matchId)
    }

    fun endActiveInnings() {
        val matchId = activeMatchId.value ?: activeMatch.value?.id ?: return
        localRepository.endInnings(matchId)
    }

    fun undoEndInnings(): BallRecord? {
        val matchId = activeMatchId.value ?: activeMatch.value?.id ?: return null
        return localRepository.undoEndInnings(matchId)
    }

    fun resumeMatch(matchId: String) {
        localRepository.resumeMatch(matchId)
    }

    data class ScorerLaunchRequest(
        val mode: String = "CLASSIC",
        val openLive: Boolean = true,
        val openYouTubeDialog: Boolean = false
    )

    private val _pendingScorerLaunch = MutableStateFlow<ScorerLaunchRequest?>(null)
    val pendingScorerLaunch: StateFlow<ScorerLaunchRequest?> = _pendingScorerLaunch.asStateFlow()

    fun requestScorerLaunch(mode: String = "CLASSIC", openLive: Boolean = true, openYouTubeDialog: Boolean = false) {
        _pendingScorerLaunch.value = ScorerLaunchRequest(mode, openLive, openYouTubeDialog)
    }

    fun consumeScorerLaunch() {
        _pendingScorerLaunch.value = null
    }

    fun updateMatchLiveStreamUrl(matchId: String, liveStreamUrl: String) {
        localRepository.updateMatchLiveStreamUrl(matchId, liveStreamUrl)
    }

    fun isMatchEditable(match: Match): Boolean {
        val tourneyMatches = matches.value.filter { it.tournamentId == match.tournamentId }
        val matchIndex = tourneyMatches.indexOfFirst { it.id == match.id }
        if (matchIndex == -1) return true
        val subsequentMatches = tourneyMatches.drop(matchIndex + 1)
        // Match is editable if it is the last match before any subsequent match is live or completed
        return subsequentMatches.none { 
            it.status == MatchStatus.COMPLETED || 
            it.status == MatchStatus.LIVE_INNINGS_1 || 
            it.status == MatchStatus.LIVE_INNINGS_2 ||
            it.teamARuns > 0 || it.teamAOversBatted > 0.0 || it.teamAWickets > 0
        }
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

    fun backupToCricketFolder(): Result<java.io.File> {
        val json = localRepository.exportDatabaseJson()
        return cricket.player.auction.data.local.CricketBackupManager.backupToCricketFolder(getApplication(), json)
    }

    fun restoreFromCricketFolder(): Result<Boolean> {
        val result = cricket.player.auction.data.local.CricketBackupManager.restoreFromCricketFolder(getApplication())
        return result.map { jsonString ->
            localRepository.importDatabaseJson(jsonString)
        }
    }

    fun clearAllData() {
        localRepository.clearAllData()
    }
}




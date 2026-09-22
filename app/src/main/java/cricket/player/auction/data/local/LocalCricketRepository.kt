package cricket.player.auction.data.local

import android.content.Context
import cricket.player.auction.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LocalCricketRepository(context: Context) {

    private val dbManager = LocalDatabaseManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

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

    private val _activeTournamentId = MutableStateFlow("t1")
    val activeTournamentId: StateFlow<String> = _activeTournamentId.asStateFlow()

    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

    private val _activeMatchId = MutableStateFlow<String?>(null)
    val activeMatchId: StateFlow<String?> = _activeMatchId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        scope.launch {
            val schema = dbManager.loadDatabase()
            _tournaments.value = schema.tournaments
            _teams.value = schema.teams
            _players.value = schema.players
            _bids.value = schema.bids
            _captainInvites.value = schema.captainInvites
            _matches.value = schema.matches
            _ballRecords.value = schema.ballRecords
            _activeTournamentId.value = schema.activeTournamentId
            _activePlayerId.value = schema.activePlayerId
            _activeMatchId.value = schema.activeMatchId
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
        val newTourney = Tournament(
            id = "t_${System.currentTimeMillis()}",
            name = name,
            defaultPurse = defaultPurse,
            maxSlots = maxSlots,
            maxOverseas = maxOverseas
        )
        _tournaments.value = _tournaments.value + newTourney
        _activeTournamentId.value = newTourney.id
        persistAsync()
        return newTourney
    }

    fun updateTournament(tournament: Tournament) {
        _tournaments.value = _tournaments.value.map { if (it.id == tournament.id) tournament else it }
        persistAsync()
    }

    fun deleteTournament(tournamentId: String) {
        _tournaments.value = _tournaments.value.filterNot { it.id == tournamentId }
        _teams.value = _teams.value.filterNot { it.tournamentId == tournamentId }
        _players.value = _players.value.filterNot { it.tournamentId == tournamentId }
        _matches.value = _matches.value.filterNot { it.tournamentId == tournamentId }
        if (_activeTournamentId.value == tournamentId) {
            _activeTournamentId.value = _tournaments.value.firstOrNull()?.id ?: ""
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
        val newTeam = Team(
            id = "team_${System.currentTimeMillis()}",
            tournamentId = _activeTournamentId.value,
            name = name,
            shortCode = shortCode,
            primaryColorHex = primaryColorHex,
            logoUrl = logoUrl,
            totalPurse = totalPurse,
            spentPurse = 0.0,
            captainEmail = captainEmail
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

    fun togglePlayerFixedSet(playerId: String, isFixed: Boolean) {
        _players.value = _players.value.map { p ->
            if (p.id == playerId) p.copy(isFixedSet = isFixed) else p
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
            val updated = match.copy(
                status = MatchStatus.COMPLETED,
                winnerTeamId = winnerId
            )
            _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
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
        val invite = _captainInvites.value.find { it.code.equals(code, ignoreCase = true) && !it.isClaimed }
            ?: return null

        _captainInvites.value = _captainInvites.value.map {
            if (it.code == invite.code) it.copy(isClaimed = true, invitedEmail = email) else it
        }

        _teams.value = _teams.value.map {
            if (it.id == invite.teamId) it.copy(captainEmail = email) else it
        }

        persistAsync()
        return _teams.value.find { it.id == invite.teamId }
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

                var currentInnings = 1
                var status = MatchStatus.LIVE_INNINGS_1
                if (newOvers >= match.totalOvers || (newWickets >= 10 && !match.isLastPlayerBattingAllowed)) {
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

                var winnerId = match.winnerTeamId
                var status = updatedMatch.status

                // Check Target Chased or All Out / Overs Finish
                if (newRuns > match.teamARuns) {
                    winnerId = match.teamBId
                    status = MatchStatus.COMPLETED
                } else if (newOvers >= match.totalOvers || (newWickets >= 10 && !match.isLastPlayerBattingAllowed)) {
                    winnerId = if (newRuns > match.teamARuns) match.teamBId else if (match.teamARuns > newRuns) match.teamAId else "TIE"
                    status = MatchStatus.COMPLETED
                }

                updatedMatch = updatedMatch.copy(
                    teamBRuns = newRuns,
                    teamBWickets = newWickets,
                    teamBOversBatted = newOvers,
                    winnerTeamId = winnerId,
                    status = status
                )
            }

            _matches.value = _matches.value.map { if (it.id == updatedMatch.id) updatedMatch else it }
        }

        persistAsync()
    }

    fun undoLastBall(matchId: String) {
        val lastBall = _ballRecords.value.filter { it.matchId == matchId }.lastOrNull() ?: return
        _ballRecords.value = _ballRecords.value.filterNot { it.id == lastBall.id }

        // Recalculate match statistics from remaining ball records
        val match = _matches.value.find { it.id == matchId } ?: return
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
            status = if (inn2Balls.isNotEmpty()) MatchStatus.LIVE_INNINGS_2 else MatchStatus.LIVE_INNINGS_1,
            winnerTeamId = null
        )

        _matches.value = _matches.value.map { if (it.id == matchId) updated else it }
        persistAsync()
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

    // --- Multi-Tournament Player Career & CricHeroes MVP Stats ---
    fun getPlayerCareerStats(): List<PlayerCareerStats> {
        val allPlayers = _players.value
        val allBalls = _ballRecords.value

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

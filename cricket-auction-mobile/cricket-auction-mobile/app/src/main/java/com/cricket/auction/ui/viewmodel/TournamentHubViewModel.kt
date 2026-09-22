package com.cricket.auction.ui.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cricket.auction.data.model.BidHistoryEntity
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerAuctionStatus
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.PlayerRole
import com.cricket.auction.data.model.TeamEntity
import com.cricket.auction.data.model.TeamWithPlayers
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.data.repository.BidValidationResult
import com.cricket.auction.data.repository.CricketAuctionRepository
import com.cricket.auction.util.AuctionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GavelStage {
    BIDDING,
    GOING_ONCE,
    GOING_TWICE,
    SOLD,
    UNSOLD
}

data class LiveAuctionState(
    val currentPlayer: PlayerEntity? = null,
    val currentBid: Long = 0L,
    val leadingTeam: TeamEntity? = null,
    val bidHistory: List<BidHistoryEntity> = emptyList(),
    val gavelStage: GavelStage = GavelStage.BIDDING,
    val timerSeconds: Int = 15,
    val isTimerRunning: Boolean = false,
    val auctionMessage: String? = null,
    val isAuctionComplete: Boolean = false
)

class TournamentHubViewModel(
    private val tournamentId: Long,
    private val repository: CricketAuctionRepository
) : ViewModel() {

    // Tournament entity
    val tournament: StateFlow<TournamentEntity?> = repository.getTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Teams
    val teams: StateFlow<List<TeamEntity>> = repository.getTeamsForTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamsWithPlayers: StateFlow<List<TeamWithPlayers>> = repository.getAllTeamsWithPlayers(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Players
    val allPlayers: StateFlow<List<PlayerEntity>> = repository.getPlayersForTournament(tournamentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Top Buys
    val topBuys: StateFlow<List<PlayerEntity>> = repository.getTopBuys(tournamentId, 10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Auction State
    private val _liveAuctionState = MutableStateFlow(LiveAuctionState())
    val liveAuctionState: StateFlow<LiveAuctionState> = _liveAuctionState.asStateFlow()

    // Timer coroutine job
    private var timerJob: Job? = null

    init {
        // Initial setup for live auction
        viewModelScope.launch {
            loadNextUpcomingPlayer()
        }
    }

    // ==========================================
    // TEAM ACTIONS
    // ==========================================

    fun addTeam(
        name: String,
        shortCode: String,
        colorHex: String,
        ownerName: String,
        customPurse: Long? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || shortCode.isBlank()) {
            onError("Team name and short code cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val t = tournament.value ?: repository.getTournamentSync(tournamentId)
                val purse = customPurse ?: (t?.budgetPerTeam ?: 100_00_00_000L)
                val team = TeamEntity(
                    tournamentId = tournamentId,
                    name = name.trim(),
                    shortCode = shortCode.trim().uppercase(),
                    colorHex = colorHex,
                    ownerName = ownerName.trim(),
                    totalPurse = purse,
                    remainingPurse = purse
                )
                repository.saveTeam(team)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add team")
            }
        }
    }

    fun updateTeam(team: TeamEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveTeam(team)
            onSuccess()
        }
    }

    fun deleteTeam(teamId: Long) {
        viewModelScope.launch {
            repository.deleteTeam(teamId)
        }
    }

    // ==========================================
    // PLAYER ACTIONS
    // ==========================================

    fun addPlayer(
        name: String,
        role: PlayerRole,
        battingStyle: String,
        bowlingStyle: String,
        isOverseas: Boolean,
        country: String,
        basePrice: Long,
        tier: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank()) {
            onError("Player name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val currentCount = allPlayers.value.size
                val player = PlayerEntity(
                    tournamentId = tournamentId,
                    name = name.trim(),
                    role = role,
                    battingStyle = battingStyle,
                    bowlingStyle = bowlingStyle,
                    isOverseas = isOverseas,
                    country = country.trim(),
                    basePrice = basePrice,
                    status = PlayerAuctionStatus.UPCOMING,
                    tier = tier.trim().ifEmpty { "Standard" },
                    notes = notes.trim(),
                    orderIndex = currentCount + 1
                )
                repository.savePlayer(player)
                // If no player is active in auction, set this player
                if (_liveAuctionState.value.currentPlayer == null) {
                    loadNextUpcomingPlayer()
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add player")
            }
        }
    }

    fun addBatchPlayers(
        rawText: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val t = tournament.value ?: repository.getTournamentSync(tournamentId)
                val defaultBase = t?.defaultBasePrice ?: 20_00_000L
                val currency = t?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH

                val players = AuctionUtils.parseBatchPlayers(
                    rawText = rawText,
                    tournamentId = tournamentId,
                    defaultBasePrice = defaultBase,
                    currencyFormat = currency
                )

                if (players.isEmpty()) {
                    onError("No valid players found in input. Please check the format.")
                    return@launch
                }

                repository.insertBatchPlayers(players)
                if (_liveAuctionState.value.currentPlayer == null) {
                    loadNextUpcomingPlayer()
                }
                onSuccess(players.size)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to import players")
            }
        }
    }

    fun updatePlayer(player: PlayerEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.savePlayer(player)
            // If current player updated, refresh state
            if (_liveAuctionState.value.currentPlayer?.id == player.id) {
                _liveAuctionState.value = _liveAuctionState.value.copy(currentPlayer = player)
            }
            onSuccess()
        }
    }

    fun deletePlayer(playerId: Long) {
        viewModelScope.launch {
            repository.deletePlayer(playerId)
            if (_liveAuctionState.value.currentPlayer?.id == playerId) {
                loadNextUpcomingPlayer()
            }
        }
    }

    // ==========================================
    // LIVE AUCTION ACTIONS
    // ==========================================

    fun loadNextUpcomingPlayer() {
        viewModelScope.launch {
            stopTimer()
            val nextPlayer = repository.getNextUpcomingPlayer(tournamentId)
            if (nextPlayer != null) {
                setAuctionPlayer(nextPlayer)
            } else {
                _liveAuctionState.value = _liveAuctionState.value.copy(
                    currentPlayer = null,
                    isAuctionComplete = true,
                    currentBid = 0L,
                    leadingTeam = null,
                    gavelStage = GavelStage.BIDDING,
                    auctionMessage = "All upcoming players in this tournament have been auctioned!"
                )
            }
        }
    }

    fun loadRandomUpcomingPlayer() {
        viewModelScope.launch {
            stopTimer()
            val randomPlayer = repository.getRandomUpcomingPlayer(tournamentId)
            if (randomPlayer != null) {
                setAuctionPlayer(randomPlayer)
            } else {
                _liveAuctionState.value = _liveAuctionState.value.copy(
                    auctionMessage = "No upcoming players left to pick randomly."
                )
            }
        }
    }

    fun selectPlayerForAuction(player: PlayerEntity) {
        viewModelScope.launch {
            stopTimer()
            setAuctionPlayer(player)
        }
    }

    private suspend fun setAuctionPlayer(player: PlayerEntity) {
        val bids = repository.getBidsForPlayer(player.id)
        _liveAuctionState.value = LiveAuctionState(
            currentPlayer = player,
            currentBid = player.basePrice,
            leadingTeam = null,
            bidHistory = emptyList(),
            gavelStage = GavelStage.BIDDING,
            timerSeconds = 15,
            isTimerRunning = false,
            auctionMessage = "Bidding started for ${player.name} at Base Price"
        )
    }

    /**
     * Places a bid by a team
     */
    fun placeBid(team: TeamEntity, newBidAmount: Long, onError: (String) -> Unit) {
        val player = _liveAuctionState.value.currentPlayer ?: return
        val t = tournament.value ?: return

        viewModelScope.launch {
            val validation = repository.validateBid(t, team, player, newBidAmount)
            if (validation is BidValidationResult.Invalid) {
                onError(validation.reason)
                return@launch
            }

            // Save to DB
            repository.placeBid(
                tournamentId = tournamentId,
                playerId = player.id,
                teamId = team.id,
                amount = newBidAmount
            )

            // Update live state
            val updatedBidHistory = listOf(
                BidHistoryEntity(
                    tournamentId = tournamentId,
                    playerId = player.id,
                    teamId = team.id,
                    bidAmount = newBidAmount
                )
            ) + _liveAuctionState.value.bidHistory

            _liveAuctionState.value = _liveAuctionState.value.copy(
                currentBid = newBidAmount,
                leadingTeam = team,
                bidHistory = updatedBidHistory,
                gavelStage = GavelStage.BIDDING,
                timerSeconds = 15, // Reset timer on each bid
                auctionMessage = "Bid placed by ${team.name} for ${AuctionUtils.formatAmount(newBidAmount, t.currencyFormat)}"
            )

            startTimer()
        }
    }

    /**
     * Gavel call: Advances through "Going Once" -> "Going Twice" -> "SOLD!"
     */
    fun advanceGavel() {
        val current = _liveAuctionState.value
        val player = current.currentPlayer ?: return
        val leading = current.leadingTeam

        when (current.gavelStage) {
            GavelStage.BIDDING -> {
                _liveAuctionState.value = current.copy(
                    gavelStage = GavelStage.GOING_ONCE,
                    timerSeconds = 10,
                    auctionMessage = "Going once... to ${leading?.name ?: "Opening"}"
                )
                startTimer()
            }
            GavelStage.GOING_ONCE -> {
                _liveAuctionState.value = current.copy(
                    gavelStage = GavelStage.GOING_TWICE,
                    timerSeconds = 5,
                    auctionMessage = "Going twice... Final call!"
                )
                startTimer()
            }
            GavelStage.GOING_TWICE -> {
                if (leading != null) {
                    sellCurrentPlayer()
                } else {
                    markCurrentPlayerUnsold()
                }
            }
            GavelStage.SOLD, GavelStage.UNSOLD -> {
                // Already completed
            }
        }
    }

    /**
     * Immediately hammer SOLD to the leading team
     */
    fun sellCurrentPlayer() {
        val current = _liveAuctionState.value
        val player = current.currentPlayer ?: return
        val leadingTeam = current.leadingTeam
        val price = current.currentBid
        val t = tournament.value

        if (leadingTeam == null) {
            _liveAuctionState.value = current.copy(
                auctionMessage = "Cannot sell without any bidding team. Mark as UNSOLD instead."
            )
            return
        }

        stopTimer()

        viewModelScope.launch {
            repository.completeSale(
                tournamentId = tournamentId,
                player = player,
                winningTeamId = leadingTeam.id,
                winningPrice = price
            )

            _liveAuctionState.value = current.copy(
                gavelStage = GavelStage.SOLD,
                isTimerRunning = false,
                auctionMessage = "🎉 SOLD! ${player.name} bought by ${leadingTeam.name} for ${AuctionUtils.formatAmount(price, t?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH)}"
            )
        }
    }

    /**
     * Mark player UNSOLD
     */
    fun markCurrentPlayerUnsold() {
        val current = _liveAuctionState.value
        val player = current.currentPlayer ?: return

        stopTimer()

        viewModelScope.launch {
            repository.markPlayerUnsold(player)
            _liveAuctionState.value = current.copy(
                gavelStage = GavelStage.UNSOLD,
                isTimerRunning = false,
                auctionMessage = "${player.name} is marked UNSOLD"
            )
        }
    }

    /**
     * Pass/skip current player without marking unsold (keeps status UPCOMING)
     */
    fun passCurrentPlayer() {
        stopTimer()
        loadNextUpcomingPlayer()
    }

    /**
     * Undo last bid made during auction of current player
     */
    fun undoLastBid() {
        val current = _liveAuctionState.value
        val player = current.currentPlayer ?: return
        val history = current.bidHistory

        if (history.isEmpty()) {
            _liveAuctionState.value = current.copy(auctionMessage = "No bids to undo")
            return
        }

        viewModelScope.launch {
            repository.undoLastBid(player.id)
            val updatedHistory = history.drop(1)
            if (updatedHistory.isNotEmpty()) {
                val previousBid = updatedHistory.first()
                val previousTeam = teams.value.find { it.id == previousBid.teamId }
                _liveAuctionState.value = current.copy(
                    currentBid = previousBid.bidAmount,
                    leadingTeam = previousTeam,
                    bidHistory = updatedHistory,
                    gavelStage = GavelStage.BIDDING,
                    auctionMessage = "Last bid undone. Current bid: ${previousBid.bidAmount}"
                )
            } else {
                _liveAuctionState.value = current.copy(
                    currentBid = player.basePrice,
                    leadingTeam = null,
                    bidHistory = emptyList(),
                    gavelStage = GavelStage.BIDDING,
                    auctionMessage = "All bids reverted. Reset to base price."
                )
            }
        }
    }

    /**
     * Release player back to auction pool (undo sale)
     */
    fun undoSaleForPlayer(playerId: Long) {
        viewModelScope.launch {
            repository.undoSale(playerId)
            // Reload
            val updatedPlayer = repository.getPlayerSync(playerId)
            if (updatedPlayer != null) {
                setAuctionPlayer(updatedPlayer)
            }
        }
    }

    /**
     * Re-auction all unsold players (Accelerated round)
     */
    fun reAuctionUnsoldPlayers(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val unsold = repository.getPlayersByStatusSync(tournamentId, PlayerAuctionStatus.UNSOLD)
            repository.restartUnsoldPlayers(tournamentId)
            loadNextUpcomingPlayer()
            onComplete(unsold.size)
        }
    }

    /**
     * Reset entire tournament auction to start over
     */
    fun resetTournamentAuction(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetTournamentAuction(tournamentId)
            loadNextUpcomingPlayer()
            onComplete()
        }
    }

    // ==========================================
    // TIMER CONTROLS
    // ==========================================

    fun startTimer() {
        stopTimer()
        _liveAuctionState.value = _liveAuctionState.value.copy(isTimerRunning = true)
        timerJob = viewModelScope.launch {
            while (_liveAuctionState.value.timerSeconds > 0 && _liveAuctionState.value.isTimerRunning) {
                delay(1000)
                val current = _liveAuctionState.value
                if (!current.isTimerRunning) break

                val newSeconds = current.timerSeconds - 1
                _liveAuctionState.value = current.copy(timerSeconds = newSeconds)

                if (newSeconds == 0) {
                    // Automatically trigger next gavel stage or finalize
                    advanceGavel()
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _liveAuctionState.value = _liveAuctionState.value.copy(isTimerRunning = false)
    }

    fun resetTimer(seconds: Int = 15) {
        stopTimer()
        _liveAuctionState.value = _liveAuctionState.value.copy(
            timerSeconds = seconds,
            isTimerRunning = false
        )
    }

    fun addTimerSeconds(extra: Int = 5) {
        val current = _liveAuctionState.value
        _liveAuctionState.value = current.copy(timerSeconds = current.timerSeconds + extra)
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _liveAuctionState.value = _liveAuctionState.value.copy(isTimerRunning = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    // ==========================================
    // REPORT GENERATOR
    // ==========================================

    fun generateAuctionReport(): String {
        val t = tournament.value ?: return ""
        val teamsData = teamsWithPlayers.value
        val players = allPlayers.value
        val sold = players.filter { it.status == PlayerAuctionStatus.SOLD }
        val unsold = players.filter { it.status == PlayerAuctionStatus.UNSOLD }
        val currency = t.currencyFormat

        val sb = StringBuilder()
        sb.appendLine("🏏====================================🏏")
        sb.appendLine("     ${t.name.uppercase()} ${t.edition}")
        sb.appendLine("           AUCTION REPORT")
        sb.appendLine("🏏====================================🏏\n")

        sb.appendLine("📊 OVERVIEW:")
        sb.appendLine("• Total Players: ${players.size}")
        sb.appendLine("• Sold: ${sold.size}")
        sb.appendLine("• Unsold: ${unsold.size}")
        sb.appendLine("• Remaining: ${players.size - sold.size - unsold.size}")
        val totalSpent = sold.sumOf { it.soldPrice ?: 0L }
        sb.appendLine("• Total Spent: ${AuctionUtils.formatAmount(totalSpent, currency)}\n")

        sb.appendLine("🏆 TOP BUYS:")
        val topList = sold.sortedByDescending { it.soldPrice ?: 0L }.take(5)
        if (topList.isEmpty()) {
            sb.appendLine("No players sold yet.\n")
        } else {
            topList.forEachIndexed { index, p ->
                val team = teamsData.find { it.team.id == p.soldToTeamId }?.team
                sb.appendLine("${index + 1}. ${p.name} (${p.role.shortName}) - ${AuctionUtils.formatAmount(p.soldPrice ?: 0L, currency)} -> ${team?.shortCode ?: "N/A"}")
            }
            sb.appendLine()
        }

        sb.appendLine("👥 TEAM SQUADS:")
        for (twp in teamsData) {
            val team = twp.team
            sb.appendLine("----------------------------------------")
            sb.appendLine("🔹 ${team.name} [${team.shortCode}]")
            sb.appendLine("  Purse Remaining: ${AuctionUtils.formatAmount(team.remainingPurse, currency)} / ${AuctionUtils.formatAmount(team.totalPurse, currency)}")
            sb.appendLine("  Squad Size: ${twp.squadCount}/${t.maxSquadSize} (Overseas: ${twp.overseasCount}/${t.maxOverseasPlayers})")
            sb.appendLine("  Roles: Batsmen: ${twp.batsmenCount}, Bowlers: ${twp.bowlersCount}, AR: ${twp.allRoundersCount}, WK: ${twp.wicketKeepersCount}")
            sb.appendLine("  Roster:")
            if (twp.players.isEmpty()) {
                sb.appendLine("    (No players bought)")
            } else {
                twp.players.forEach { p ->
                    sb.appendLine("    • ${p.name} [${p.role.shortName}${if (p.isOverseas) ", OS" else ""}] - ${AuctionUtils.formatAmount(p.soldPrice ?: 0L, currency)}")
                }
            }
            sb.appendLine()
        }

        if (unsold.isNotEmpty()) {
            sb.appendLine("❌ UNSOLD PLAYERS (${unsold.size}):")
            unsold.forEach { p ->
                sb.appendLine("• ${p.name} (${p.role.shortName}) - Base: ${AuctionUtils.formatAmount(p.basePrice, currency)}")
            }
            sb.appendLine()
        }

        sb.appendLine("Generated by Cricket Auction App")
        return sb.toString()
    }

    companion object {
        fun provideFactory(
            tournamentId: Long,
            repository: CricketAuctionRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TournamentHubViewModel(tournamentId, repository) as T
                }
            }
        }
    }
}

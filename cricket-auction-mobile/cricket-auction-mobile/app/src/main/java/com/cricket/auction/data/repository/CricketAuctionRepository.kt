package com.cricket.auction.data.repository

import com.cricket.auction.data.CricketAuctionDatabase
import com.cricket.auction.data.model.BidHistoryEntity
import com.cricket.auction.data.model.PlayerAuctionStatus
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.TeamEntity
import com.cricket.auction.data.model.TeamWithPlayers
import com.cricket.auction.data.model.TournamentEntity
import kotlinx.coroutines.flow.Flow

sealed class BidValidationResult {
    object Valid : BidValidationResult()
    data class Invalid(val reason: String) : BidValidationResult()
}

class CricketAuctionRepository(private val db: CricketAuctionDatabase) {

    private val tournamentDao = db.tournamentDao()
    private val teamDao = db.teamDao()
    private val playerDao = db.playerDao()
    private val bidHistoryDao = db.bidHistoryDao()

    // --- Tournaments ---
    fun getAllTournaments(): Flow<List<TournamentEntity>> = tournamentDao.getAllTournaments()

    fun getTournament(id: Long): Flow<TournamentEntity?> = tournamentDao.getTournamentById(id)

    suspend fun getTournamentSync(id: Long): TournamentEntity? = tournamentDao.getTournamentByIdSync(id)

    suspend fun saveTournament(tournament: TournamentEntity): Long {
        return if (tournament.id == 0L) {
            tournamentDao.insert(tournament)
        } else {
            tournamentDao.update(tournament)
            tournament.id
        }
    }

    suspend fun deleteTournament(id: Long) {
        tournamentDao.deleteById(id)
    }

    // --- Teams ---
    fun getTeamsForTournament(tournamentId: Long): Flow<List<TeamEntity>> =
        teamDao.getTeamsForTournament(tournamentId)

    suspend fun getTeamsForTournamentSync(tournamentId: Long): List<TeamEntity> =
        teamDao.getTeamsForTournamentSync(tournamentId)

    fun getTeamWithPlayers(teamId: Long): Flow<TeamWithPlayers?> =
        teamDao.getTeamWithPlayers(teamId)

    fun getAllTeamsWithPlayers(tournamentId: Long): Flow<List<TeamWithPlayers>> =
        teamDao.getAllTeamsWithPlayers(tournamentId)

    suspend fun getAllTeamsWithPlayersSync(tournamentId: Long): List<TeamWithPlayers> =
        teamDao.getAllTeamsWithPlayersSync(tournamentId)

    suspend fun saveTeam(team: TeamEntity): Long {
        return if (team.id == 0L) {
            teamDao.insert(team)
        } else {
            teamDao.update(team)
            team.id
        }
    }

    suspend fun deleteTeam(teamId: Long) {
        // Any players assigned to this team will have soldToTeamId set to NULL via CASCADE/SET_NULL
        teamDao.deleteById(teamId)
    }

    // --- Players ---
    fun getPlayersForTournament(tournamentId: Long): Flow<List<PlayerEntity>> =
        playerDao.getPlayersForTournament(tournamentId)

    suspend fun getPlayersForTournamentSync(tournamentId: Long): List<PlayerEntity> =
        playerDao.getPlayersForTournamentSync(tournamentId)

    fun getPlayer(id: Long): Flow<PlayerEntity?> =
        playerDao.getPlayerById(id)

    suspend fun getPlayerSync(id: Long): PlayerEntity? =
        playerDao.getPlayerByIdSync(id)

    fun getPlayersByStatus(tournamentId: Long, status: PlayerAuctionStatus): Flow<List<PlayerEntity>> =
        playerDao.getPlayersByStatus(tournamentId, status)

    suspend fun getPlayersByStatusSync(tournamentId: Long, status: PlayerAuctionStatus): List<PlayerEntity> =
        playerDao.getPlayersByStatusSync(tournamentId, status)

    fun getPlayersForTeam(teamId: Long): Flow<List<PlayerEntity>> =
        playerDao.getPlayersForTeam(teamId)

    suspend fun getNextUpcomingPlayer(tournamentId: Long): PlayerEntity? =
        playerDao.getNextUpcomingPlayer(tournamentId)

    suspend fun getRandomUpcomingPlayer(tournamentId: Long): PlayerEntity? =
        playerDao.getRandomUpcomingPlayer(tournamentId)

    suspend fun getRandomUnsoldPlayer(tournamentId: Long): PlayerEntity? =
        playerDao.getRandomUnsoldPlayer(tournamentId)

    fun getTopBuys(tournamentId: Long, limit: Int = 10): Flow<List<PlayerEntity>> =
        playerDao.getTopBuys(tournamentId, limit)

    suspend fun savePlayer(player: PlayerEntity): Long {
        return if (player.id == 0L) {
            playerDao.insert(player)
        } else {
            playerDao.update(player)
            player.id
        }
    }

    suspend fun insertBatchPlayers(players: List<PlayerEntity>) {
        playerDao.insertAll(players)
    }

    suspend fun deletePlayer(playerId: Long) {
        // If player was sold, we should refund the team
        val player = playerDao.getPlayerByIdSync(playerId)
        if (player != null && player.soldToTeamId != null && player.soldPrice != null) {
            val team = teamDao.getTeamByIdSync(player.soldToTeamId)
            if (team != null) {
                teamDao.updateRemainingPurse(team.id, team.remainingPurse + player.soldPrice)
            }
        }
        playerDao.deleteById(playerId)
    }

    // --- Auction Logic ---

    /**
     * Validates if a team can bid the given amount for the player.
     */
    suspend fun validateBid(
        tournament: TournamentEntity,
        team: TeamEntity,
        player: PlayerEntity,
        bidAmount: Long
    ): BidValidationResult {
        // 1. Check if team has enough purse
        if (bidAmount > team.remainingPurse) {
            return BidValidationResult.Invalid("Insufficient purse (Has ₹${team.remainingPurse}, bid ₹$bidAmount)")
        }

        // 2. Fetch team's current players
        val currentPlayers = playerDao.getPlayersForTeamSync(team.id)

        // 3. Max squad size check
        if (currentPlayers.size >= tournament.maxSquadSize) {
            return BidValidationResult.Invalid("Squad is full (${tournament.maxSquadSize} max)")
        }

        // 4. Overseas quota check
        if (player.isOverseas) {
            val overseasCount = currentPlayers.count { it.isOverseas }
            if (overseasCount >= tournament.maxOverseasPlayers) {
                return BidValidationResult.Invalid("Overseas quota full (${tournament.maxOverseasPlayers} max)")
            }
        }

        // 5. Reserve purse check for minimum squad requirement
        val newSquadSize = currentPlayers.size + 1
        val slotsRemainingForMin = (tournament.minSquadSize - newSquadSize).coerceAtLeast(0)
        val reserveNeeded = slotsRemainingForMin * tournament.defaultBasePrice
        val purseAfterBid = team.remainingPurse - bidAmount

        if (purseAfterBid < reserveNeeded) {
            return BidValidationResult.Invalid(
                "Must reserve money to reach min squad size of ${tournament.minSquadSize}"
            )
        }

        return BidValidationResult.Valid
    }

    suspend fun placeBid(
        tournamentId: Long,
        playerId: Long,
        teamId: Long,
        amount: Long
    ): Long {
        val bid = BidHistoryEntity(
            tournamentId = tournamentId,
            playerId = playerId,
            teamId = teamId,
            bidAmount = amount,
            isWinningBid = false
        )
        return bidHistoryDao.insert(bid)
    }

    suspend fun completeSale(
        tournamentId: Long,
        player: PlayerEntity,
        winningTeamId: Long,
        winningPrice: Long
    ) {
        val team = teamDao.getTeamByIdSync(winningTeamId) ?: return

        // Mark player sold
        playerDao.markSold(
            playerId = player.id,
            teamId = winningTeamId,
            soldPrice = winningPrice,
            status = PlayerAuctionStatus.SOLD
        )

        // Deduct purse
        val updatedPurse = (team.remainingPurse - winningPrice).coerceAtLeast(0L)
        teamDao.updateRemainingPurse(team.id, updatedPurse)

        // Record winning bid
        val winningBid = BidHistoryEntity(
            tournamentId = tournamentId,
            playerId = player.id,
            teamId = winningTeamId,
            bidAmount = winningPrice,
            isWinningBid = true
        )
        bidHistoryDao.insert(winningBid)
    }

    suspend fun markPlayerUnsold(player: PlayerEntity) {
        playerDao.markUnsold(player.id)
    }

    suspend fun undoSale(playerId: Long) {
        val player = playerDao.getPlayerByIdSync(playerId) ?: return
        if (player.soldToTeamId != null && player.soldPrice != null) {
            val team = teamDao.getTeamByIdSync(player.soldToTeamId)
            if (team != null) {
                teamDao.updateRemainingPurse(team.id, team.remainingPurse + player.soldPrice)
            }
        }
        playerDao.resetPlayer(playerId)
        bidHistoryDao.deleteBidsForPlayer(playerId)
    }

    suspend fun restartUnsoldPlayers(tournamentId: Long) {
        playerDao.resetUnsoldPlayersToUpcoming(tournamentId)
    }

    suspend fun resetTournamentAuction(tournamentId: Long) {
        // Reset all teams' purses to their totalPurse
        val teams = teamDao.getTeamsForTournamentSync(tournamentId)
        for (team in teams) {
            teamDao.updateRemainingPurse(team.id, team.totalPurse)
        }
        // Reset all players to UPCOMING
        playerDao.resetAllPlayersInTournament(tournamentId)
        // Clear all bid history
        bidHistoryDao.clearAllBidsInTournament(tournamentId)
    }

    fun getBidsForPlayer(playerId: Long): Flow<List<BidHistoryEntity>> =
        bidHistoryDao.getBidsForPlayer(playerId)

    suspend fun getLastBidForPlayer(playerId: Long): BidHistoryEntity? =
        bidHistoryDao.getLastBidForPlayer(playerId)

    suspend fun undoLastBid(playerId: Long) {
        val lastBid = bidHistoryDao.getLastBidForPlayer(playerId)
        if (lastBid != null) {
            bidHistoryDao.deleteBid(lastBid.id)
        }
    }
}

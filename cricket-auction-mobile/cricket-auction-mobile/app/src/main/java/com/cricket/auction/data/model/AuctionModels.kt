package com.cricket.auction.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class TeamWithPlayers(
    @Embedded val team: TeamEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "soldToTeamId"
    )
    val players: List<PlayerEntity> = emptyList()
) {
    val batsmenCount: Int get() = players.count { it.role == PlayerRole.BATSMAN }
    val bowlersCount: Int get() = players.count { it.role == PlayerRole.BOWLER }
    val allRoundersCount: Int get() = players.count { it.role == PlayerRole.ALL_ROUNDER }
    val wicketKeepersCount: Int get() = players.count { it.role == PlayerRole.WICKET_KEEPER }
    val overseasCount: Int get() = players.count { it.isOverseas }
    val totalSpent: Long get() = players.sumOf { it.soldPrice ?: 0L }
    val squadCount: Int get() = players.size
}

data class BidWithDetails(
    val bid: BidHistoryEntity,
    val teamName: String,
    val teamShortCode: String,
    val teamColorHex: String,
    val playerName: String
)

data class TournamentSummary(
    val tournament: TournamentEntity,
    val totalTeams: Int,
    val totalPlayers: Int,
    val soldPlayers: Int,
    val unsoldPlayers: Int,
    val upcomingPlayers: Int,
    val totalMoneySpent: Long,
    val remainingPurseTotal: Long,
    val highestBidPlayer: PlayerEntity?
)

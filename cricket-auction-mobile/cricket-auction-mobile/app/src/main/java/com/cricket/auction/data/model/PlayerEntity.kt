package com.cricket.auction.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlayerRole(val displayName: String, val shortName: String) {
    BATSMAN("Batsman", "BAT"),
    BOWLER("Bowler", "BOWL"),
    ALL_ROUNDER("All-Rounder", "AR"),
    WICKET_KEEPER("Wicket Keeper", "WK")
}

enum class PlayerAuctionStatus {
    UPCOMING,
    IN_AUCTION,
    SOLD,
    UNSOLD
}

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["soldToTeamId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tournamentId"]),
        Index(value = ["soldToTeamId"])
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tournamentId: Long,
    val name: String,
    val role: PlayerRole = PlayerRole.BATSMAN,
    val battingStyle: String = "Right Hand",
    val bowlingStyle: String = "Right-arm Medium",
    val isOverseas: Boolean = false,
    val country: String = "Domestic",
    val basePrice: Long = 20_00_000L,
    val status: PlayerAuctionStatus = PlayerAuctionStatus.UPCOMING,
    val soldToTeamId: Long? = null,
    val soldPrice: Long? = null,
    val orderIndex: Int = 0,
    val tier: String = "Standard",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

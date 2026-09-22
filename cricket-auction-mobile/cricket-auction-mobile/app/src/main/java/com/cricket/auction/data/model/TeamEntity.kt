package com.cricket.auction.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teams",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tournamentId"])]
)
data class TeamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tournamentId: Long,
    val name: String,
    val shortCode: String,
    val colorHex: String = "#1E88E5",
    val ownerName: String = "",
    val totalPurse: Long,
    val remainingPurse: Long,
    val createdAt: Long = System.currentTimeMillis()
)

package com.cricket.auction.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TournamentStatus {
    UPCOMING,
    IN_PROGRESS,
    COMPLETED
}

enum class CurrencyFormat {
    INR_CR_LAKH, // e.g. ₹20 L, ₹1.5 Cr, ₹100 Cr
    POINTS,      // e.g. 1000 Pts
    USD          // e.g. $500K, $2M
}

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val edition: String = "",
    val budgetPerTeam: Long = 100_00_00_000L, // default 100 Cr
    val currencyFormat: CurrencyFormat = CurrencyFormat.INR_CR_LAKH,
    val minSquadSize: Int = 15,
    val maxSquadSize: Int = 25,
    val maxOverseasPlayers: Int = 8,
    val defaultBasePrice: Long = 20_00_000L, // 20 Lakhs
    val status: TournamentStatus = TournamentStatus.UPCOMING,
    val createdAt: Long = System.currentTimeMillis()
)

package com.cricket.auction.util

import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.PlayerRole
import java.text.DecimalFormat
import java.util.Locale

object AuctionUtils {

    private val crFormat = DecimalFormat("0.##")
    private val lakhFormat = DecimalFormat("0.##")
    private val standardFormat = DecimalFormat("#,##,###")

    val DEFAULT_TEAM_COLORS = listOf(
        "#1E88E5", // Blue
        "#E53935", // Red
        "#FDD835", // Yellow / Gold
        "#8E24AA", // Purple
        "#00897B", // Teal
        "#FB8C00", // Orange
        "#3949AB", // Indigo
        "#43A047", // Green
        "#D81B60", // Pink
        "#6D4C41", // Brown
        "#00ACC1", // Cyan
        "#546E7A"  // Blue Grey
    )

    fun formatAmount(amount: Long, format: CurrencyFormat = CurrencyFormat.INR_CR_LAKH): String {
        return when (format) {
            CurrencyFormat.INR_CR_LAKH -> {
                when {
                    amount >= 1_00_00_000L -> {
                        val cr = amount.toDouble() / 1_00_00_000.0
                        "₹${crFormat.format(cr)} Cr"
                    }
                    amount >= 1_00_000L -> {
                        val lakh = amount.toDouble() / 1_00_000.0
                        "₹${lakhFormat.format(lakh)} L"
                    }
                    else -> "₹${standardFormat.format(amount)}"
                }
            }
            CurrencyFormat.POINTS -> {
                "${standardFormat.format(amount)} Pts"
            }
            CurrencyFormat.USD -> {
                when {
                    amount >= 1_000_000L -> "$${crFormat.format(amount / 1_000_000.0)}M"
                    amount >= 1_000L -> "$${lakhFormat.format(amount / 1_000.0)}K"
                    else -> "$$amount"
                }
            }
        }
    }

    /**
     * Parses flexible user input like "20L", "1.5Cr", "100 Cr", "50", "2000000"
     */
    fun parseAmount(input: String, format: CurrencyFormat = CurrencyFormat.INR_CR_LAKH): Long? {
        val clean = input.trim().replace(",", "").replace("₹", "").replace("$", "").lowercase(Locale.ROOT)
        if (clean.isEmpty()) return null

        try {
            if (clean.endsWith("cr") || clean.endsWith("crore") || clean.endsWith("crores")) {
                val numStr = clean.replace("crores", "").replace("crore", "").replace("cr", "").trim()
                val value = numStr.toDoubleOrNull() ?: return null
                return (value * 1_00_00_000L).toLong()
            }
            if (clean.endsWith("l") || clean.endsWith("lakh") || clean.endsWith("lakhs") || clean.endsWith("lac") || clean.endsWith("lacs")) {
                val numStr = clean.replace("lakhs", "").replace("lakh", "").replace("lacs", "").replace("lac", "").replace("l", "").trim()
                val value = numStr.toDoubleOrNull() ?: return null
                return (value * 1_00_000L).toLong()
            }
            if (clean.endsWith("k")) {
                val numStr = clean.replace("k", "").trim()
                val value = numStr.toDoubleOrNull() ?: return null
                return (value * 1_000L).toLong()
            }
            if (clean.endsWith("m")) {
                val numStr = clean.replace("m", "").trim()
                val value = numStr.toDoubleOrNull() ?: return null
                return (value * 1_000_000L).toLong()
            }
            if (clean.endsWith("pts")) {
                val numStr = clean.replace("pts", "").trim()
                return numStr.toLongOrNull()
            }

            // Raw number: check if it's small (e.g. user entered "2" meaning 2 Cr or 20 meaning 20 L?)
            val rawLong = clean.toLongOrNull()
            if (rawLong != null) {
                return rawLong
            }
            val rawDouble = clean.toDoubleOrNull()
            if (rawDouble != null) {
                return rawDouble.toLong()
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    /**
     * Computes increment buttons appropriate for the current bid amount
     */
    fun getSuggestedIncrements(currentBid: Long, format: CurrencyFormat): List<Long> {
        return when (format) {
            CurrencyFormat.INR_CR_LAKH -> {
                when {
                    currentBid < 1_00_00_000L -> listOf(5_00_000L, 10_00_000L, 20_00_000L, 25_00_000L)
                    currentBid < 5_00_00_000L -> listOf(10_00_000L, 20_00_000L, 25_00_000L, 50_00_000L)
                    currentBid < 10_00_00_000L -> listOf(25_00_000L, 50_00_000L, 1_00_00_000L, 2_00_00_000L)
                    else -> listOf(50_00_000L, 1_00_00_000L, 2_00_00_000L, 5_00_00_000L)
                }
            }
            CurrencyFormat.POINTS -> {
                when {
                    currentBid < 100L -> listOf(10L, 20L, 50L, 100L)
                    currentBid < 500L -> listOf(25L, 50L, 100L, 200L)
                    else -> listOf(50L, 100L, 250L, 500L)
                }
            }
            CurrencyFormat.USD -> {
                when {
                    currentBid < 100_000L -> listOf(10_000L, 25_000L, 50_000L, 100_000L)
                    currentBid < 1_000_000L -> listOf(50_000L, 100_000L, 250_000L, 500_000L)
                    else -> listOf(250_000L, 500_000L, 1_000_000L, 2_000_000L)
                }
            }
        }
    }

    /**
     * Parses batch text into PlayerEntity objects.
     * Supports formats:
     * 1. Name only (e.g. "Rohit Sharma")
     * 2. Name, Role, BasePrice (e.g. "Virat Kohli, Batsman, 2 Cr")
     * 3. Full CSV: Name, Role, Batting, Bowling, Overseas, BasePrice, Tier
     */
    fun parseBatchPlayers(
        rawText: String,
        tournamentId: Long,
        defaultBasePrice: Long,
        currencyFormat: CurrencyFormat
    ): List<PlayerEntity> {
        val lines = rawText.split("\n", "\r\n")
        val players = mutableListOf<PlayerEntity>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue

            val parts = trimmed.split(",", "\t", ";").map { it.trim() }
            if (parts.isEmpty()) continue

            val name = parts[0]
            if (name.isEmpty()) continue

            var role = PlayerRole.BATSMAN
            var basePrice = defaultBasePrice
            var isOverseas = false
            var country = "Domestic"
            var tier = "Standard"

            if (parts.size > 1) {
                val rolePart = parts[1].lowercase(Locale.ROOT)
                role = when {
                    rolePart.contains("wk") || rolePart.contains("keeper") -> PlayerRole.WICKET_KEEPER
                    rolePart.contains("bowl") -> PlayerRole.BOWLER
                    rolePart.contains("all") || rolePart.contains("ar") -> PlayerRole.ALL_ROUNDER
                    rolePart.contains("bat") -> PlayerRole.BATSMAN
                    else -> PlayerRole.BATSMAN
                }
            }

            if (parts.size > 2) {
                val priceParsed = parseAmount(parts[2], currencyFormat)
                if (priceParsed != null && priceParsed > 0) {
                    basePrice = priceParsed
                }
            }

            if (parts.size > 3) {
                val overseasPart = parts[3].lowercase(Locale.ROOT)
                isOverseas = overseasPart.contains("yes") ||
                        overseasPart.contains("true") ||
                        overseasPart.contains("overseas") ||
                        overseasPart.contains("os") ||
                        (!overseasPart.contains("domestic") && !overseasPart.contains("ind") && !overseasPart.contains("no"))
                if (isOverseas) {
                    country = parts[3]
                }
            }

            if (parts.size > 4) {
                tier = parts[4]
            }

            players.add(
                PlayerEntity(
                    tournamentId = tournamentId,
                    name = name,
                    role = role,
                    basePrice = basePrice,
                    isOverseas = isOverseas,
                    country = country,
                    tier = tier,
                    orderIndex = players.size + 1
                )
            )
        }

        return players
    }
}

package com.cricket.auction

import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerRole
import com.cricket.auction.util.AuctionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuctionUtilsTest {

    @Test
    fun testFormatAmountINR() {
        assertEquals("₹100 Cr", AuctionUtils.formatAmount(100_00_00_000L, CurrencyFormat.INR_CR_LAKH))
        assertEquals("₹1.5 Cr", AuctionUtils.formatAmount(1_50_00_000L, CurrencyFormat.INR_CR_LAKH))
        assertEquals("₹20 L", AuctionUtils.formatAmount(20_00_000L, CurrencyFormat.INR_CR_LAKH))
        assertEquals("₹50 L", AuctionUtils.formatAmount(50_00_000L, CurrencyFormat.INR_CR_LAKH))
    }

    @Test
    fun testFormatAmountPointsAndUSD() {
        assertEquals("5,000 Pts", AuctionUtils.formatAmount(5000L, CurrencyFormat.POINTS))
        assertEquals("$2M", AuctionUtils.formatAmount(2_000_000L, CurrencyFormat.USD))
        assertEquals("$500K", AuctionUtils.formatAmount(500_000L, CurrencyFormat.USD))
    }

    @Test
    fun testParseAmount() {
        assertEquals(100_00_00_000L, AuctionUtils.parseAmount("100 Cr", CurrencyFormat.INR_CR_LAKH))
        assertEquals(1_50_00_000L, AuctionUtils.parseAmount("1.5Cr", CurrencyFormat.INR_CR_LAKH))
        assertEquals(20_00_000L, AuctionUtils.parseAmount("20 L", CurrencyFormat.INR_CR_LAKH))
        assertEquals(50_00_000L, AuctionUtils.parseAmount("50 Lakhs", CurrencyFormat.INR_CR_LAKH))
        assertEquals(1500L, AuctionUtils.parseAmount("1500 Pts", CurrencyFormat.POINTS))
    }

    @Test
    fun testSuggestedIncrements() {
        val lowIncrements = AuctionUtils.getSuggestedIncrements(50_00_000L, CurrencyFormat.INR_CR_LAKH)
        assertTrue(lowIncrements.contains(10_00_000L))

        val midIncrements = AuctionUtils.getSuggestedIncrements(3_00_00_000L, CurrencyFormat.INR_CR_LAKH)
        assertTrue(midIncrements.contains(20_00_000L))

        val highIncrements = AuctionUtils.getSuggestedIncrements(8_00_00_000L, CurrencyFormat.INR_CR_LAKH)
        assertTrue(highIncrements.contains(1_00_00_000L))
    }

    @Test
    fun testBatchPlayerParsing() {
        val raw = """
            Virat Kohli, Batsman, 2 Cr, Domestic, Marquee
            Jasprit Bumrah, Bowler, 2 Cr
            Pat Cummins, All-Rounder, 2 Cr, Australia, Marquee
            Rishabh Pant, Wicket Keeper, 2 Cr
        """.trimIndent()

        val parsed = AuctionUtils.parseBatchPlayers(
            rawText = raw,
            tournamentId = 1L,
            defaultBasePrice = 20_00_000L,
            currencyFormat = CurrencyFormat.INR_CR_LAKH
        )

        assertEquals(4, parsed.size)

        // Check Virat
        val virat = parsed[0]
        assertEquals("Virat Kohli", virat.name)
        assertEquals(PlayerRole.BATSMAN, virat.role)
        assertEquals(2_00_00_000L, virat.basePrice)
        assertFalse(virat.isOverseas)
        assertEquals("Marquee", virat.tier)

        // Check Bumrah
        val bumrah = parsed[1]
        assertEquals("Jasprit Bumrah", bumrah.name)
        assertEquals(PlayerRole.BOWLER, bumrah.role)

        // Check Cummins (Overseas)
        val cummins = parsed[2]
        assertEquals("Pat Cummins", cummins.name)
        assertEquals(PlayerRole.ALL_ROUNDER, cummins.role)
        assertTrue(cummins.isOverseas)
        assertEquals("Australia", cummins.country)

        // Check Pant (WK)
        val pant = parsed[3]
        assertEquals("Rishabh Pant", pant.name)
        assertEquals(PlayerRole.WICKET_KEEPER, pant.role)
    }
}

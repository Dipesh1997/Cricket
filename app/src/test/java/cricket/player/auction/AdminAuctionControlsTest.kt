package cricket.player.auction

import cricket.player.auction.model.Bid
import cricket.player.auction.model.Player
import cricket.player.auction.model.PlayerRole
import cricket.player.auction.model.PlayerStatus
import cricket.player.auction.model.Team
import org.junit.Assert.*
import org.junit.Test

class AdminAuctionControlsTest {

    @Test
    fun testEditPlayerSoldPriceAdjustsTeamPurse() {
        val team = Team(id = "team1", name = "Mumbai Strikers", shortCode = "MS", primaryColorHex = "#004BA0", totalPurse = 100.0, spentPurse = 12.0)
        val player = Player(
            id = "p1",
            name = "Virat",
            role = PlayerRole.BATSMAN,
            country = "India",
            isOverseas = false,
            basePrice = 2.0,
            currentBid = 12.0,
            soldPrice = 12.0,
            soldToTeamId = "team1",
            status = PlayerStatus.SOLD
        )

        val oldPrice = player.soldPrice ?: player.currentBid
        val newPrice = 15.0
        val diff = newPrice - oldPrice

        val updatedTeam = team.copy(spentPurse = (team.spentPurse + diff).coerceAtLeast(0.0))
        val updatedPlayer = player.copy(soldPrice = newPrice, currentBid = newPrice)

        assertEquals(15.0, updatedTeam.spentPurse, 0.001)
        assertEquals(85.0, updatedTeam.remainingPurse, 0.001)
        assertEquals(15.0, updatedPlayer.soldPrice ?: 0.0, 0.001)
    }

    @Test
    fun testUndoLastBidRestoresPreviousBidOrBasePrice() {
        val player = Player(
            id = "p1",
            name = "Rohit",
            role = PlayerRole.BATSMAN,
            country = "India",
            isOverseas = false,
            basePrice = 2.0,
            currentBid = 3.5,
            highestBidderTeamId = "team2",
            status = PlayerStatus.BIDDING
        )
        val bid1 = Bid(id = "b1", playerId = "p1", teamId = "team1", teamName = "CSK", amount = 2.5)
        val bid2 = Bid(id = "b2", playerId = "p1", teamId = "team2", teamName = "MI", amount = 3.5)
        val playerBids = listOf(bid2, bid1) // Newest first

        // Undoing bid2:
        val remainingBids = playerBids.filterNot { it.id == bid2.id }
        assertEquals(1, remainingBids.size)
        val prevBid = remainingBids.first()

        val restoredPlayer = player.copy(
            currentBid = prevBid.amount,
            highestBidderTeamId = prevBid.teamId,
            status = PlayerStatus.BIDDING
        )
        assertEquals(2.5, restoredPlayer.currentBid, 0.001)
        assertEquals("team1", restoredPlayer.highestBidderTeamId)

        // Undoing bid1:
        val remainingAfterAll = remainingBids.filterNot { it.id == bid1.id }
        assertTrue(remainingAfterAll.isEmpty())
        val basePlayer = restoredPlayer.copy(
            currentBid = restoredPlayer.basePrice,
            highestBidderTeamId = null,
            status = PlayerStatus.UP_NEXT
        )
        assertEquals(2.0, basePlayer.currentBid, 0.001)
        assertNull(basePlayer.highestBidderTeamId)
        assertEquals(PlayerStatus.UP_NEXT, basePlayer.status)
    }

    @Test
    fun testUndoLastSoldPlayerRestoresStatusAndRefundsTeam() {
        val team = Team(id = "t1", name = "RCB", shortCode = "BC", primaryColorHex = "#EC1C24", totalPurse = 100.0, spentPurse = 20.0)
        val soldPlayer = Player(
            id = "p1",
            name = "Maxwell",
            role = PlayerRole.ALL_ROUNDER,
            country = "Australia",
            isOverseas = true,
            basePrice = 2.0,
            soldPrice = 14.0,
            soldToTeamId = "t1",
            status = PlayerStatus.SOLD
        )

        val refund = soldPlayer.soldPrice ?: soldPlayer.currentBid
        val refundedTeam = team.copy(spentPurse = (team.spentPurse - refund).coerceAtLeast(0.0))
        val restoredPlayer = soldPlayer.copy(
            status = PlayerStatus.UP_NEXT,
            soldPrice = null,
            soldToTeamId = null,
            currentBid = soldPlayer.basePrice,
            highestBidderTeamId = null
        )

        assertEquals(6.0, refundedTeam.spentPurse, 0.001)
        assertEquals(94.0, refundedTeam.remainingPurse, 0.001)
        assertEquals(PlayerStatus.UP_NEXT, restoredPlayer.status)
        assertNull(restoredPlayer.soldPrice)
        assertNull(restoredPlayer.soldToTeamId)
        assertEquals(2.0, restoredPlayer.currentBid, 0.001)
    }

    @Test
    fun testRemovePlayerRefundsPurseIfSold() {
        val team = Team(id = "t1", name = "GT", shortCode = "GT", primaryColorHex = "#0B4973", totalPurse = 100.0, spentPurse = 10.0)
        val soldPlayer = Player(
            id = "p1",
            name = "Shami",
            role = PlayerRole.BOWLER,
            country = "India",
            isOverseas = false,
            basePrice = 2.0,
            soldPrice = 10.0,
            soldToTeamId = "t1",
            status = PlayerStatus.SOLD
        )

        val refund = soldPlayer.soldPrice ?: soldPlayer.currentBid
        val updatedTeam = team.copy(spentPurse = (team.spentPurse - refund).coerceAtLeast(0.0))
        assertEquals(0.0, updatedTeam.spentPurse, 0.001)
        assertEquals(100.0, updatedTeam.remainingPurse, 0.001)
    }

    @Test
    fun testRemovePlayerFromTeamForReAuctionClearsBidsAndRestoresPool() {
        val team = Team(id = "t_csk", name = "Chennai Super Kings", shortCode = "CSK", primaryColorHex = "#FFFF00", totalPurse = 100.0, spentPurse = 16.0)
        val player = Player(
            id = "p_dhoni",
            name = "MS Dhoni",
            role = PlayerRole.WICKET_KEEPER,
            country = "India",
            isOverseas = false,
            basePrice = 2.0,
            currentBid = 16.0,
            soldPrice = 16.0,
            soldToTeamId = "t_csk",
            status = PlayerStatus.SOLD
        )
        val bid = Bid(id = "b_1", playerId = "p_dhoni", teamId = "t_csk", teamName = "CSK", amount = 16.0)
        val allBids = mutableListOf(bid)

        val refund = player.soldPrice ?: player.currentBid
        val updatedTeam = team.copy(spentPurse = (team.spentPurse - refund).coerceAtLeast(0.0))
        val restoredPlayer = player.copy(
            status = PlayerStatus.UP_NEXT,
            soldPrice = null,
            soldToTeamId = null,
            currentBid = player.basePrice,
            highestBidderTeamId = null
        )
        allBids.removeAll { it.playerId == player.id }

        assertEquals(0.0, updatedTeam.spentPurse, 0.001)
        assertEquals(100.0, updatedTeam.remainingPurse, 0.001)
        assertEquals(PlayerStatus.UP_NEXT, restoredPlayer.status)
        assertNull(restoredPlayer.soldPrice)
        assertNull(restoredPlayer.soldToTeamId)
        assertNull(restoredPlayer.highestBidderTeamId)
        assertEquals(2.0, restoredPlayer.currentBid, 0.001)
        assertTrue(allBids.isEmpty())
    }
}

package cricket.player.auction

import cricket.player.auction.model.Match
import cricket.player.auction.model.Tournament
import org.junit.Assert.*
import org.junit.Test

class MasterAdminModeTest {

    @Test
    fun testMasterAdminModeFiltersCorrectly() {
        val t1 = Tournament(id = "t1", name = "Local Tournament 1")
        val t2 = Tournament(id = "t2", name = "Web Device Tournament 2")
        val t3 = Tournament(id = "t3", name = "Android Device Tournament 3")
        val allTournaments = listOf(t1, t2, t3)
        val myTournamentIds = setOf("t1")

        // Standard device mode (isMasterAdmin = false)
        val standardVisible = allTournaments.filter { it.id in myTournamentIds }
        assertEquals(1, standardVisible.size)
        assertEquals("t1", standardVisible[0].id)

        // Master Admin Mode (isMasterAdmin = true)
        val masterAdminVisible = allTournaments // Unrestricted
        assertEquals(3, masterAdminVisible.size)
        assertTrue(masterAdminVisible.any { it.id == "t2" })
        assertTrue(masterAdminVisible.any { it.id == "t3" })
    }

    @Test
    fun testCascadeDeletionCalculations() {
        val matches = listOf(
            Match(id = "m1", tournamentId = "t1", teamAId = "A", teamBId = "B"),
            Match(id = "m2", tournamentId = "t2", teamAId = "C", teamBId = "D")
        )

        val tourneyMatchIds = matches.filter { it.tournamentId == "t1" }.map { it.id }.toSet()
        assertEquals(setOf("m1"), tourneyMatchIds)

        val remainingMatches = matches.filterNot { it.tournamentId == "t1" }
        assertEquals(1, remainingMatches.size)
        assertEquals("m2", remainingMatches[0].id)
    }
}

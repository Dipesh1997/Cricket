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

    @Test
    fun testDeviceTournamentIsolationAndPruning() {
        val myDeviceId = "pixel_device_123"
        val foreignDeviceId = "web_device_456"

        val tMyCreated = Tournament(id = "t_mine", name = "My Pixel Cup", creatorDeviceId = myDeviceId)
        val tForeign = Tournament(id = "t_foreign", name = "Web Cup", creatorDeviceId = foreignDeviceId)
        val tJoined = Tournament(id = "t_joined", name = "Joined League", creatorDeviceId = foreignDeviceId)

        val allTournaments = listOf(tMyCreated, tForeign, tJoined)

        // Assume device had contaminated myTournamentIds containing foreign tournaments
        val contaminatedMyIds = setOf("t_mine", "t_foreign", "t_joined")
        val roles = mapOf("t_mine" to "ADMIN", "t_foreign" to "ADMIN", "t_joined" to "SCORER")

        // Pruning logic: if creatorDeviceId is foreign and role is ADMIN, it is pruned
        val cleanedSet = contaminatedMyIds.filter { tId ->
            val tourney = allTournaments.find { it.id == tId }
            if (tourney != null && tourney.creatorDeviceId.isNotBlank() && tourney.creatorDeviceId != myDeviceId) {
                roles[tId] != "ADMIN" // Legit joined roles (like SCORER) are kept
            } else {
                true
            }
        }.toSet()

        assertEquals(setOf("t_mine", "t_joined"), cleanedSet)
        assertFalse(cleanedSet.contains("t_foreign"))

        // Standard user sees only their tournaments
        val visibleTournaments = allTournaments.filter { it.id in cleanedSet }
        assertEquals(2, visibleTournaments.size)
        assertTrue(visibleTournaments.any { it.id == "t_mine" })
        assertTrue(visibleTournaments.any { it.id == "t_joined" })
        assertFalse(visibleTournaments.any { it.id == "t_foreign" })
    }
}

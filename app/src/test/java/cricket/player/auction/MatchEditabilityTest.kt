package cricket.player.auction

import cricket.player.auction.model.Match
import cricket.player.auction.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test

class MatchEditabilityTest {

    private fun isMatchEditable(match: Match, allMatches: List<Match>): Boolean {
        val tourneyMatches = allMatches.filter { it.tournamentId == match.tournamentId }
        val matchIndex = tourneyMatches.indexOfFirst { it.id == match.id }
        if (matchIndex == -1) return true
        val subsequentMatches = tourneyMatches.drop(matchIndex + 1)
        return subsequentMatches.none { it.status == MatchStatus.COMPLETED }
    }

    @Test
    fun testMatchEditableUntilNextMatchCompleted() {
        val m1 = Match(id = "m1", tournamentId = "t1", teamAId = "tA", teamBId = "tB", status = MatchStatus.COMPLETED)
        val m2 = Match(id = "m2", tournamentId = "t1", teamAId = "tB", teamBId = "tC", status = MatchStatus.LIVE_INNINGS_1)
        val m3 = Match(id = "m3", tournamentId = "t1", teamAId = "tA", teamBId = "tC", status = MatchStatus.SCHEDULED)

        val matchesList = listOf(m1, m2, m3)

        // Match 1 is COMPLETED, but subsequent matches (m2, m3) are not completed -> M1 must be EDITABLE
        assertTrue("Match 1 should be editable because subsequent matches are not completed", isMatchEditable(m1, matchesList))
        // Match 2 is LIVE -> M2 must be editable
        assertTrue("Match 2 should be editable", isMatchEditable(m2, matchesList))

        // Now Match 2 gets completed!
        val m2Completed = m2.copy(status = MatchStatus.COMPLETED)
        val updatedMatches = listOf(m1, m2Completed, m3)

        // Match 1 has subsequent match m2 completed -> M1 must NOT be editable (LOCKED)
        assertFalse("Match 1 should now be LOCKED because Match 2 is completed", isMatchEditable(m1, updatedMatches))
        // Match 2 has subsequent match m3 not completed -> M2 must be EDITABLE
        assertTrue("Match 2 should be editable because Match 3 is not completed", isMatchEditable(m2Completed, updatedMatches))

        // Now Match 3 gets completed!
        val m3Completed = m3.copy(status = MatchStatus.COMPLETED)
        val finalMatches = listOf(m1, m2Completed, m3Completed)

        // Match 1 and Match 2 locked, Match 3 editable
        assertFalse("Match 1 should be locked", isMatchEditable(m1, finalMatches))
        assertFalse("Match 2 should be locked", isMatchEditable(m2Completed, finalMatches))
        assertTrue("Match 3 should be editable", isMatchEditable(m3Completed, finalMatches))
    }
}

package cricket.player.auction.util

import android.content.Context
import android.content.Intent
import cricket.player.auction.model.*

object CricketShareHelper {

    fun shareTeam(context: Context, team: Team, squad: List<Player>) {
        val text = buildString {
            appendLine("🏏 *${team.name} (${team.shortCode})*")
            if (team.captainEmail != null) {
                appendLine("👑 Captain: ${team.captainEmail}")
            }
            appendLine("💰 Purse: Spent ${team.spentPurse.formatIplCurrency()} | Remaining ${team.remainingPurse.formatIplCurrency()}")
            appendLine("👥 Squad (${squad.size}/${team.maxSlots} Slots, ${squad.count { it.isOverseas }}/${team.maxOverseas} Overseas):")
            if (squad.isEmpty()) {
                appendLine("   (No players acquired yet)")
            } else {
                squad.forEachIndexed { i, p ->
                    val priceStr = (p.soldPrice ?: p.currentBid).formatIplCurrency()
                    val overseasTag = if (p.isOverseas) " ✈️" else ""
                    appendLine("   ${i + 1}. ${p.name} (${p.role.label}) - $priceStr$overseasTag")
                }
            }
            appendLine()
            appendLine("Shared via Cricket Auction Arena 🏆")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${team.name} Squad"))
    }

    fun shareAllTeams(context: Context, tournamentName: String, teams: List<Team>, players: List<Player>) {
        val text = buildString {
            appendLine("🏆 *${tournamentName.ifBlank { "Cricket Tournament" }} - All Teams & Squads* 🏆")
            appendLine()
            if (teams.isEmpty()) {
                appendLine("No teams registered yet.")
            } else {
                teams.forEachIndexed { idx, team ->
                    val squad = players.filter { it.soldToTeamId == team.id }
                    appendLine("🏏 *${idx + 1}. ${team.name} (${team.shortCode})*")
                    if (team.captainEmail != null) {
                        appendLine("   👑 Captain: ${team.captainEmail}")
                    }
                    appendLine("   💰 Spent: ${team.spentPurse.formatIplCurrency()} | Left: ${team.remainingPurse.formatIplCurrency()}")
                    val squadNames = if (squad.isEmpty()) "None" else squad.joinToString(", ") { "${it.name} (${it.role.label})" }
                    appendLine("   👥 Squad (${squad.size}/${team.maxSlots}): $squadNames")
                    appendLine()
                }
            }
            appendLine("Shared via Cricket Auction Arena 🏆")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share All Teams"))
    }

    fun shareMatchScorecard(
        context: Context,
        match: Match,
        teamA: Team?,
        teamB: Team?,
        ballRecords: List<BallRecord> = emptyList(),
        players: List<Player> = emptyList()
    ) {
        val tAName = teamA?.name ?: "Team A"
        val tBName = teamB?.name ?: "Team B"
        val text = buildString {
            appendLine("🏏 *MATCH SCORECARD: $tAName vs $tBName* 📊")
            val statusStr = if (match.status == MatchStatus.COMPLETED) "COMPLETED 🏆" else "LIVE 🔴 (Innings ${match.currentInnings})"
            appendLine("📌 Status: $statusStr")
            appendLine("------------------------------")
            appendLine("• *$tAName*: ${match.teamARuns}/${match.teamAWickets} (${match.teamAOversBatted} ov)")
            appendLine("• *$tBName*: ${match.teamBRuns}/${match.teamBWickets} (${match.teamBOversBatted} ov)")
            if (match.resultSummary.isNotBlank()) {
                appendLine("🏆 *Result*: ${match.resultSummary}")
            }
            appendLine("------------------------------")

            val mBalls = ballRecords.filter { it.matchId == match.id }
            if (mBalls.isNotEmpty()) {
                val inn1Balls = mBalls.filter { it.innings == 1 }
                val inn2Balls = mBalls.filter { it.innings == 2 }

                val inn1Runs = inn1Balls.filter { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                    .groupBy { it.strikerId }
                    .mapValues { (_, bList) -> bList.sumOf { it.runsScored } }
                val topBatterInn1 = inn1Runs.maxByOrNull { it.value }
                if (topBatterInn1 != null) {
                    val p = players.find { it.id == topBatterInn1.key }
                    if (p != null) appendLine("⭐ $tAName Top Scorer: ${p.name} - ${topBatterInn1.value} runs")
                }

                val inn2Runs = inn2Balls.filter { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                    .groupBy { it.strikerId }
                    .mapValues { (_, bList) -> bList.sumOf { it.runsScored } }
                val topBatterInn2 = inn2Runs.maxByOrNull { it.value }
                if (topBatterInn2 != null) {
                    val p = players.find { it.id == topBatterInn2.key }
                    if (p != null) appendLine("⭐ $tBName Top Scorer: ${p.name} - ${topBatterInn2.value} runs")
                }
            }
            appendLine()
            appendLine("Shared via Cricket Auction Arena 🏆")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Match Scorecard"))
    }
}

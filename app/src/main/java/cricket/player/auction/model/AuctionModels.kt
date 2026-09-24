package cricket.player.auction.model

enum class PlayerRole(val label: String) {
    BATSMAN("Batsman"),
    BOWLER("Bowler"),
    ALL_ROUNDER("All-Rounder"),
    WICKET_KEEPER("Wicket Keeper")
}

enum class PlayerStatus {
    UP_NEXT,
    BIDDING,
    SOLD,
    UNSOLD
}

enum class UserRole(val label: String) {
    ADMIN_AUCTIONEER("Auctioneer / Admin"),
    TEAM_CAPTAIN("Team Captain / Owner"),
    SCORER("Live Scorer"),
    SPECTATOR("Spectator / Fan")
}

enum class MatchStatus(val label: String) {
    SCHEDULED("Scheduled"),
    LIVE_INNINGS_1("Innings 1 Live"),
    LIVE_INNINGS_2("Innings 2 Live"),
    COMPLETED("Completed")
}

enum class ExtraType {
    NONE,
    WIDE,
    NO_BALL,
    BYE,
    LEG_BYE
}

enum class DismissalType(val label: String) {
    BOWLED("Bowled"),
    CAUGHT("Caught"),
    RUN_OUT("Run Out"),
    LBW("LBW"),
    STUMPED("Stumped"),
    HIT_WICKET("Hit Wicket"),
    RETIRED_HURT("Retired Hurt")
}

data class Tournament(
    val id: String,
    val name: String,
    val defaultPurse: Double = 100.0, // in Crores
    val maxSlots: Int = 25,
    val maxOverseas: Int = 8,
    val driveFileUrl: String = "",
    val scorerInviteCode: String = "",
    val adminCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Player(
    val id: String,
    val tournamentId: String = "t1",
    val name: String,
    val role: PlayerRole,
    val country: String,
    val isOverseas: Boolean,
    val basePrice: Double, // in Crores e.g. 2.0
    val currentBid: Double = basePrice,
    val highestBidderTeamId: String? = null,
    val status: PlayerStatus = PlayerStatus.UP_NEXT,
    val soldPrice: Double? = null,
    val soldToTeamId: String? = null,
    val imageUrl: String = "",
    val stats: String = "",
    val setName: String = "Set 1",
    val isFixedSet: Boolean = false
)

data class Team(
    val id: String,
    val tournamentId: String = "t1",
    val name: String,
    val shortCode: String,
    val primaryColorHex: String,
    val logoUrl: String = "",
    val totalPurse: Double = 100.0, // in Crores
    val spentPurse: Double = 0.0,
    val captainEmail: String? = null,
    val inviteCode: String = "",
    val maxSlots: Int = 25,
    val maxOverseas: Int = 8
) {
    val remainingPurse: Double
        get() = (totalPurse - spentPurse).coerceAtLeast(0.0)
}

data class Bid(
    val id: String,
    val playerId: String,
    val teamId: String,
    val teamName: String,
    val amount: Double, // in Crores
    val timestamp: Long = System.currentTimeMillis(),
    val bidderEmail: String? = null
)

data class CaptainInvite(
    val code: String,
    val teamId: String,
    val invitedEmail: String? = null,
    val isClaimed: Boolean = false
)

data class UserProfile(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val role: UserRole = UserRole.SPECTATOR,
    val assignedTeamId: String? = null
)

// --- Match Scoring Data Models ---
data class Match(
    val id: String,
    val tournamentId: String,
    val teamAId: String,
    val teamBId: String,
    val totalOvers: Int = 20,
    val isLastPlayerBattingAllowed: Boolean = true, // Rule toggle for solo player batting!
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val currentInnings: Int = 1,
    val teamARuns: Int = 0,
    val teamAWickets: Int = 0,
    val teamAOversBatted: Double = 0.0,
    val teamBRuns: Int = 0,
    val teamBWickets: Int = 0,
    val teamBOversBatted: Double = 0.0,
    val winnerTeamId: String? = null,
    val tossWinnerTeamId: String? = null,
    val tossDecision: String? = null,
    val currentStrikerId: String? = null,
    val currentNonStrikerId: String? = null,
    val currentBowlerId: String? = null,
    val matchDate: Long = System.currentTimeMillis(),
    val resultSummary: String = ""
)

data class BallRecord(
    val id: String,
    val matchId: String,
    val innings: Int,
    val overNumber: Int,
    val ballNumber: Int,
    val strikerId: String,
    val nonStrikerId: String?,
    val bowlerId: String,
    val runsScored: Int,
    val extraType: ExtraType = ExtraType.NONE,
    val extraRuns: Int = 0,
    val isWicket: Boolean = false,
    val dismissalType: DismissalType? = null,
    val dismissedPlayerId: String? = null,
    val fielderId: String? = null,
    val commentary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// --- Player Career & CricHeroes MVP Stats Models ---
data class PlayerCareerStats(
    val playerId: String,
    val playerName: String,
    val playerRole: PlayerRole,
    val photoUrl: String = "",
    val matches: Int = 0,
    val inningsBatted: Int = 0,
    val runsScored: Int = 0,
    val ballsFaced: Int = 0,
    val highestScore: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val oversBowled: Double = 0.0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wicketsTaken: Int = 0,
    val bestBowlingWickets: Int = 0,
    val bestBowlingRuns: Int = 0,
    val threeWicketHauls: Int = 0,
    val fiveWicketHauls: Int = 0,
    val catches: Int = 0,
    val stumpings: Int = 0,
    val runOuts: Int = 0,
    val mvpPoints: Double = 0.0
) {
    val battingAverage: Double
        get() = if (inningsBatted > 0) runsScored.toDouble() / inningsBatted else 0.0

    val strikeRate: Double
        get() = if (ballsFaced > 0) (runsScored.toDouble() / ballsFaced) * 100.0 else 0.0

    val economyRate: Double
        get() = if (oversBowled > 0.0) runsConceded.toDouble() / oversBowled else 0.0

    val bowlingAverage: Double
        get() = if (wicketsTaken > 0) runsConceded.toDouble() / wicketsTaken else 0.0
}

data class PointsTableEntry(
    val teamId: String,
    val teamName: String,
    val shortCode: String,
    val primaryColorHex: String,
    val logoUrl: String = "",
    val matchesPlayed: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val tied: Int = 0,
    val points: Int = 0,
    val runsScored: Int = 0,
    val oversBatted: Double = 0.0,
    val runsConceded: Int = 0,
    val oversBowled: Double = 0.0
) {
    val netRunRate: Double
        get() {
            val forRate = if (oversBatted > 0.0) runsScored.toDouble() / oversBatted else 0.0
            val againstRate = if (oversBowled > 0.0) runsConceded.toDouble() / oversBowled else 0.0
            return forRate - againstRate
        }
}

// CricHeroes MVP Calculation Formula
fun calculateCricHeroesMvpPoints(
    runs: Int,
    fours: Int,
    sixes: Int,
    ballsFaced: Int,
    wickets: Int,
    bowledOrLbwCount: Int = 0,
    dotBalls: Int = 0,
    maidens: Int = 0,
    oversBowled: Double = 0.0,
    runsConceded: Int = 0,
    catches: Int = 0,
    stumpings: Int = 0,
    runOuts: Int = 0
): Double {
    var points = 0.0

    // Batting Points
    points += runs * 1.0
    points += fours * 1.0
    points += sixes * 2.0
    if (runs >= 100) points += 32.0
    else if (runs >= 50) points += 16.0
    else if (runs >= 30) points += 8.0

    if (ballsFaced >= 10) {
        val sr = (runs.toDouble() / ballsFaced) * 100.0
        if (sr >= 200) points += 15.0
        else if (sr >= 150) points += 10.0
        else if (sr < 80) points -= 5.0
    }

    // Bowling Points
    points += wickets * 20.0
    points += bowledOrLbwCount * 8.0
    points += dotBalls * 1.0
    points += maidens * 8.0
    if (wickets >= 5) points += 32.0
    else if (wickets >= 3) points += 16.0

    if (oversBowled >= 2.0) {
        val econ = runsConceded.toDouble() / oversBowled
        if (econ <= 4.0) points += 15.0
        else if (econ <= 6.0) points += 10.0
        else if (econ > 11.0) points -= 5.0
    }

    // Fielding Points
    points += catches * 8.0
    points += stumpings * 12.0
    points += runOuts * 12.0

    return points
}

// Helper to format currency in Lakhs and Crores
fun Double.formatIplCurrency(): String {
    return if (this >= 1.0) {
        val rounded = String.format("%.2f", this)
        "₹$rounded Cr"
    } else {
        val lakhs = (this * 100).toInt()
        "₹$lakhs Lakh"
    }
}

// Data Model Sanitization Extensions (Guarantees safety after Gson deserialization)
fun Tournament.sanitized(): Tournament {
    val cleanId = id ?: "t_${System.currentTimeMillis()}"
    val cleanAdminCode = if (adminCode.isNullOrBlank()) "ADM" + cleanId.takeLast(4).uppercase() else adminCode
    return this.copy(
        id = cleanId,
        name = name ?: "Tournament",
        defaultPurse = if (defaultPurse <= 0) 100.0 else defaultPurse,
        maxSlots = if (maxSlots <= 0) 25 else maxSlots,
        maxOverseas = if (maxOverseas < 0) 8 else maxOverseas,
        driveFileUrl = driveFileUrl ?: "",
        scorerInviteCode = if (scorerInviteCode.isNullOrBlank()) "SC" + cleanId.takeLast(4).uppercase() else scorerInviteCode,
        adminCode = cleanAdminCode
    )
}

fun Player.sanitized(): Player {
    return this.copy(
        id = id ?: "p_${System.currentTimeMillis()}",
        tournamentId = tournamentId ?: "t1",
        name = name ?: "Player",
        role = role ?: PlayerRole.BATSMAN,
        country = country ?: "India",
        status = status ?: PlayerStatus.UP_NEXT,
        imageUrl = imageUrl ?: "",
        stats = stats ?: "",
        setName = setName ?: "Set 1"
    )
}

fun Team.sanitized(): Team {
    return this.copy(
        id = id ?: "team_${System.currentTimeMillis()}",
        tournamentId = tournamentId ?: "t1",
        name = name ?: "Team",
        shortCode = shortCode ?: "TM",
        primaryColorHex = primaryColorHex ?: "#1976D2",
        logoUrl = logoUrl ?: "",
        inviteCode = inviteCode ?: ""
    )
}

fun Bid.sanitized(): Bid {
    return this.copy(
        id = id ?: "bid_${System.currentTimeMillis()}",
        playerId = playerId ?: "",
        teamId = teamId ?: "",
        teamName = teamName ?: ""
    )
}

fun CaptainInvite.sanitized(): CaptainInvite {
    return this.copy(
        code = code ?: "",
        teamId = teamId ?: ""
    )
}

fun Match.sanitized(): Match {
    return this.copy(
        id = id ?: "m_${System.currentTimeMillis()}",
        tournamentId = tournamentId ?: "t1",
        teamAId = teamAId ?: "",
        teamBId = teamBId ?: "",
        status = status ?: MatchStatus.SCHEDULED
    )
}

fun BallRecord.sanitized(): BallRecord {
    return this.copy(
        id = id ?: "b_${System.currentTimeMillis()}",
        matchId = matchId ?: "",
        strikerId = strikerId ?: "",
        bowlerId = bowlerId ?: "",
        extraType = extraType ?: ExtraType.NONE,
        commentary = commentary ?: ""
    )
}


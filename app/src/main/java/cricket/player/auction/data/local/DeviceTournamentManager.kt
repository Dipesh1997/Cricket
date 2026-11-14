package cricket.player.auction.data.local

import android.content.Context
import android.content.SharedPreferences

class DeviceTournamentManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cricket_device_tournaments_prefs", Context.MODE_PRIVATE)

    fun getMyTournamentIds(): Set<String> {
        val set = prefs.getStringSet("my_tournament_ids", null)
        return set?.toSet() ?: emptySet()
    }

    fun addTournament(tournamentId: String, role: String = "ADMIN") {
        if (tournamentId.isBlank()) return
        val current = getMyTournamentIds().toMutableSet()
        current.add(tournamentId)
        prefs.edit()
            .putStringSet("my_tournament_ids", current)
            .putString("role_$tournamentId", role)
            .apply()
    }

    fun removeTournament(tournamentId: String) {
        val current = getMyTournamentIds().toMutableSet()
        current.remove(tournamentId)
        prefs.edit()
            .putStringSet("my_tournament_ids", current)
            .remove("role_$tournamentId")
            .apply()
    }

    fun getTournamentRole(tournamentId: String): String {
        return prefs.getString("role_$tournamentId", "ADMIN") ?: "ADMIN"
    }

    fun hasTournament(tournamentId: String): Boolean {
        return getMyTournamentIds().contains(tournamentId)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}


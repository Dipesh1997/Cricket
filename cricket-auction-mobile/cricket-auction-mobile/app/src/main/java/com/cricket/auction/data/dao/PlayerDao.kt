package com.cricket.auction.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cricket.auction.data.model.PlayerAuctionStatus
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.PlayerRole
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY orderIndex ASC, id ASC")
    fun getPlayersForTournament(tournamentId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY orderIndex ASC, id ASC")
    suspend fun getPlayersForTournamentSync(tournamentId: Long): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    fun getPlayerById(id: Long): Flow<PlayerEntity?>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerByIdSync(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = :status ORDER BY orderIndex ASC, id ASC")
    fun getPlayersByStatus(tournamentId: Long, status: PlayerAuctionStatus): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = :status ORDER BY orderIndex ASC, id ASC")
    suspend fun getPlayersByStatusSync(tournamentId: Long, status: PlayerAuctionStatus): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE soldToTeamId = :teamId ORDER BY soldPrice DESC")
    fun getPlayersForTeam(teamId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE soldToTeamId = :teamId ORDER BY soldPrice DESC")
    suspend fun getPlayersForTeamSync(teamId: Long): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = 'UPCOMING' ORDER BY orderIndex ASC, id ASC LIMIT 1")
    suspend fun getNextUpcomingPlayer(tournamentId: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = 'UPCOMING' ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUpcomingPlayer(tournamentId: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = 'UNSOLD' ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnsoldPlayer(tournamentId: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = 'SOLD' ORDER BY soldPrice DESC LIMIT :limit")
    fun getTopBuys(tournamentId: Long, limit: Int = 10): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId AND status = 'SOLD' ORDER BY soldPrice DESC LIMIT :limit")
    suspend fun getTopBuysSync(tournamentId: Long, limit: Int = 10): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Update
    suspend fun update(player: PlayerEntity)

    @Query("UPDATE players SET status = :status, soldToTeamId = :teamId, soldPrice = :soldPrice WHERE id = :playerId")
    suspend fun markSold(playerId: Long, teamId: Long, soldPrice: Long, status: PlayerAuctionStatus = PlayerAuctionStatus.SOLD)

    @Query("UPDATE players SET status = 'UNSOLD', soldToTeamId = NULL, soldPrice = NULL WHERE id = :playerId")
    suspend fun markUnsold(playerId: Long)

    @Query("UPDATE players SET status = 'UPCOMING', soldToTeamId = NULL, soldPrice = NULL WHERE id = :playerId")
    suspend fun resetPlayer(playerId: Long)

    @Query("UPDATE players SET status = 'UPCOMING', soldToTeamId = NULL, soldPrice = NULL WHERE tournamentId = :tournamentId")
    suspend fun resetAllPlayersInTournament(tournamentId: Long)

    @Query("UPDATE players SET status = 'UPCOMING' WHERE tournamentId = :tournamentId AND status = 'UNSOLD'")
    suspend fun resetUnsoldPlayersToUpcoming(tournamentId: Long)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deleteById(id: Long)
}

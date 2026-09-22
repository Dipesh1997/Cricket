package com.cricket.auction.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cricket.auction.data.model.TeamEntity
import com.cricket.auction.data.model.TeamWithPlayers
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams WHERE tournamentId = :tournamentId ORDER BY name ASC")
    fun getTeamsForTournament(tournamentId: Long): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE tournamentId = :tournamentId ORDER BY name ASC")
    suspend fun getTeamsForTournamentSync(tournamentId: Long): List<TeamEntity>

    @Query("SELECT * FROM teams WHERE id = :id")
    fun getTeamById(id: Long): Flow<TeamEntity?>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamByIdSync(id: Long): TeamEntity?

    @Transaction
    @Query("SELECT * FROM teams WHERE id = :id")
    fun getTeamWithPlayers(id: Long): Flow<TeamWithPlayers?>

    @Transaction
    @Query("SELECT * FROM teams WHERE tournamentId = :tournamentId ORDER BY name ASC")
    fun getAllTeamsWithPlayers(tournamentId: Long): Flow<List<TeamWithPlayers>>

    @Transaction
    @Query("SELECT * FROM teams WHERE tournamentId = :tournamentId ORDER BY name ASC")
    suspend fun getAllTeamsWithPlayersSync(tournamentId: Long): List<TeamWithPlayers>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: TeamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Update
    suspend fun update(team: TeamEntity)

    @Query("UPDATE teams SET remainingPurse = :remainingPurse WHERE id = :teamId")
    suspend fun updateRemainingPurse(teamId: Long, remainingPurse: Long)

    @Delete
    suspend fun delete(team: TeamEntity)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteById(id: Long)
}

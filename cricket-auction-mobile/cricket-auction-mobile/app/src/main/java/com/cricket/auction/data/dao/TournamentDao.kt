package com.cricket.auction.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cricket.auction.data.model.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun getTournamentById(id: Long): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentByIdSync(id: Long): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tournament: TournamentEntity): Long

    @Update
    suspend fun update(tournament: TournamentEntity)

    @Delete
    suspend fun delete(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

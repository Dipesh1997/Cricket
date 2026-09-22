package com.cricket.auction.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cricket.auction.data.model.BidHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BidHistoryDao {

    @Query("SELECT * FROM bid_history WHERE playerId = :playerId ORDER BY timestamp DESC, id DESC")
    fun getBidsForPlayer(playerId: Long): Flow<List<BidHistoryEntity>>

    @Query("SELECT * FROM bid_history WHERE playerId = :playerId ORDER BY timestamp DESC, id DESC")
    suspend fun getBidsForPlayerSync(playerId: Long): List<BidHistoryEntity>

    @Query("SELECT * FROM bid_history WHERE tournamentId = :tournamentId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBidsForTournament(tournamentId: Long, limit: Int = 20): Flow<List<BidHistoryEntity>>

    @Query("SELECT * FROM bid_history WHERE playerId = :playerId ORDER BY id DESC LIMIT 1")
    suspend fun getLastBidForPlayer(playerId: Long): BidHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bid: BidHistoryEntity): Long

    @Query("DELETE FROM bid_history WHERE id = :bidId")
    suspend fun deleteBid(bidId: Long)

    @Query("DELETE FROM bid_history WHERE playerId = :playerId")
    suspend fun deleteBidsForPlayer(playerId: Long)

    @Query("DELETE FROM bid_history WHERE tournamentId = :tournamentId")
    suspend fun clearAllBidsInTournament(tournamentId: Long)
}

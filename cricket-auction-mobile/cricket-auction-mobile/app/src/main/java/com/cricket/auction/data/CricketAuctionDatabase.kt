package com.cricket.auction.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cricket.auction.data.dao.BidHistoryDao
import com.cricket.auction.data.dao.PlayerDao
import com.cricket.auction.data.dao.TeamDao
import com.cricket.auction.data.dao.TournamentDao
import com.cricket.auction.data.model.BidHistoryEntity
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.TeamEntity
import com.cricket.auction.data.model.TournamentEntity

@Database(
    entities = [
        TournamentEntity::class,
        TeamEntity::class,
        PlayerEntity::class,
        BidHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CricketAuctionDatabase : RoomDatabase() {

    abstract fun tournamentDao(): TournamentDao
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun bidHistoryDao(): BidHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CricketAuctionDatabase? = null

        fun getInstance(context: Context): CricketAuctionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketAuctionDatabase::class.java,
                    "cricket_auction.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

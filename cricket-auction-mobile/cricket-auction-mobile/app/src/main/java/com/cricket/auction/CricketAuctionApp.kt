package com.cricket.auction

import android.app.Application
import com.cricket.auction.data.CricketAuctionDatabase
import com.cricket.auction.data.repository.CricketAuctionRepository

class CricketAuctionApp : Application() {

    lateinit var database: CricketAuctionDatabase
        private set

    lateinit var repository: CricketAuctionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = CricketAuctionDatabase.getInstance(this)
        repository = CricketAuctionRepository(database)
    }
}

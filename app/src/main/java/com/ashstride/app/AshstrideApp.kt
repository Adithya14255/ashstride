package com.ashstride.app

import android.app.Application
import androidx.room.Room
import com.ashstride.app.data.db.AshDatabase
import com.ashstride.app.sync.HealthSync
import com.ashstride.app.sync.SyncScheduler

class AshstrideApp : Application() {
    val db: AshDatabase by lazy {
        Room.databaseBuilder(this, AshDatabase::class.java, "ashstride.db").build()
    }
    val healthSync: HealthSync by lazy { HealthSync(this, db) }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedule(this)
    }
}

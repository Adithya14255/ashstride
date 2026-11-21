package com.ashstride.app

import android.app.Application
import androidx.room.Room
import com.ashstride.app.data.db.AshDatabase

class AshstrideApp : Application() {
    val db: AshDatabase by lazy {
        Room.databaseBuilder(this, AshDatabase::class.java, "ashstride.db").build()
    }
}

package com.example.mapactivityapp.roomdb

import androidx.room.Database
import com.example.mapactivityapp.model.Place


@Database(entities = arrayOf(Place::class), version = 1)
abstract class PlaceDatabase : androidx.room.RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}
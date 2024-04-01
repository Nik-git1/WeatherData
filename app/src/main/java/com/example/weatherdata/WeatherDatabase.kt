package com.example.weatherdata

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

@Database(entities = [WeatherData::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun weatherDataDao(): WeatherDataDao

    companion object {
        @Volatile
        private var instance: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {

            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                ).build().also { instance = it }
            }
        }
    }
}

package com.example.weatherdata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDataDao {
    @Insert
    suspend fun insert(weatherData: WeatherData)

    @Query("SELECT * FROM weather_data")
     fun getAllWeatherData(): Flow<List<WeatherData>>
}



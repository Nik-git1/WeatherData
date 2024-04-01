package com.example.weatherdata

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_data")
data class WeatherData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val maxTemp: Double,
    val minTemp: Double
)

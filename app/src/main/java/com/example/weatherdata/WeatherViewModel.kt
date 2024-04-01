package com.example.weatherdata

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WeatherViewModel(private  val weatherDataDao: WeatherDataDao) :ViewModel(){
    val allWeather : LiveData<List<WeatherData>> = weatherDataDao.getAllWeatherData().asLiveData()

    fun insert(weatherData: WeatherData) = viewModelScope.launch {
        weatherDataDao.insert(weatherData)
    }
}
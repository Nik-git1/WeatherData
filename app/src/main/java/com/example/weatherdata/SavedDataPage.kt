package com.example.weatherdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDataPage(weatherViewModel: WeatherViewModel, navigateBack: () -> Unit) {
    val allWeather by weatherViewModel.allWeather.observeAsState(listOf())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Saved Data") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },content = { padding ->
            Column( modifier = Modifier.padding(padding)
    ) {
        if (allWeather.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(allWeather) { weatherData ->
                    Text(
                        text = "Date: ${weatherData.date}, Max Temp: ${weatherData.maxTemp}, Min Temp: ${weatherData.minTemp}",
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            Text(
                text = "No saved data available",
                modifier = Modifier.fillMaxSize(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
})}

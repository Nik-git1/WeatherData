package com.example.weatherdata

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var weatherDatabase: WeatherDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weatherDatabase = WeatherDatabase.getDatabase(applicationContext)
        weatherViewModel = WeatherViewModel(weatherDatabase.weatherDataDao())
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "main") {
                composable("main") {
                    WeatherApp(this@MainActivity, weatherViewModel, navController)
                    // Pass the MainActivity reference explicitly
                }
                composable("savedData") {
                    SavedDataPage(weatherViewModel) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherApp(context: Context, weatherViewModel: WeatherViewModel, navController: NavHostController) {
    // Define your state variables and other logic here
    val locationPermissionState = remember { mutableStateOf(false) }
    var maxTemp by remember { mutableStateOf<Double?>(null) }
    var minTemp by remember { mutableStateOf<Double?>(null) }
    var rain by remember { mutableStateOf<Boolean?>(null) } // New rain variable
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var day by remember { mutableStateOf(0) }
    var month by remember { mutableStateOf(0) }
    var year by remember { mutableStateOf(0) }

    // Get the current hour
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    // Determine the background image based on the current hour
    val backgroundResource = if (rain == true) {
        R.drawable.rainy // Rainy weather
    } else {
        when {
            currentHour in 19..23 || currentHour in 0..6 -> R.drawable.night // Night time
            else -> R.drawable.sun// Day time
        }
    }

    Scaffold(
        content = { padding ->
            Image(
                painter = painterResource(id = backgroundResource), //rainy and sunny
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(
                modifier = Modifier.padding(padding)
            ) {
                MainScreen(
                    onSearchClicked = { searchDay, searchMonth, searchYear ->
                        if (locationPermissionState.value || requestLocationPermission(context)) {
                            day = searchDay
                            month = searchMonth
                            year = searchYear
                            searchWeatherData(
                                searchDay,
                                searchMonth,
                                searchYear,
                                context,
                                weatherViewModel
                            ) { newMaxTemp, newMinTemp, newRain, newErrorMessage -> // Include rain parameter
                                if (!newErrorMessage.isNullOrEmpty()) {
                                    errorMessage = newErrorMessage
                                    maxTemp=null
                                    minTemp=null
                                    rain=null
                                } else {
                                    maxTemp = newMaxTemp
                                    minTemp = newMinTemp
                                    rain = newRain // Update rain state variable
                                }
                            }
                        }
                    },
                    maxTemp = maxTemp,
                    minTemp = minTemp,
                    rain = rain, // Pass rain to MainScreen
                    errorMessage = errorMessage,
                    onSaveClicked = {
                        // Define behavior when save button is clicked
                        // This could include saving weather data offline
                        if (maxTemp != null && minTemp != null) {
                            val date = "$year-$month-$day"
                            val weatherData =
                                WeatherData(date = date, maxTemp = maxTemp!!, minTemp = minTemp!!)
                            weatherViewModel.insert(weatherData)
                            // Optionally, provide feedback to the user that data has been saved
                        } else {
                            // Handle the case when maxTemp or minTemp is null
                            // You may show a toast or provide feedback to the user
                        }
                    }
                )
            }
        },
        topBar = {
            // Define the top app bar
            TopAppBar(
                title = { Text(text = "Weather App") },
                actions = {
                    // IconButton to navigate to the "savedData" destination
                    IconButton(
                        onClick = { navController.navigate("savedData") }
                    ) {
                        Text("Saved Data")
                    }
                }
            )
        },
        floatingActionButton = {
            // Optionally, define behavior for the floating action button
            if (!locationPermissionState.value && !checkLocationPermission(context)) {
                requestLocationPermission(context)
            }
        }
    )
}

private fun checkLocationPermission(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun requestLocationPermission(context: Context): Boolean {
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as ComponentActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        return false
    }
    return true
}

private fun searchWeatherData(
    day: Int,
    month: Int,
    year: Int,
    context: Context,
    weatherViewModel: WeatherViewModel,
    onWeatherDataReceived: (Double?, Double?, Boolean?, String?) -> Unit // Include rain parameter
) {
    // Get the current date
    val currentDate = Calendar.getInstance()
    val selectedDate = Calendar.getInstance()
    selectedDate.set(year, month - 1, day) // Month in Calendar starts from 0

    // Check if the selected date is in the future
    if (selectedDate.after(currentDate)) {
        val lastTenYearsData = mutableListOf<WeatherData>()
        val allWeather = weatherViewModel.allWeather.value ?: listOf()

        // Calculate the date range for the last 10 years
        for (i in 1..10) {
            val pastYear = selectedDate.get(Calendar.YEAR) - i

            // Filter weather data for the specific year, month, and day
            val weatherData = allWeather.find { data ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = SimpleDateFormat("yyyy-M-d", Locale.US).parse(data.date)?.time ?: 0
                }
                cal.get(Calendar.YEAR) == pastYear &&
                        cal.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                        cal.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
            }

            // Add filtered data to the list
            if (weatherData != null) {
                lastTenYearsData.add(weatherData)
            }
        }

        println(lastTenYearsData)

        // Calculate the average of max temperature and min temperature for the last 10 years
        if (lastTenYearsData.isNotEmpty()) {
            var totalMaxTemp = 0.0
            var totalMinTemp = 0.0
            for (data in lastTenYearsData) {
                totalMaxTemp += data.maxTemp
                totalMinTemp += data.minTemp
            }
            val avgMaxTemp = totalMaxTemp / lastTenYearsData.size
            val avgMinTemp = totalMinTemp / lastTenYearsData.size

            // Set rain to false as default
            val rain = false

            // Invoke the callback with the calculated average temperatures and rain status
            onWeatherDataReceived(avgMaxTemp, avgMinTemp, rain, null)
        } else {
            // No data available in the database for the last 10 years
            onWeatherDataReceived(null, null, false, "No data available for the last 10 years")
        }
        return
    }


    // If the selected date is not in the future, fetch weather data from the API as usual
    CoroutineScope(Dispatchers.IO).launch {
        // Call the getWeatherData function from WeatherApi
        val weatherData = WeatherApi.getWeatherData(day, month, year, context)

        // Update the UI on the Main dispatcher
        withContext(Dispatchers.Main) {
            // Process weather data and update the UI
            // For simplicity, let's print the weather data for now
            weatherData?.let { data ->
                val maxTemp = data["maxTemp"] as? Double
                val minTemp = data["minTemp"] as? Double
                val rain = data["Rain"] as? Boolean
                val errorMessage = data["errorMessage"] as? String
                onWeatherDataReceived(maxTemp, minTemp, rain, errorMessage)
            }
        }
    }
}



@Composable
fun MainScreen(
    onSearchClicked: (day: Int, month: Int, year: Int) -> Unit,
    maxTemp: Double?,
    minTemp: Double?,
    rain: Boolean?,
    errorMessage: String?,
    onSaveClicked: () -> Unit
) {
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val context = LocalContext.current
    val error = errorMessage != null

    println(maxTemp)
    println(minTemp)

    if ( maxTemp != null && minTemp != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 156.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Min °C",
                    style = TextStyle(fontSize = 16.sp)
                )
                Text(
                    text = "$minTemp",
                    style = TextStyle(fontSize = 70.sp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Max °C",
                    style = TextStyle(fontSize = 16.sp)
                )
                Text(
                    text = "$maxTemp",
                    style = TextStyle(fontSize = 70.sp)
                )
            }
        }
    }else{
        Column(
            modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center).padding(top= 96.dp)
        ) {
                val textToDisplay = if (errorMessage != null) {
                    errorMessage
                } else {
                    "No date Selected"
                }
                Text(
                    text = textToDisplay,
                    style = TextStyle(fontSize = 32.sp),
                    fontWeight = FontWeight.Light
                )
            }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Button to trigger calendar date picker dialog
        OutlinedButton(
            onClick = {
                showDatePickerDialog(context) { selectedDate ->
                    day = selectedDate.get(Calendar.DAY_OF_MONTH).toString()
                    month = (selectedDate.get(Calendar.MONTH) + 1).toString()
                    year = selectedDate.get(Calendar.YEAR).toString()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Select Date")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Button to trigger weather data search
        OutlinedButton(
            onClick = {
                val dayInt = day.toIntOrNull()
                val monthInt = month.toIntOrNull()
                val yearInt = year.toIntOrNull()
                if (dayInt != null && monthInt != null && yearInt != null) {
                    onSearchClicked(dayInt, monthInt, yearInt)
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = day.isNotEmpty() && month.isNotEmpty() && year.isNotEmpty()
        ) {
            Text(text = "Search")
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Card to display weather data or error message

        if (errorMessage == null && maxTemp != null && minTemp != null) {
            // Show success button if no error and data available
            Button(
                onClick = onSaveClicked, // Invoke onSaveClicked callback when button is clicked
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Save to offline")
            }
        }
    }
}

fun showDatePickerDialog(context: Context, onDateSelected: (Calendar) -> Unit) {
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(calendar)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.show()
}


@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    MainScreen(
        onSearchClicked = { _, _, _ -> },
        maxTemp = 25.0, // Sample maximum temperature
        minTemp = 15.0,  // Sample minimum temperature
        rain = true, // Sample rain status
        errorMessage = null ,// Preview without error message
        onSaveClicked = {}
    )
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

package com.example.weatherdata

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

object WeatherApi {
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

    suspend fun getWeatherData(day: Int, month: Int, year: Int, context: Context): Map<String, Any?>?  {
        return withContext(Dispatchers.IO) {
            val timestamp = convertToTimestamp(day, month, year)
            println(timestamp)
            val location = getCurrentLocation(context)

            if (location != null) {
                val (lat, lon) = location

                val apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&start_date=$timestamp&end_date=$timestamp"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                try {
                    val responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val json = JSONObject(response.toString())
                        val dailyData = json.optJSONObject("daily")
                        println(dailyData)

                        if (dailyData != null) {
                            val maxTempArray = dailyData.optJSONArray("temperature_2m_max")
                            val minTempArray = dailyData.optJSONArray("temperature_2m_min")
                            val precipitation = dailyData.optJSONArray("precipitation_sum")



                            if (maxTempArray != null && maxTempArray.length() > 0 && minTempArray != null && minTempArray.length() > 0 && precipitation!= null) {
                                val maxTempValue = maxTempArray.optDouble(0, Double.NaN)
                                val minTempValue = minTempArray.optDouble(0, Double.NaN)
                                val rain = precipitation.optDouble(0, Double.NaN)
//                                val rain =45
                                var isRain = false

                                if(rain>30){
                                    isRain=true
                                }
                                if (!maxTempValue.isNaN() && !minTempValue.isNaN()) {
                                    mapOf(
                                        "maxTemp" to maxTempValue,
                                        "minTemp" to minTempValue,
                                        "errorMessage" to null,
                                        "Rain" to isRain
                                    ) // Return temperature values and no error message
                                } else {
                                    mapOf(
                                        "maxTemp" to 0.0,
                                        "minTemp" to 0.0,
                                        "errorMessage" to "Data not available for this date",
                                        "Rain" to null
                                    )  // Handle case where values are null or NaN
                                }
                            } else {
                                null // Handle case where arrays are null or empty
                            }
                        } else {
                            val errorMessage = json.optString("reason")
                            println("API Error: $errorMessage")
                            mapOf(
                                "maxTemp" to Double.NaN,
                                "minTemp" to Double.NaN,
                                "errorMessage" to errorMessage,
                                "Rain" to null
                            ) // Return NaN values and error message
                        }
                    } else {
                        println("HTTP error: $responseCode")
                        null // Return null values and HTTP error message
                    }
                } finally {
                    connection.disconnect()
                }
            } else {
                null
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { continuation ->
            // Check if the ACCESS_FINE_LOCATION permission is granted
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            continuation.resume(Pair(location.latitude, location.longitude))
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener { e: Exception ->
                        e.printStackTrace()
                        continuation.resume(null)
                    }
            } else {
                continuation.resume(null)
            }
        }
    }

    private fun convertToTimestamp(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.MONTH, month - 1) // Month is zero-based in Calendar
        calendar.set(Calendar.YEAR, year)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateFormat.format(calendar.time)
    }
}

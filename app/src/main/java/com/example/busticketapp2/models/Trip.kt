package com.example.busticketapp2.models

import java.text.SimpleDateFormat
import java.util.*

data class Trip(
    val id: Int = 0,
    val fromCity: String,
    val toCity: String,
    val departureTime: String,
    val arrivalTime: String,
    val price: Double,
    val status: String = "Активен",
    val tripDate: String = getCurrentDate(),
    val stops: List<Stop> = emptyList()
) {
    companion object {
        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun formatDate(date: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val parsedDate = inputFormat.parse(date)
                outputFormat.format(parsedDate ?: Date())
            } catch (e: Exception) {
                date
            }
        }
    }
}
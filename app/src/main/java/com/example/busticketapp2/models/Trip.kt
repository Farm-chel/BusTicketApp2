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
    val tripDate: String = getCurrentDate(), // Теперь это будет изменяемое поле
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

        // Новый метод для проверки валидности даты
        fun isValidDate(date: String): Boolean {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.isLenient = false
                sdf.parse(date)
                true
            } catch (e: Exception) {
                false
            }
        }

        // Метод для получения завтрашней даты
        fun getTomorrowDate(): String {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(calendar.time)
        }

        // Метод для получения даты через 7 дней
        fun getNextWeekDate(): String {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(calendar.time)
        }
    }
}
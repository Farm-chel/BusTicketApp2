package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_details)

        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        // Получаем данные о маршруте
        val tripId = intent.getIntExtra("TRIP_ID", -1)
        val tripName = intent.getStringExtra("TRIP_NAME") ?: "Маршрут"
        val tripFrom = intent.getStringExtra("TRIP_FROM") ?: ""
        val tripTo = intent.getStringExtra("TRIP_TO") ?: ""
        val tripTime = intent.getStringExtra("TRIP_TIME") ?: ""
        val tripPrice = intent.getDoubleExtra("TRIP_PRICE", 0.0)
        val tripStopsCount = intent.getIntExtra("TRIP_STOPS_COUNT", 0)
        val tripDuration = intent.getStringExtra("TRIP_DURATION") ?: ""

        initViews(tripName, tripFrom, tripTo, tripTime, tripPrice,
            tripId, tripStopsCount, tripDuration)
    }

    private fun initViews(tripName: String, from: String, to: String, time: String,
                          price: Double, tripId: Int, stopsCount: Int, duration: String) {
        try {
            val txtTripTitle: TextView = findViewById(R.id.txtTripTitle)
            val txtRoute: TextView = findViewById(R.id.txtRoute)
            val txtTime: TextView = findViewById(R.id.txtTime)
            val txtPrice: TextView = findViewById(R.id.txtPrice)
            val txtDuration: TextView = findViewById(R.id.txtDuration)
            val txtStopsCount: TextView = findViewById(R.id.txtStopsCount)
            val btnShowMap: Button = findViewById(R.id.btnShowMap)
            val btnShowStops: Button = findViewById(R.id.btnShowStops)
            val btnBackToRoutes: Button = findViewById(R.id.btnBackToRoutes)

            // Получаем реальное количество остановок из базы данных
            val actualStops = dbHelper.getStopsByTripId(tripId)
            val actualStopsCount = actualStops.size

            // Определяем эмодзи для маршрута
            val emoji = when {
                from.contains("Слободской", ignoreCase = true) || to.contains("Слободской", ignoreCase = true) -> "🏙️"
                from.contains("Котельнич", ignoreCase = true) || to.contains("Котельнич", ignoreCase = true) -> "🚂"
                from.contains("Советск", ignoreCase = true) || to.contains("Советск", ignoreCase = true) -> "🏛️"
                else -> "🚌"
            }

            // Определяем ожидаемое количество остановок для этого маршрута
            val expectedStops = when (tripId) {
                1, 2 -> 38    // Слободской ↔ Киров
                3, 4 -> 70    // Киров ↔ Котельнич
                5, 6 -> 51    // Киров ↔ Советск
                else -> 0
            }

            // Формируем текст с количеством остановок
            val stopsText = if (actualStopsCount == expectedStops) {
                "🚏 Количество остановок: $actualStopsCount"
            } else {
                "🚏 Количество остановок: $actualStopsCount"
            }

            // Устанавливаем значения
            txtTripTitle.text = "$emoji $from → $to"
            txtRoute.text = "📍 Маршрут: $from → $to"

            // Парсим время для лучшего отображения
            val timeParts = time.split(" - ")
            if (timeParts.size == 2) {
                txtTime.text = "⏰ Время: ${timeParts[0]} → ${timeParts[1]}"
            } else {
                txtTime.text = "⏰ Время: $time"
            }

            txtPrice.text = "💰 Стоимость: ${price.toInt()} руб."
            txtDuration.text = "⏱️ Продолжительность: $duration"
            txtStopsCount.text = stopsText

            // Кнопка показа карты
            btnShowMap.setOnClickListener {
                try {
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("TRIP_ID", tripId)
                    intent.putExtra("TRIP_NAME", "$from → $to")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Ошибка открытия карты: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            // Кнопка показа остановок
            btnShowStops.setOnClickListener {
                try {
                    // Проверяем, есть ли остановки
                    if (actualStopsCount == 0) {
                        Toast.makeText(this, "Нет данных об остановках для этого маршрута", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val intent = Intent(this, StopsListActivity::class.java)
                    intent.putExtra("TRIP_ID", tripId)
                    intent.putExtra("TRIP_NAME", "$from → $to")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Ошибка открытия списка остановок: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }

            // Кнопка "Назад к маршрутам"
            btnBackToRoutes.setOnClickListener {
                finish()
            }

            // Показываем количество реально загруженных остановок
            if (actualStopsCount > 0) {
                Log.d("TripDetails", "Загружено $actualStopsCount остановок для маршрута $tripId")
            } else {
                Toast.makeText(this, "Нет данных об остановках для этого маршрута", Toast.LENGTH_SHORT).show()
                btnShowStops.isEnabled = false
                btnShowStops.alpha = 0.5f
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка загрузки деталей маршрута: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
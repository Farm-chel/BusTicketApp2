package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

        initViews(tripName, tripFrom, tripTo, tripTime, tripPrice, tripId, tripStopsCount, tripDuration)
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
            val btnBackToRoutes: Button = findViewById(R.id.btnBackToRoutes)

            // Определяем эмодзи для маршрута
            val emoji = when {
                from.contains("Слободской") || to.contains("Слободской") -> "🏙️"
                from.contains("Котельнич") || to.contains("Котельнич") -> "🚂"
                from.contains("Вятские") || to.contains("Вятские") -> "🌲"
                from.contains("Советск") || to.contains("Советск") -> "🏛️"
                else -> "🚌"
            }

            // Устанавливаем значения
            txtTripTitle.text = "$emoji $from → $to"
            txtRoute.text = "📍 Маршрут: $from → $to"
            txtTime.text = "⏰ Время: $time"
            txtPrice.text = "💰 Стоимость: ${price.toInt()} руб."
            txtDuration.text = "⏱️ Продолжительность: $duration"
            txtStopsCount.text = "🚏 Количество остановок: $stopsCount"

            // Кнопка показа карты
            btnShowMap.setOnClickListener {
                try {
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("TRIP_ID", tripId)
                    intent.putExtra("TRIP_NAME", "$from → $to")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Ошибка открытия карты", Toast.LENGTH_SHORT).show()
                }
            }

            // Кнопка "Назад к маршрутам"
            btnBackToRoutes.setOnClickListener {
                finish()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка загрузки деталей маршрута", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
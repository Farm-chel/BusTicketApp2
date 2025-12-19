package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper

class RouteMapActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_route_map)

        dbHelper = DatabaseHelper(this)

        val btnBack: Button = findViewById(R.id.btnBack)
        val txtRouteTitle: TextView = findViewById(R.id.txtRouteTitle)
        val listViewStops: ListView = findViewById(R.id.listViewStops)

        // Получаем данные о рейсе из intent
        val tripId = intent.getIntExtra("TRIP_ID", -1)
        val tripName = intent.getStringExtra("TRIP_NAME") ?: "Маршрут"

        // Определяем эмодзи для маршрута
        val emoji = when {
            tripName.contains("Слободской") -> "🏙️"
            tripName.contains("Котельнич") -> "🚂"
            tripName.contains("Вятские") -> "🌲"
            tripName.contains("Советск") -> "🏛️"
            else -> "🗺️"
        }

        txtRouteTitle.text = "$emoji Маршрут: $tripName"

        // Получаем остановки из базы данных
        val stops = if (tripId != -1) {
            dbHelper.getStopsByTripId(tripId)
        } else {
            emptyList()
        }

        // Форматируем остановки для отображения на карте
        val stopsDisplay = stops.mapIndexed { index, stop ->
            "📍 ${index + 1}. ${stop.name}\n" +
                    "   ⏰ ${stop.arrivalTime} - ${stop.departureTime} | 💰 ${stop.priceFromStart.toInt()} руб."
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            if (stopsDisplay.isEmpty()) listOf("Информация об остановках временно недоступна") else stopsDisplay)
        listViewStops.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        // Добавляем обработчик для перехода к карте
        listViewStops.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("TRIP_ID", tripId)
            intent.putExtra("TRIP_NAME", tripName)
            startActivity(intent)
        }
    }
}
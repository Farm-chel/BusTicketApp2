package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper

class TripsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_trips)

        dbHelper = DatabaseHelper(this)

        val btnBack: Button = findViewById(R.id.btnBack)
        val listViewTrips: ListView = findViewById(R.id.listViewTrips)

        // Получаем рейсы из базы данных
        val trips = dbHelper.getAllTrips()

        // Форматируем рейсы для отображения
        val tripsDisplay = trips.map { trip ->
            "🚌 ${trip.fromCity} → ${trip.toCity} | ${trip.departureTime}-${trip.arrivalTime} | ${trip.price.toInt()} руб."
        }

        // Используем кастомный адаптер для черного текста
        val adapter = ArrayAdapter(this, R.layout.item_trip, R.id.textViewTrip, tripsDisplay)
        listViewTrips.adapter = adapter

        listViewTrips.setOnItemClickListener { parent, view, position, id ->
            val selectedTrip = trips[position]

            // Запускаем активити с деталями рейса
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("TRIP_ID", selectedTrip.id)
            intent.putExtra("TRIP_NAME", "${selectedTrip.fromCity} - ${selectedTrip.toCity}")
            intent.putExtra("TRIP_FROM", selectedTrip.fromCity)
            intent.putExtra("TRIP_TO", selectedTrip.toCity)
            intent.putExtra("TRIP_TIME", "${selectedTrip.departureTime}-${selectedTrip.arrivalTime}")
            intent.putExtra("TRIP_PRICE", selectedTrip.price)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }

        if (trips.isEmpty()) {
            Toast.makeText(this, "Нет доступных рейсов", Toast.LENGTH_SHORT).show()
        }
    }
}
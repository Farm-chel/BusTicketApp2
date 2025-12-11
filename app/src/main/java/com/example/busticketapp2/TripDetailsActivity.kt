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

        initViews(tripName, tripFrom, tripTo, tripTime, tripPrice, tripId)
    }

    private fun initViews(tripName: String, from: String, to: String, time: String, price: Double, tripId: Int) {
        try {
            val txtTripTitle: TextView = findViewById(R.id.txtTripTitle)
            val txtRoute: TextView = findViewById(R.id.txtRoute)
            val txtTime: TextView = findViewById(R.id.txtTime)
            val txtPrice: TextView = findViewById(R.id.txtPrice)
            val btnShowMap: Button = findViewById(R.id.btnShowMap)
            val btnBackToRoutes: Button = findViewById(R.id.btnBackToRoutes)

            txtTripTitle.text = tripName
            txtRoute.text = "$from → $to"
            txtTime.text = "⏰ $time"
            txtPrice.text = "💰 ${price.toInt()} руб."

            // Кнопка показа карты
            btnShowMap.setOnClickListener {
                try {
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("TRIP_ID", tripId)
                    intent.putExtra("TRIP_NAME", "$from → $to")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка открытия карты", Toast.LENGTH_SHORT).show()
                }
            }

            // Кнопка "Назад к маршрутам"
            btnBackToRoutes.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки деталей маршрута", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
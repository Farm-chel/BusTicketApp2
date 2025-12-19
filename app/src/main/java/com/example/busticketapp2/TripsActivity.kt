package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip

class TripsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private val tripsList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trips)

        supportActionBar?.hide()
        dbHelper = DatabaseHelper(this)

        initViews()
        loadTrips()
    }

    private fun initViews() {
        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadTrips() {
        runOnUiThread {
            try {
                // Показываем прогресс
                val progressBar: ProgressBar = findViewById(R.id.progressBar)
                val txtLoading: TextView = findViewById(R.id.txtLoading)
                progressBar.visibility = android.view.View.VISIBLE
                txtLoading.visibility = android.view.View.VISIBLE

                // Получаем рейсы
                tripsList.clear()
                val allTrips = dbHelper.getAllTrips()

                if (allTrips.isEmpty()) {
                    showErrorMessage("В базе данных нет рейсов")
                    return@runOnUiThread
                }

                tripsList.addAll(allTrips)
                showTripsList()

                Toast.makeText(this, "Найдено ${tripsList.size} рейсов", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                showErrorMessage("Ошибка загрузки: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showErrorMessage(message: String) {
        val txtNoTrips: TextView = findViewById(R.id.txtNoTrips)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val txtLoading: TextView = findViewById(R.id.txtLoading)
        val listViewTrips: ListView = findViewById(R.id.listViewTrips)

        progressBar.visibility = android.view.View.GONE
        txtLoading.visibility = android.view.View.GONE
        txtNoTrips.visibility = android.view.View.VISIBLE
        listViewTrips.visibility = android.view.View.GONE

        txtNoTrips.text = "❌ Ошибка\n$message\n\nПопробуйте:\n1. Перезапустить приложение\n2. Удалить и переустановить"
    }

    private fun showTripsList() {
        val txtNoTrips: TextView = findViewById(R.id.txtNoTrips)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val txtLoading: TextView = findViewById(R.id.txtLoading)
        val listViewTrips: ListView = findViewById(R.id.listViewTrips)

        progressBar.visibility = android.view.View.GONE
        txtLoading.visibility = android.view.View.GONE
        txtNoTrips.visibility = android.view.View.GONE
        listViewTrips.visibility = android.view.View.VISIBLE

        // Создаем адаптер
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            tripsList.map { trip ->
                val emoji = when {
                    trip.fromCity.contains("Слободской", ignoreCase = true) -> "🏙️"
                    trip.fromCity.contains("Котельнич", ignoreCase = true) -> "🚂"
                    trip.fromCity.contains("Вятские", ignoreCase = true) -> "🌲"
                    trip.fromCity.contains("Советск", ignoreCase = true) -> "🏛️"
                    else -> "🚌"
                }

                "$emoji ${trip.fromCity} → ${trip.toCity}\n" +
                        "⏰ ${trip.departureTime} - ${trip.arrivalTime} | 💰 ${trip.price.toInt()} руб."
            }
        )

        listViewTrips.adapter = adapter

        // Обработчик клика
        listViewTrips.setOnItemClickListener { parent, view, position, id ->
            val selectedTrip = tripsList[position]
            openTripDetails(selectedTrip)
        }
    }

    private fun openTripDetails(trip: Trip) {
        val intent = Intent(this, TripDetailsActivity::class.java)
        intent.putExtra("TRIP_ID", trip.id)
        intent.putExtra("TRIP_FROM", trip.fromCity)
        intent.putExtra("TRIP_TO", trip.toCity)
        intent.putExtra("TRIP_TIME", "${trip.departureTime}-${trip.arrivalTime}")
        intent.putExtra("TRIP_PRICE", trip.price)
        startActivity(intent)
    }
}
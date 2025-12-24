package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip

class BookingActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var spinnerTrips: Spinner
    private lateinit var tvSelectedTrip: TextView
    private lateinit var tvPrice: TextView
    private lateinit var btnBook: Button
    private lateinit var btnBack: Button

    private var selectedTrip: Trip? = null
    private val tripsList = mutableListOf<Trip>()
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_booking)

        dbHelper = DatabaseHelper(this)

        currentUserId = intent.getIntExtra("USER_ID", -1)

        initViews()
        loadAllTrips()
        setupSpinner()
        setupClickListeners()

        if (currentUserId == -1) {
            Toast.makeText(this, "❌ Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        spinnerTrips = findViewById(R.id.spinnerTrips)
        tvSelectedTrip = findViewById(R.id.tvSelectedTrip)
        tvPrice = findViewById(R.id.tvPrice)
        btnBook = findViewById(R.id.btnBook)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadAllTrips() {
        try {
            tripsList.clear()
            val allTrips = dbHelper.getAllTrips()

            if (allTrips.isEmpty()) {
                Toast.makeText(this, "❌ В базе данных нет рейсов!", Toast.LENGTH_LONG).show()
                return
            }

            tripsList.addAll(allTrips)

            // Проверяем, есть ли все 6 маршрутов
            if (tripsList.size < 6) {
                Toast.makeText(this, "⚠️ В базе только ${tripsList.size} рейсов", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка загрузки рейсов: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSpinner() {
        if (tripsList.isEmpty()) {
            tvSelectedTrip.text = "❌ Нет доступных рейсов"
            tvPrice.text = "0 руб."
            btnBook.isEnabled = false
            Toast.makeText(this, "Нет рейсов для бронирования", Toast.LENGTH_SHORT).show()
            return
        }

        // Форматируем для отображения в спиннере
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            tripsList.map {
                val emoji = getTripEmoji(it)
                "$emoji ${it.fromCity} → ${it.toCity} | ${it.departureTime} | ${it.price.toInt()} руб."
            })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTrips.adapter = adapter

        spinnerTrips.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedTrip = tripsList[position]
                updateTripInfo()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedTrip = null
                updateTripInfo()
            }
        }

        if (tripsList.isNotEmpty()) {
            selectedTrip = tripsList[0]
            updateTripInfo()
        }
    }

    private fun getTripEmoji(trip: Trip): String {
        return when {
            trip.fromCity.contains("Слободской") || trip.toCity.contains("Слободской") -> "🏙️"
            trip.fromCity.contains("Котельнич") || trip.toCity.contains("Котельнич") -> "🚂"
            trip.fromCity.contains("Советск") || trip.toCity.contains("Советск") -> "🏛️"
            else -> "🚌"
        }
    }

    private fun updateTripInfo() {
        selectedTrip?.let { trip ->
            // Рассчитываем длительность поездки
            val duration = calculateDuration(trip.departureTime, trip.arrivalTime)

            // Определяем количество остановок
            val stopsCount = when (trip.id) {
                1, 2 -> 38    // Слободской ↔ Киров
                3, 4 -> 70    // Киров ↔ Котельнич
                5, 6 -> 51    // Киров ↔ Советск
                else -> 0
            }

            tvSelectedTrip.text = "📍 ${trip.fromCity} → ${trip.toCity}\n" +
                    "🕐 ${trip.departureTime} - ${trip.arrivalTime}\n" +
                    "⏱️ $duration\n" +
                    "🚏 $stopsCount остановок"
            tvPrice.text = "${trip.price.toInt()} руб."
        } ?: run {
            tvSelectedTrip.text = "❌ Не выбран"
            tvPrice.text = "0 руб."
        }
    }

    private fun calculateDuration(departure: String, arrival: String): String {
        return when {
            departure == "08:00" && arrival == "09:00" -> "1 час"
            departure == "14:00" && arrival == "15:00" -> "1 час"
            departure == "09:30" && arrival == "11:55" -> "2 ч 25 мин"
            departure == "16:00" && arrival == "18:25" -> "2 ч 25 мин"
            departure == "08:30" && arrival == "10:40" -> "2 ч 10 мин"
            departure == "11:40" && arrival == "13:50" -> "2 ч 10 мин"
            else -> {
                try {
                    val depParts = departure.split(":")
                    val arrParts = arrival.split(":")

                    val depHour = depParts[0].toInt()
                    val depMin = depParts[1].toInt()
                    val arrHour = arrParts[0].toInt()
                    val arrMin = arrParts[1].toInt()

                    var totalMinutes = (arrHour * 60 + arrMin) - (depHour * 60 + depMin)
                    if (totalMinutes < 0) totalMinutes += 24 * 60

                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60

                    when {
                        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
                        hours > 0 -> "$hours ч"
                        else -> "$minutes мин"
                    }
                } catch (e: Exception) {
                    "N/A"
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnBook.setOnClickListener {
            if (selectedTrip != null) {
                showTicketCountDialog()
            } else {
                Toast.makeText(this, "❌ Выберите рейс", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showTicketCountDialog() {
        if (selectedTrip != null) {
            val duration = calculateDuration(selectedTrip!!.departureTime, selectedTrip!!.arrivalTime)

            AlertDialog.Builder(this)
                .setTitle("🎫 Выберите количество билетов")
                .setMessage("Рейс: ${selectedTrip!!.fromCity} → ${selectedTrip!!.toCity}\n" +
                        "Время: ${selectedTrip!!.departureTime} - ${selectedTrip!!.arrivalTime}\n" +
                        "Длительность: $duration\n" +
                        "Цена: ${selectedTrip!!.price.toInt()} руб.")
                .setPositiveButton("🎫 Один билет") { dialog, which ->
                    bookSingleTicket()
                }
                .setNeutralButton("🎫🎫 Несколько билетов") { dialog, which ->
                    bookMultipleTickets()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            Toast.makeText(this, "❌ Выберите рейс", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bookSingleTicket() {
        val intent = Intent(this, DateSelectionActivity::class.java)
        intent.putExtra("TRIP_ID", selectedTrip!!.id)
        intent.putExtra("USER_ID", currentUserId)
        startActivity(intent)
    }

    private fun bookMultipleTickets() {
        val intent = Intent(this, MultiBookingActivity::class.java)
        intent.putExtra("TRIP_ID", selectedTrip!!.id)
        intent.putExtra("USER_ID", currentUserId)
        startActivity(intent)
    }
}
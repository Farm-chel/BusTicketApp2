package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                progressBar.visibility = View.VISIBLE
                txtLoading.visibility = View.VISIBLE

                // Получаем рейсы
                tripsList.clear()
                val allTrips = dbHelper.getAllTrips()

                if (allTrips.isEmpty()) {
                    showErrorMessage("В базе данных нет рейсов")
                    return@runOnUiThread
                }

                tripsList.addAll(allTrips)
                showTripsListWithCards()

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

        progressBar.visibility = View.GONE
        txtLoading.visibility = View.GONE
        txtNoTrips.visibility = View.VISIBLE
        listViewTrips.visibility = View.GONE

        txtNoTrips.text = "❌ Ошибка\n$message\n\nПопробуйте:\n1. Перезапустить приложение\n2. Удалить и переустановить"
    }

    private fun showTripsListWithCards() {
        val txtNoTrips: TextView = findViewById(R.id.txtNoTrips)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val txtLoading: TextView = findViewById(R.id.txtLoading)
        val listViewTrips: ListView = findViewById(R.id.listViewTrips)

        progressBar.visibility = View.GONE
        txtLoading.visibility = View.GONE
        txtNoTrips.visibility = View.GONE
        listViewTrips.visibility = View.VISIBLE

        // Создаем кастомный адаптер
        val adapter = TripAdapter(this, tripsList)
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

        // Рассчитываем количество остановок
        val stopsCount = when (trip.id) {
            1, 2 -> 42
            3, 4 -> 70
            5, 6 -> 66
            7, 8 -> 42
            else -> 0
        }
        intent.putExtra("TRIP_STOPS_COUNT", stopsCount)

        // Рассчитываем длительность
        val duration = calculateDuration(trip.departureTime, trip.arrivalTime)
        intent.putExtra("TRIP_DURATION", duration)

        startActivity(intent)
    }

    private fun calculateDuration(departure: String, arrival: String): String {
        return when {
            departure == "08:00" && arrival == "09:00" -> "1 час"
            departure == "14:00" && arrival == "15:00" -> "1 час"
            departure == "09:30" && arrival == "11:55" -> "2 ч 25 мин"
            departure == "16:00" && arrival == "18:25" -> "2 ч 25 мин"
            departure == "07:30" && arrival == "14:30" -> "7 часов"
            departure == "06:00" && arrival == "13:00" -> "7 часов"
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
}

// Кастомный адаптер для отображения рейсов
class TripAdapter(
    private val context: TripsActivity,
    private val trips: List<Trip>
) : ArrayAdapter<Trip>(context, R.layout.item_trip_card, trips) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.item_trip_card, parent, false)

        val trip = trips[position]

        val tripEmoji = view.findViewById<TextView>(R.id.tripEmoji)
        val tripRoute = view.findViewById<TextView>(R.id.tripRoute)
        val tripTime = view.findViewById<TextView>(R.id.tripTime)
        val tripPrice = view.findViewById<TextView>(R.id.tripPrice)
        val tripInfo = view.findViewById<TextView>(R.id.tripInfo)

        // Определяем эмодзи
        val emoji = when {
            trip.fromCity.contains("Слободской", ignoreCase = true) -> "🏙️"
            trip.fromCity.contains("Котельнич", ignoreCase = true) -> "🚂"
            trip.fromCity.contains("Вятские", ignoreCase = true) -> "🌲"
            trip.fromCity.contains("Советск", ignoreCase = true) -> "🏛️"
            else -> "🚌"
        }

        // Определяем количество остановок
        val stopsCount = when (trip.id) {
            1, 2 -> 42
            3, 4 -> 70
            5, 6 -> 66
            7, 8 -> 42
            else -> 0
        }

        // Рассчитываем длительность
        val duration = calculateDuration(trip.departureTime, trip.arrivalTime)

        // Устанавливаем значения
        tripEmoji.text = emoji
        tripRoute.text = "${trip.fromCity} → ${trip.toCity}"
        tripTime.text = "${trip.departureTime} - ${trip.arrivalTime}"
        tripPrice.text = "${trip.price.toInt()} руб."
        tripInfo.text = "⏱️ $duration | 🚏 $stopsCount остановок"

        return view
    }

    private fun calculateDuration(departure: String, arrival: String): String {
        return when {
            departure == "08:00" && arrival == "09:00" -> "1 час"
            departure == "14:00" && arrival == "15:00" -> "1 час"
            departure == "09:30" && arrival == "11:55" -> "2 ч 25 мин"
            departure == "16:00" && arrival == "18:25" -> "2 ч 25 мин"
            departure == "07:30" && arrival == "14:30" -> "7 часов"
            departure == "06:00" && arrival == "13:00" -> "7 часов"
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
}
package com.example.busticketapp2

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper

class StopsListActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stops_list)

        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        val tripId = intent.getIntExtra("TRIP_ID", -1)
        val tripName = intent.getStringExtra("TRIP_NAME") ?: "Маршрут"

        initViews(tripId, tripName)
    }

    private fun initViews(tripId: Int, tripName: String) {
        try {
            val txtStopsTitle: TextView = findViewById(R.id.txtStopsTitle)
            val txtStopsInfo: TextView = findViewById(R.id.txtStopsInfo)
            val listViewStops: ListView = findViewById(R.id.listViewStops)
            val btnBackFromStops: Button = findViewById(R.id.btnBackFromStops)

            // Устанавливаем заголовок
            txtStopsTitle.text = "🚏 Остановки маршрута: $tripName"

            // Получаем остановки из базы данных
            Log.d("StopsList", "Загрузка остановок для маршрута ID: $tripId")
            val stops = dbHelper.getStopsByTripId(tripId)
            Log.d("StopsList", "Получено ${stops.size} остановок")

            if (stops.isNotEmpty()) {
                // Форматируем для отображения
                val stopItems = mutableListOf<String>()

                stops.forEachIndexed { index, stop ->
                    val item = "${index + 1}. ${stop.name}\n   📍 ${stop.arrivalTime} - ${stop.departureTime}"

                    // Добавляем цену, если она больше 0
                    if (stop.priceFromStart > 0) {
                        stopItems.add("$item | 💰 ${stop.priceFromStart.toInt()} руб.")
                    } else {
                        stopItems.add(item)
                    }
                }

                // Создаем адаптер
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    stopItems
                )
                listViewStops.adapter = adapter

                // Обновляем информацию
                txtStopsInfo.text = "📊 Всего остановок: ${stops.size}\n" +
                        "🕐 Первая остановка: ${stops.first().arrivalTime}\n" +
                        "🏁 Последняя остановка: ${stops.last().arrivalTime}"

                // Добавляем обработчик клика по остановке
                listViewStops.setOnItemClickListener { parent, view, position, id ->
                    val selectedStop = stops[position]
                    Toast.makeText(
                        this,
                        "${selectedStop.name}\n${selectedStop.arrivalTime} - ${selectedStop.departureTime}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                txtStopsInfo.text = "❌ Нет данных об остановках"
                Toast.makeText(this, "В базе данных нет остановок для этого маршрута", Toast.LENGTH_LONG).show()

                // Создаем пустой адаптер
                val adapter = ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    listOf("Нет данных об остановках")
                )
                listViewStops.adapter = adapter
            }

            // Кнопка назад
            btnBackFromStops.setOnClickListener {
                finish()
            }

        } catch (e: Exception) {
            Log.e("StopsList", "Ошибка: ${e.message}", e)
            Toast.makeText(this, "❌ Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
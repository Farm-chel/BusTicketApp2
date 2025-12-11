package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip

class MultiBookingActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var selectedTrip: Trip
    private lateinit var spinnerPassengerCount: Spinner
    private lateinit var btnContinue: Button
    private lateinit var btnBack: Button
    private lateinit var txtTripInfo: TextView

    private var currentUserId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_booking)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        // Получаем данные о рейсе и ID текущего пользователя
        val tripId = intent.getIntExtra("TRIP_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        selectedTrip = dbHelper.getTripById(tripId) ?: run {
            Toast.makeText(this, "Ошибка: рейс не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUserId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSpinner()
        setupClickListeners()
    }

    private fun initViews() {
        spinnerPassengerCount = findViewById(R.id.spinnerPassengerCount)
        btnContinue = findViewById(R.id.btnContinue)
        btnBack = findViewById(R.id.btnBack)
        txtTripInfo = findViewById(R.id.txtTripInfo)

        // Отображаем информацию о рейсе
        txtTripInfo.text = "${selectedTrip.fromCity} → ${selectedTrip.toCity}\n" +
                "${selectedTrip.departureTime} - ${selectedTrip.arrivalTime}\n" +
                "${selectedTrip.price.toInt()} руб. за билет"
    }

    private fun setupSpinner() {
        val passengerCounts = arrayOf("1 пассажир", "2 пассажира", "3 пассажира", "4 пассажира", "5 пассажиров")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, passengerCounts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPassengerCount.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnContinue.setOnClickListener {
            val passengerCount = spinnerPassengerCount.selectedItemPosition + 1
            val intent = Intent(this, SimpleSeatSelectionActivity::class.java)
            intent.putExtra("TRIP_ID", selectedTrip.id)
            intent.putExtra("USER_ID", currentUserId)
            intent.putExtra("MULTI_MODE", true) // Флаг для многоместного режима
            intent.putExtra("PASSENGER_COUNT", passengerCount) // Количество пассажиров
            startActivity(intent)
        }
    }
}
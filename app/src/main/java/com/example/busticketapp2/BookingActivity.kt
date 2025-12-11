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
    private lateinit var tvSelectedTrip: TextView // Изменили имя с txtSelectedTrip на tvSelectedTrip
    private lateinit var tvPrice: TextView // Изменили имя с txtPrice на tvPrice
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
        setupSpinner()
        setupClickListeners()

        if (currentUserId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        spinnerTrips = findViewById(R.id.spinnerTrips)
        tvSelectedTrip = findViewById(R.id.tvSelectedTrip) // Убедитесь что этот ID существует в layout
        tvPrice = findViewById(R.id.tvPrice) // Убедитесь что этот ID существует в layout
        btnBook = findViewById(R.id.btnBook)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupSpinner() {
        tripsList.clear()
        tripsList.addAll(dbHelper.getAllTrips())

        if (tripsList.isEmpty()) {
            Toast.makeText(this, "Нет доступных рейсов", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            tripsList.map { "${it.fromCity} → ${it.toCity} - ${it.departureTime}" })
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

    private fun updateTripInfo() {
        selectedTrip?.let { trip ->
            tvSelectedTrip.text = "${trip.fromCity} → ${trip.toCity}\n${trip.departureTime} - ${trip.arrivalTime}"
            tvPrice.text = "${trip.price.toInt()} руб."
        } ?: run {
            tvSelectedTrip.text = "Не выбран"
            tvPrice.text = "0 руб."
        }
    }

    private fun setupClickListeners() {
        btnBook.setOnClickListener {
            if (selectedTrip != null) {
                showTicketCountDialog()
            } else {
                Toast.makeText(this, "Выберите рейс", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showTicketCountDialog() {
        if (selectedTrip != null) {
            AlertDialog.Builder(this)
                .setTitle("🎫 Выберите количество билетов")
                .setMessage("Рейс: ${selectedTrip!!.fromCity} → ${selectedTrip!!.toCity}\n" +
                        "Время: ${selectedTrip!!.departureTime}\n" +
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
            Toast.makeText(this, "Выберите рейс", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bookSingleTicket() {
        val intent = Intent(this, SimpleSeatSelectionActivity::class.java)
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
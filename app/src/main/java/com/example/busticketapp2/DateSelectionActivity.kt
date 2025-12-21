package com.example.busticketapp2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip
import java.text.SimpleDateFormat
import java.util.*

class DateSelectionActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var selectedTrip: Trip
    private lateinit var btnSelectDate: Button
    private lateinit var btnContinue: Button
    private lateinit var btnBack: Button
    private lateinit var txtSelectedDate: TextView
    private lateinit var txtTripInfo: TextView
    private lateinit var txtAvailableDates: TextView

    private var selectedDate: String = ""
    private var currentUserId = -1
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_date_selection)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        // Получаем данные о рейсе
        val tripId = intent.getIntExtra("TRIP_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)
        selectedTrip = dbHelper.getTripById(tripId) ?: run {
            Toast.makeText(this, "Ошибка: рейс не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUserId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        updateAvailableDates()
    }

    private fun initViews() {
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnContinue = findViewById(R.id.btnContinue)
        btnBack = findViewById(R.id.btnBack)
        txtSelectedDate = findViewById(R.id.txtSelectedDate)
        txtTripInfo = findViewById(R.id.txtTripInfo)
        txtAvailableDates = findViewById(R.id.txtAvailableDates)

        // Информация о рейсе
        txtTripInfo.text = "${selectedTrip.fromCity} → ${selectedTrip.toCity}\n" +
                "${selectedTrip.departureTime} - ${selectedTrip.arrivalTime}\n" +
                "${selectedTrip.price.toInt()} руб."

        // Устанавливаем минимальную дату (завтра)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnContinue.setOnClickListener {
            if (selectedDate.isNotEmpty()) {
                proceedToSeatSelection()
            } else {
                Toast.makeText(this, "Выберите дату поездки", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Форматируем дату в формат yyyy-MM-dd
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )

                selectedDate = formattedDate
                updateSelectedDateText()
            },
            year,
            month,
            day
        )

        // Устанавливаем минимальную дату (завтра)
        val minDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        datePicker.datePicker.minDate = minDate

        // Максимальная дата - через 30 дней
        val maxDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 30)
        }.timeInMillis
        datePicker.datePicker.maxDate = maxDate

        datePicker.show()
    }

    private fun updateSelectedDateText() {
        val formattedDate = Trip.formatDate(selectedDate)
        txtSelectedDate.text = "📅 Выбрана дата: $formattedDate"
        btnContinue.isEnabled = true
    }

    private fun updateAvailableDates() {
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val nextWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        txtAvailableDates.text = "✅ Доступные даты:\n" +
                "• Завтра: ${sdf.format(tomorrow.time)}\n" +
                "• Через неделю: ${sdf.format(nextWeek.time)}\n" +
                "• Любой день в течение месяца"
    }

    private fun proceedToSeatSelection() {
        val intent = Intent(this, SimpleSeatSelectionActivity::class.java)
        intent.putExtra("TRIP_ID", selectedTrip.id)
        intent.putExtra("USER_ID", currentUserId)
        intent.putExtra("TRIP_DATE", selectedDate) // Передаем выбранную дату
        startActivity(intent)
    }
}
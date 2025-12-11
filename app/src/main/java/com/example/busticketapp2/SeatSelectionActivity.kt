package com.example.busticketapp2

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var selectedTrip: Trip
    private lateinit var gridLayoutSeats: GridLayout
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: Button
    private lateinit var txtSelectedSeat: TextView
    private lateinit var txtTripInfo: TextView

    private var selectedSeat: Int = 0
    private val totalSeats = 45 // Всего мест в автобусе

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        // Получаем данные о рейсе
        val tripId = intent.getIntExtra("TRIP_ID", -1)
        selectedTrip = dbHelper.getTripById(tripId) ?: run {
            Toast.makeText(this, "Ошибка: рейс не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSeatGrid()
        setupClickListeners()
    }

    private fun initViews() {
        gridLayoutSeats = findViewById(R.id.gridLayoutSeats)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnBack = findViewById(R.id.btnBack)
        txtSelectedSeat = findViewById(R.id.txtSelectedSeat)
        txtTripInfo = findViewById(R.id.txtTripInfo)

        // Отображаем информацию о рейсе
        txtTripInfo.text = "${selectedTrip.fromCity} → ${selectedTrip.toCity}\n${selectedTrip.departureTime} - ${selectedTrip.arrivalTime}\n${selectedTrip.price.toInt()} руб."
    }

    private fun setupSeatGrid() {
        gridLayoutSeats.removeAllViews()
        gridLayoutSeats.columnCount = 4 // 4 места в ряду
        gridLayoutSeats.rowCount = (totalSeats + 3) / 4 // Расчет количества рядов

        val bookedSeats = dbHelper.getBookedSeats(selectedTrip.id)

        for (seatNumber in 1..totalSeats) {
            val seatButton = Button(this).apply {
                text = seatNumber.toString()
                tag = seatNumber
                textSize = 12f
                setPadding(8, 8, 8, 8)

                // Разные цвета в зависимости от статуса места
                when {
                    bookedSeats.contains(seatNumber) -> {
                        setBackgroundColor(Color.RED)
                        setTextColor(Color.WHITE)
                        isEnabled = false
                        text = "✗$seatNumber"
                    }
                    seatNumber == selectedSeat -> {
                        setBackgroundColor(Color.GREEN)
                        setTextColor(Color.WHITE)
                    }
                    else -> {
                        setBackgroundColor(Color.LTGRAY)
                        setTextColor(Color.BLACK)
                    }
                }

                // Обработчик выбора места
                setOnClickListener {
                    if (!bookedSeats.contains(seatNumber)) {
                        selectedSeat = seatNumber
                        updateSeatSelection()
                    }
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }

            seatButton.layoutParams = params
            gridLayoutSeats.addView(seatButton)
        }

        updateSeatSelection()
    }

    private fun updateSeatSelection() {
        // Обновляем все кнопки
        val bookedSeats = dbHelper.getBookedSeats(selectedTrip.id)

        for (i in 0 until gridLayoutSeats.childCount) {
            val seatButton = gridLayoutSeats.getChildAt(i) as Button
            val seatNum = seatButton.tag as Int

            when {
                bookedSeats.contains(seatNum) -> {
                    seatButton.setBackgroundColor(Color.RED)
                    seatButton.setTextColor(Color.WHITE)
                    seatButton.isEnabled = false
                    seatButton.text = "✗$seatNum"
                }
                seatNum == selectedSeat -> {
                    seatButton.setBackgroundColor(Color.GREEN)
                    seatButton.setTextColor(Color.WHITE)
                    seatButton.text = "✓$seatNum"
                }
                else -> {
                    seatButton.setBackgroundColor(Color.LTGRAY)
                    seatButton.setTextColor(Color.BLACK)
                    seatButton.text = seatNum.toString()
                }
            }
        }

        // Обновляем текст выбранного места
        if (selectedSeat > 0) {
            txtSelectedSeat.text = "Выбрано место: $selectedSeat"
            btnConfirm.isEnabled = true
        } else {
            txtSelectedSeat.text = "Выберите место"
            btnConfirm.isEnabled = false
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            if (selectedSeat > 0) {
                confirmBooking()
            } else {
                Toast.makeText(this, "Выберите место", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmBooking() {
        val currentUser = getCurrentUser()

        if (currentUser != null) {
            val bookingId = dbHelper.addBookingWithSeat(
                userId = currentUser.id,
                tripId = selectedTrip.id,
                passengerName = currentUser.fullName,
                passengerEmail = currentUser.email,
                seatNumber = selectedSeat
            )

            if (bookingId != -1L) {
                showSuccessDialog(bookingId)
            } else {
                Toast.makeText(this, "Ошибка бронирования", Toast.LENGTH_SHORT).show()
            }
        } else {
            showLoginRequiredDialog()
        }
    }

    private fun getCurrentUser(): com.example.busticketapp2.models.User? {
        return try {
            dbHelper.getUser("user", "user") // Для демо - используем тестового пользователя
        } catch (e: Exception) {
            null
        }
    }

    private fun showSuccessDialog(bookingId: Long) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("✅ Бронирование успешно!")
            .setMessage("Ваш билет забронирован!\nНомер билета: $bookingId")
            .setPositiveButton("Показать чек") { dialog, which ->
                val intent = Intent(this, ReceiptActivity::class.java)
                intent.putExtra("BOOKING_ID", bookingId.toInt())
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Закрыть") { dialog, which ->
                finish()
            }
            .create()

        alertDialog.show()
    }

    private fun showLoginRequiredDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("🔐 Требуется авторизация")
            .setMessage("Для бронирования билетов необходимо войти в систему.")
            .setPositiveButton("OK", null)
            .create()

        alertDialog.show()
    }
}
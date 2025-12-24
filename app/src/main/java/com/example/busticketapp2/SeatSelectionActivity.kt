package com.example.busticketapp2

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
    private val seatsPerRow = 4 // 2 слева + 2 справа

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
        gridLayoutSeats.columnCount = 5 // 2 места слева + проход + 2 места справа

        // Рассчитываем количество рядов: 45 мест / 4 места в ряду = 12 рядов
        val totalRows = (totalSeats + seatsPerRow - 1) / seatsPerRow
        gridLayoutSeats.rowCount = totalRows

        val bookedSeats = dbHelper.getBookedSeats(selectedTrip.id)

        for (row in 0 until totalRows) {
            // Создаем 5 колонок в каждом ряду
            for (col in 0 until 5) {
                // Если это колонка 2 (индекс 2) - это проход
                if (col == 2) {
                    // Создаем пустое View для прохода
                    val aisleView = View(this).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 60 // Ширина прохода
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(col)
                            rowSpec = GridLayout.spec(row)
                            setMargins(8, 4, 8, 4)
                        }
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                    gridLayoutSeats.addView(aisleView)
                    continue
                }

                // Рассчитываем номер места для текущей колонки
                // Для колонок 0,1 - левая сторона, для 3,4 - правая сторона
                val currentSeatNumber = when {
                    col < 2 -> row * 2 + col + 1 // Левая сторона: места 1,2,5,6,9,10...
                    else -> row * 2 + col - 1 // Правая сторона: места 3,4,7,8,11,12...
                }

                // Проверяем, не превышает ли номер места общее количество мест
                if (currentSeatNumber <= totalSeats) {
                    val seatButton = Button(this).apply {
                        text = currentSeatNumber.toString()
                        tag = currentSeatNumber
                        textSize = 12f
                        setPadding(8, 8, 8, 8)

                        // Разные цвета в зависимости от статуса места
                        when {
                            bookedSeats.contains(currentSeatNumber) -> {
                                setBackgroundColor(Color.RED)
                                setTextColor(Color.WHITE)
                                isEnabled = false
                                text = "✗$currentSeatNumber"
                            }
                            currentSeatNumber == selectedSeat -> {
                                setBackgroundColor(Color.GREEN)
                                setTextColor(Color.WHITE)
                                text = "✓$currentSeatNumber"
                            }
                            else -> {
                                setBackgroundColor(Color.LTGRAY)
                                setTextColor(Color.BLACK)
                                text = currentSeatNumber.toString()
                            }
                        }

                        // Обработчик выбора места
                        setOnClickListener {
                            if (!bookedSeats.contains(currentSeatNumber)) {
                                selectedSeat = currentSeatNumber
                                updateSeatSelection()
                            }
                        }
                    }

                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(col, 1f)
                        rowSpec = GridLayout.spec(row, 1f)
                        setMargins(4, 4, 4, 4)
                    }

                    seatButton.layoutParams = params
                    gridLayoutSeats.addView(seatButton)
                } else {
                    // Если места нет, создаем пустое View
                    val emptyView = View(this).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(col, 1f)
                            rowSpec = GridLayout.spec(row, 1f)
                            setMargins(4, 4, 4, 4)
                        }
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                    gridLayoutSeats.addView(emptyView)
                }
            }
        }

        updateSeatSelection()
    }

    private fun updateSeatSelection() {
        // Обновляем все кнопки
        val bookedSeats = dbHelper.getBookedSeats(selectedTrip.id)

        for (i in 0 until gridLayoutSeats.childCount) {
            val view = gridLayoutSeats.getChildAt(i)
            if (view is Button) {
                val seatNum = view.tag as? Int ?: continue

                when {
                    bookedSeats.contains(seatNum) -> {
                        view.setBackgroundColor(Color.RED)
                        view.setTextColor(Color.WHITE)
                        view.isEnabled = false
                        view.text = "✗$seatNum"
                    }
                    seatNum == selectedSeat -> {
                        view.setBackgroundColor(Color.GREEN)
                        view.setTextColor(Color.WHITE)
                        view.text = "✓$seatNum"
                    }
                    else -> {
                        view.setBackgroundColor(Color.LTGRAY)
                        view.setTextColor(Color.BLACK)
                        view.text = seatNum.toString()
                    }
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
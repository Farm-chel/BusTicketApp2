package com.example.busticketapp2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip
import com.example.busticketapp2.models.User

class SimpleSeatSelectionActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var selectedTrip: Trip
    private lateinit var gridLayoutSeats: GridLayout
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: Button
    private lateinit var txtSelectedSeat: TextView
    private lateinit var txtTripInfo: TextView

    private var selectedSeat: Int = 0
    private var userId: Int = -1
    private var isMultiMode: Boolean = false
    private var passengerCount: Int = 1
    private val selectedSeats = mutableListOf<Int>()
    private val totalSeats = 45

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        val tripId = intent.getIntExtra("TRIP_ID", -1)
        userId = intent.getIntExtra("USER_ID", -1)
        isMultiMode = intent.getBooleanExtra("MULTI_MODE", false)
        passengerCount = intent.getIntExtra("PASSENGER_COUNT", 1)

        selectedTrip = dbHelper.getTripById(tripId) ?: run {
            Toast.makeText(this, "Ошибка: рейс не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (userId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
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

        txtTripInfo.text = "${selectedTrip.fromCity} → ${selectedTrip.toCity}\n${selectedTrip.departureTime} - ${selectedTrip.arrivalTime}\n${selectedTrip.price.toInt()} руб."

        if (isMultiMode) {
            txtSelectedSeat.text = "Выберите $passengerCount мест(а)"
        } else {
            txtSelectedSeat.text = "Выберите место"
        }
    }

    private fun setupSeatGrid() {
        gridLayoutSeats.removeAllViews()
        gridLayoutSeats.columnCount = 4

        val bookedSeats = dbHelper.getBookedSeats(selectedTrip.id)

        for (seatNumber in 1..totalSeats) {
            val seatButton = Button(this).apply {
                text = seatNumber.toString()
                tag = seatNumber
                textSize = 12f
                setPadding(8, 8, 8, 8)

                when {
                    bookedSeats.contains(seatNumber) -> {
                        setBackgroundColor(Color.RED)
                        setTextColor(Color.WHITE)
                        isEnabled = false
                        text = "✗$seatNumber"
                    }
                    isMultiMode && selectedSeats.contains(seatNumber) -> {
                        setBackgroundColor(Color.GREEN)
                        setTextColor(Color.WHITE)
                        text = "✓$seatNumber"
                    }
                    !isMultiMode && seatNumber == selectedSeat -> {
                        setBackgroundColor(Color.GREEN)
                        setTextColor(Color.WHITE)
                        text = "✓$seatNumber"
                    }
                    else -> {
                        setBackgroundColor(Color.LTGRAY)
                        setTextColor(Color.BLACK)
                    }
                }

                setOnClickListener {
                    if (!bookedSeats.contains(seatNumber)) {
                        if (isMultiMode) {
                            if (selectedSeats.contains(seatNumber)) {
                                selectedSeats.remove(seatNumber)
                            } else {
                                if (selectedSeats.size < passengerCount) {
                                    selectedSeats.add(seatNumber)
                                } else {
                                    Toast.makeText(this@SimpleSeatSelectionActivity,
                                        "Вы уже выбрали максимальное количество мест ($passengerCount)",
                                        Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                            }
                        } else {
                            selectedSeat = seatNumber
                            selectedSeats.clear()
                            selectedSeats.add(seatNumber)
                        }
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
                isMultiMode && selectedSeats.contains(seatNum) -> {
                    seatButton.setBackgroundColor(Color.GREEN)
                    seatButton.setTextColor(Color.WHITE)
                    seatButton.text = "✓$seatNum"
                }
                !isMultiMode && seatNum == selectedSeat -> {
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

        if (isMultiMode) {
            if (selectedSeats.isNotEmpty()) {
                txtSelectedSeat.text = "Выбраны места: ${selectedSeats.sorted().joinToString(", ")}"
                btnConfirm.isEnabled = selectedSeats.size == passengerCount
            } else {
                txtSelectedSeat.text = "Выберите $passengerCount мест(а)"
                btnConfirm.isEnabled = false
            }
        } else {
            if (selectedSeat > 0) {
                txtSelectedSeat.text = "Выбрано место: $selectedSeat"
                btnConfirm.isEnabled = true
            } else {
                txtSelectedSeat.text = "Выберите место"
                btnConfirm.isEnabled = false
            }
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            if (isMultiMode) {
                if (selectedSeats.size == passengerCount) {
                    showMultiPassengerDialog()
                } else {
                    Toast.makeText(this, "Выберите все места", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (selectedSeat > 0) {
                    showSinglePassengerDialog()
                }
            }
        }
    }

    private fun showSinglePassengerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_simple_booking, null)
        val editPassengerName = dialogView.findViewById<EditText>(R.id.editPassengerName)
        val editPassengerEmail = dialogView.findViewById<EditText>(R.id.editPassengerEmail)

        // Устанавливаем черный цвет текста ПРОГРАММНО
        editPassengerName.setTextColor(Color.BLACK)
        editPassengerEmail.setTextColor(Color.BLACK)

        // Устанавливаем подсказки
        editPassengerName.hint = "Введите ФИО пассажира"
        editPassengerEmail.hint = "Введите email пассажира"

        // Получаем пользователя
        val user = dbHelper.getUserById(userId)

        if (user != null) {
            // Автозаполняем
            editPassengerName.setText(user.fullName)
            editPassengerEmail.setText(user.email)

            // Делаем текст черным
            editPassengerName.setTextColor(Color.BLACK)
            editPassengerEmail.setTextColor(Color.BLACK)
        } else {
            // Показываем предупреждение красным
            Toast.makeText(this, "⚠️ Пользователь не найден. Заполните данные вручную.",
                Toast.LENGTH_LONG).show()
        }

        // Создаем кастомный диалог
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("📝 Данные пассажира")
            .setView(dialogView)
            .setPositiveButton("✅ Забронировать") { dialog, which ->
                val passengerName = editPassengerName.text.toString().trim()
                val passengerEmail = editPassengerEmail.text.toString().trim()

                if (passengerName.isEmpty()) {
                    editPassengerName.error = "Введите ФИО"
                    editPassengerName.requestFocus()
                    return@setPositiveButton
                }

                if (passengerEmail.isEmpty()) {
                    editPassengerEmail.error = "Введите email"
                    editPassengerEmail.requestFocus()
                    return@setPositiveButton
                }

                // Валидация email
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(passengerEmail).matches()) {
                    editPassengerEmail.error = "Введите корректный email"
                    editPassengerEmail.requestFocus()
                    return@setPositiveButton
                }

                // СОЗДАЕМ БРОНИРОВАНИЕ
                val bookingId = dbHelper.addBookingWithSeat(
                    userId = userId,
                    tripId = selectedTrip.id,
                    passengerName = passengerName,
                    passengerEmail = passengerEmail,
                    seatNumber = selectedSeat
                )

                if (bookingId != -1L) {
                    showSuccessDialog(bookingId.toInt(), listOf(selectedSeat))
                } else {
                    Toast.makeText(this, "❌ Ошибка бронирования", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("❌ Отмена", null)
            .create()

        // Показываем и устанавливаем фокус
        alertDialog.show()
        editPassengerName.requestFocus()
    }

    private fun showMultiPassengerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multi_passenger, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerPassengerFields)

        // Создаем поля для каждого пассажира
        for (i in 0 until passengerCount) {
            val passengerView = layoutInflater.inflate(R.layout.item_passenger_details, null)
            val txtPassengerNumber = passengerView.findViewById<TextView>(R.id.txtPassengerNumber)
            val editPassengerName = passengerView.findViewById<EditText>(R.id.editPassengerName)
            val editPassengerEmail = passengerView.findViewById<EditText>(R.id.editPassengerEmail)

            txtPassengerNumber.text = "Пассажир ${i + 1} (Место ${selectedSeats[i]})"

            // Автозаполнение данных текущего пользователя для первого пассажира
            if (i == 0) {
                val currentUser = dbHelper.getUserById(userId)
                currentUser?.let { user ->
                    editPassengerName.setText(user.fullName)
                    editPassengerEmail.setText(user.email)
                } ?: run {
                    Toast.makeText(this, "Данные пользователя не найдены. Заполните вручную.", Toast.LENGTH_SHORT).show()
                }
            }

            container.addView(passengerView)
        }

        AlertDialog.Builder(this)
            .setTitle("Данные пассажиров")
            .setView(dialogView)
            .setPositiveButton("Подтвердить") { dialog, which ->
                processMultiBooking(container)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun processMultiBooking(container: LinearLayout) {
        var allBookingsSuccessful = true
        val bookingIds = mutableListOf<Long>()

        for (i in 0 until passengerCount) {
            val passengerView = container.getChildAt(i)
            val editPassengerName = passengerView.findViewById<EditText>(R.id.editPassengerName)
            val editPassengerEmail = passengerView.findViewById<EditText>(R.id.editPassengerEmail)

            val passengerName = editPassengerName.text.toString().trim()
            val passengerEmail = editPassengerEmail.text.toString().trim()

            if (passengerName.isEmpty() || passengerEmail.isEmpty()) {
                Toast.makeText(this, "Заполните данные для всех пассажиров", Toast.LENGTH_SHORT).show()
                return
            }

            // Валидация email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(passengerEmail).matches()) {
                Toast.makeText(this, "Введите корректный email адрес для пассажира ${i + 1}", Toast.LENGTH_LONG).show()
                return
            }

            val bookingId = dbHelper.addBookingWithSeat(
                userId = userId,
                tripId = selectedTrip.id,
                passengerName = passengerName,
                passengerEmail = passengerEmail,
                seatNumber = selectedSeats[i]
            )

            if (bookingId == -1L) {
                allBookingsSuccessful = false
            } else {
                bookingIds.add(bookingId)
            }
        }

        if (allBookingsSuccessful) {
            showSuccessDialog(bookingIds.first().toInt(), selectedSeats)
        } else {
            Toast.makeText(this, "Ошибка при бронировании некоторых билетов", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessDialog(bookingId: Int, seats: List<Int>) {
        val totalPrice = selectedTrip.price * seats.size

        AlertDialog.Builder(this)
            .setTitle("✅ Бронирование успешно!")
            .setMessage("Забронировано ${seats.size} билет(а)\n" +
                    "Места: ${seats.sorted().joinToString(", ")}\n" +
                    "Общая стоимость: ${totalPrice.toInt()} руб.\n" +
                    "Номер основного билета: $bookingId")
            .setPositiveButton("OK") { dialog, which ->
                // Возвращаемся на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .create()
            .show()
    }
}
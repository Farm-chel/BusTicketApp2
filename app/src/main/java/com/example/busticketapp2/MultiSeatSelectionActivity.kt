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
import com.example.busticketapp2.models.User

class MultiSeatSelectionActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var selectedTrip: Trip
    private lateinit var gridLayoutSeats: GridLayout
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: Button
    private lateinit var txtSelectedSeats: TextView
    private lateinit var txtTripInfo: TextView
    private lateinit var txtPassengerCount: TextView

    private var passengerCount = 1
    private var currentUserId = -1
    private val selectedSeats = mutableListOf<Int>()
    private val totalSeats = 45

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_seat_selection)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        // Получаем данные о рейсе, количестве пассажиров и ID текущего пользователя
        val tripId = intent.getIntExtra("TRIP_ID", -1)
        passengerCount = intent.getIntExtra("PASSENGER_COUNT", 1)
        currentUserId = intent.getIntExtra("CURRENT_USER_ID", -1)

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
        setupSeatGrid()
        setupClickListeners()
    }

    private fun initViews() {
        gridLayoutSeats = findViewById(R.id.gridLayoutSeats)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnBack = findViewById(R.id.btnBack)
        txtSelectedSeats = findViewById(R.id.txtSelectedSeats)
        txtTripInfo = findViewById(R.id.txtTripInfo)
        txtPassengerCount = findViewById(R.id.txtPassengerCount)

        // Отображаем информацию о рейсе и количестве пассажиров
        txtTripInfo.text = "${selectedTrip.fromCity} → ${selectedTrip.toCity}\n${selectedTrip.departureTime} - ${selectedTrip.arrivalTime}\n${selectedTrip.price.toInt()} руб."
        txtPassengerCount.text = "Выберите $passengerCount мест(а)"
    }

    private fun setupSeatGrid() {
        gridLayoutSeats.removeAllViews()
        gridLayoutSeats.columnCount = 4
        gridLayoutSeats.rowCount = (totalSeats + 3) / 4

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
                    selectedSeats.contains(seatNumber) -> {
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
                        if (selectedSeats.contains(seatNumber)) {
                            selectedSeats.remove(seatNumber)
                        } else {
                            if (selectedSeats.size < passengerCount) {
                                selectedSeats.add(seatNumber)
                            } else {
                                Toast.makeText(this@MultiSeatSelectionActivity,
                                    "Вы уже выбрали максимальное количество мест",
                                    Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
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
                selectedSeats.contains(seatNum) -> {
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

        // Обновляем текст выбранных мест
        if (selectedSeats.isNotEmpty()) {
            txtSelectedSeats.text = "Выбраны места: ${selectedSeats.sorted().joinToString(", ")}"
        } else {
            txtSelectedSeats.text = "Выберите $passengerCount мест(а)"
        }

        // Если выбрано нужное количество мест, активируем кнопку
        btnConfirm.isEnabled = selectedSeats.size == passengerCount
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            if (selectedSeats.size == passengerCount) {
                showPassengerDetailsDialog()
            } else {
                Toast.makeText(this, "Выберите все места", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPassengerDetailsDialog() {
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
                val currentUser = getCurrentUser()
                currentUser?.let { user ->
                    editPassengerName.setText(user.fullName)
                    editPassengerEmail.setText(user.email)
                }
            }

            container.addView(passengerView)
        }

        AlertDialog.Builder(this)
            .setTitle("Данные пассажиров")
            .setView(dialogView)
            .setPositiveButton("Подтвердить") { dialog, which ->
                processBooking(container)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun processBooking(container: LinearLayout) {
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
                userId = currentUserId,
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
            showSuccessDialog(bookingIds)
        } else {
            Toast.makeText(this, "Ошибка при бронировании некоторых билетов", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentUser(): User? {
        return try {
            dbHelper.getUserById(currentUserId)
        } catch (e: Exception) {
            null
        }
    }

    private fun showSuccessDialog(bookingIds: List<Long>) {
        val totalPrice = selectedTrip.price * passengerCount

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("✅ Бронирование успешно!")
            .setMessage("Забронировано ${bookingIds.size} билет(а)\n" +
                    "Номера билетов: ${bookingIds.joinToString(", ")}\n" +
                    "Общая стоимость: ${totalPrice.toInt()} руб.\n\n" +
                    "Билеты можно посмотреть в вашем профиле.")
            .setPositiveButton("Показать чек") { dialog, which ->
                // Показываем первый чек, остальные можно посмотреть в профиле
                val intent = Intent(this, ReceiptActivity::class.java)
                intent.putExtra("BOOKING_ID", bookingIds.first().toInt())
                startActivity(intent)
                finish()
            }
            .setNegativeButton("В профиль") { dialog, which ->
                // Возвращаемся в главное меню
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .create()

        alertDialog.show()
    }
}
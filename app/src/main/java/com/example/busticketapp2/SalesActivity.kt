package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Trip
import com.example.busticketapp2.models.User

class SalesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var spinnerTrips: Spinner
    private lateinit var spinnerUsers: Spinner
    private lateinit var spinnerSeats: Spinner
    private lateinit var editPassengerName: EditText
    private lateinit var editPassengerEmail: EditText
    private lateinit var btnSell: Button
    private lateinit var btnBack: Button

    private var selectedTrip: Trip? = null
    private var selectedUser: User? = null
    private val tripsList = mutableListOf<Trip>()
    private val usersList = mutableListOf<User>()
    private val seatOptions = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_sales)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupSpinners()
        setupClickListeners()
    }

    private fun initViews() {
        spinnerTrips = findViewById(R.id.spinnerTrips)
        spinnerUsers = findViewById(R.id.spinnerUsers)
        spinnerSeats = findViewById(R.id.spinnerSeats)
        editPassengerName = findViewById(R.id.editPassengerName)
        editPassengerEmail = findViewById(R.id.editPassengerEmail)
        btnSell = findViewById(R.id.btnSell)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupSpinners() {
        // Заполняем список рейсов
        tripsList.clear()
        tripsList.addAll(dbHelper.getAllTrips())

        val tripsAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            tripsList.map { trip ->
                // Определяем эмодзи для маршрута
                val emoji = when {
                    trip.fromCity.contains("Слободской") || trip.toCity.contains("Слободской") -> "🏙️"
                    trip.fromCity.contains("Котельнич") || trip.toCity.contains("Котельнич") -> "🚂"
                    trip.fromCity.contains("Вятские") || trip.toCity.contains("Вятские") -> "🌲"
                    trip.fromCity.contains("Советск") || trip.toCity.contains("Советск") -> "🏛️"
                    else -> "🚌"
                }

                "$emoji ${trip.fromCity} → ${trip.toCity} - ${trip.departureTime} - ${trip.price.toInt()} руб."
            }
        )
        tripsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTrips.adapter = tripsAdapter

        spinnerTrips.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < tripsList.size) {
                    selectedTrip = tripsList[position]
                    updateSeatOptions() // Обновляем места при выборе рейса
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedTrip = null
            }
        }

        // Заполняем список пользователей
        usersList.clear()
        usersList.addAll(dbHelper.getAllUsers().filter { it.role == "Пассажир" })

        if (usersList.isEmpty()) {
            // Если нет пассажиров, добавляем сообщение
            val usersAdapter = ArrayAdapter(
                this,
                R.layout.spinner_item,
                listOf("Нет зарегистрированных пассажиров")
            )
            usersAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinnerUsers.adapter = usersAdapter
        } else {
            val usersAdapter = ArrayAdapter(
                this,
                R.layout.spinner_item,
                usersList.map { user ->
                    "${user.fullName} (${user.email})"
                }
            )
            usersAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinnerUsers.adapter = usersAdapter
        }

        spinnerUsers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < usersList.size) {
                    selectedUser = usersList[position]
                    // Автозаполнение данных пользователя
                    editPassengerName.setText(selectedUser?.fullName)
                    editPassengerEmail.setText(selectedUser?.email)
                } else {
                    selectedUser = null
                    editPassengerName.text.clear()
                    editPassengerEmail.text.clear()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedUser = null
            }
        }

        // Инициализация мест
        updateSeatOptions()
    }

    private fun updateSeatOptions() {
        seatOptions.clear()

        // Всегда добавляем опцию авто-выбора
        seatOptions.add("🚗 Авто-выбор")

        if (selectedTrip != null) {
            // Получаем занятые места для выбранного рейса
            val bookedSeats = dbHelper.getBookedSeats(selectedTrip!!.id)

            // Добавляем все свободные места (1-45)
            for (seatNumber in 1..45) {
                if (!bookedSeats.contains(seatNumber)) {
                    seatOptions.add("💺 Место $seatNumber")
                }
            }

            // Если все места заняты
            if (seatOptions.size == 1) { // Только "Авто-выбор"
                seatOptions.add("⚠️ Нет свободных мест")
            }
        }

        val seatsAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            seatOptions
        )
        seatsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerSeats.adapter = seatsAdapter

        // Выбираем авто-выбор по умолчанию
        if (seatOptions.contains("🚗 Авто-выбор")) {
            spinnerSeats.setSelection(seatOptions.indexOf("🚗 Авто-выбор"))
        }
    }

    private fun setupClickListeners() {
        btnSell.setOnClickListener {
            val name = editPassengerName.text.toString().trim()
            val email = editPassengerEmail.text.toString().trim()
            val selectedSeatOption = spinnerSeats.selectedItem?.toString() ?: ""

            if (selectedTrip == null) {
                Toast.makeText(this, "❌ Выберите рейс", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedUser == null) {
                Toast.makeText(this, "❌ Выберите пользователя", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                editPassengerName.error = "Введите имя пассажира"
                editPassengerName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                editPassengerEmail.error = "Введите email пассажира"
                editPassengerEmail.requestFocus()
                return@setOnClickListener
            }

            // Валидация email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editPassengerEmail.error = "Введите корректный email адрес"
                editPassengerEmail.requestFocus()
                return@setOnClickListener
            }

            // Определяем номер места
            var seatNumber: Int

            if (selectedSeatOption == "🚗 Авто-выбор" || selectedSeatOption.contains("Авто-выбор")) {
                // Автоматический выбор первого свободного места
                val bookedSeats = dbHelper.getBookedSeats(selectedTrip!!.id)
                seatNumber = 1
                while (bookedSeats.contains(seatNumber) && seatNumber <= 45) {
                    seatNumber++
                }

                if (seatNumber > 45) {
                    Toast.makeText(this, "❌ В автобусе нет свободных мест", Toast.LENGTH_SHORT).show()
                    updateSeatOptions() // Обновляем список
                    return@setOnClickListener
                }
            } else if (selectedSeatOption.contains("Место")) {
                // Парсим выбранное место (например, "💺 Место 15")
                val seatText = selectedSeatOption.replace("💺 Место ", "").replace("Место ", "")
                seatNumber = seatText.toIntOrNull() ?: 0

                if (seatNumber == 0 || seatNumber > 45) {
                    Toast.makeText(this, "❌ Неверный номер места", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Проверяем, что место свободно
                val bookedSeats = dbHelper.getBookedSeats(selectedTrip!!.id)
                if (bookedSeats.contains(seatNumber)) {
                    Toast.makeText(this, "❌ Место $seatNumber уже занято", Toast.LENGTH_SHORT).show()
                    updateSeatOptions() // Обновляем список мест
                    return@setOnClickListener
                }
            } else {
                Toast.makeText(this, "❌ Выберите место или авто-выбор", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Создаем бронирование для выбранного пользователя
            val bookingId = dbHelper.addBookingForUser(
                userId = selectedUser!!.id,
                tripId = selectedTrip!!.id,
                passengerName = name,
                passengerEmail = email,
                seatNumber = seatNumber
            )

            if (bookingId != -1L) {
                // Успешная продажа
                val successMessage = """
                    ✅ Билет продан!
                    
                    📋 Детали:
                    • Пассажир: $name
                    • Email: $email
                    • Рейс: ${selectedTrip!!.fromCity} → ${selectedTrip!!.toCity}
                    • Время: ${selectedTrip!!.departureTime}
                    • Место: $seatNumber
                    • Стоимость: ${selectedTrip!!.price.toInt()} руб.
                    • Номер билета: $bookingId
                """.trimIndent()

                // Показываем уведомление
                AlertDialog.Builder(this)
                    .setTitle("✅ Продажа успешна!")
                    .setMessage(successMessage)
                    .setPositiveButton("📄 Показать чек") { dialog, which ->
                        // Открываем чек
                        val intent = Intent(this@SalesActivity, ReceiptActivity::class.java)
                        intent.putExtra("BOOKING_ID", bookingId.toInt())
                        startActivity(intent)
                    }
                    .setNeutralButton("🔄 Продолжить") { dialog, which ->
                        // Очищаем поля для следующей продажи
                        editPassengerName.text.clear()
                        editPassengerEmail.text.clear()
                        updateSeatOptions() // Обновляем места
                    }
                    .show()

            } else {
                Toast.makeText(this, "❌ Ошибка при продаже билета", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
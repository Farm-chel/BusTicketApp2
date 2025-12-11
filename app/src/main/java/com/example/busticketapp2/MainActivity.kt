package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var loginLayout: View
    private lateinit var mainMenuLayout: View
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnProfile: MaterialButton
    private lateinit var tvCurrentUser: MaterialTextView
    private lateinit var btnViewTripsGuest: MaterialButton

    // Кнопки меню в виде CardView (убираем cardQrScanner)
    private lateinit var cardTrips: CardView
    private lateinit var cardBooking: CardView
    private lateinit var cardSales: CardView
    private lateinit var cardReports: CardView
    private lateinit var cardUserManagement: CardView
    private lateinit var cardAbout: CardView
    private lateinit var cardMyTickets: CardView

    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    var currentUser: User? = null

    companion object {
        private const val REGISTRATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            dbHelper = DatabaseHelper(this)

            initViews()
            setupClickListeners()

            val registeredUsername = intent.getStringExtra("REGISTERED_USERNAME")
            val registeredPassword = intent.getStringExtra("REGISTERED_PASSWORD")

            if (!registeredUsername.isNullOrEmpty() && !registeredPassword.isNullOrEmpty()) {
                etUsername.setText(registeredUsername)
                etPassword.setText(registeredPassword)
                login()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initViews() {
        loginLayout = findViewById(R.id.loginCard)
        mainMenuLayout = findViewById(R.id.mainMenuCard)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogout = findViewById(R.id.btnLogout)
        btnProfile = findViewById(R.id.btnProfile)
        tvCurrentUser = findViewById(R.id.tvCurrentUser)
        btnViewTripsGuest = findViewById(R.id.btnViewTripsGuest)

        // Инициализация CardView (без cardQrScanner)
        cardTrips = findViewById(R.id.cardTrips)
        cardBooking = findViewById(R.id.cardBooking)
        cardSales = findViewById(R.id.cardSales)
        cardReports = findViewById(R.id.cardReports)
        cardUserManagement = findViewById(R.id.cardUserManagement)
        cardAbout = findViewById(R.id.cardAbout)
        cardMyTickets = findViewById(R.id.cardMyTickets)

        usernameLayout = findViewById(R.id.usernameLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener { login() }
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivityForResult(intent, REGISTRATION_REQUEST_CODE)
        }
        btnLogout.setOnClickListener { logout() }
        btnProfile.setOnClickListener { showProfile() }

        // НОВАЯ КНОПКА: Просмотр рейсов для гостей
        btnViewTripsGuest.setOnClickListener {
            try {
                // Запускаем активность просмотра рейсов
                val intent = Intent(this, TripsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // Простые обработчики для карточек
        cardTrips.setOnClickListener {
            startActivity(Intent(this, TripsActivity::class.java))
        }

        cardBooking.setOnClickListener {
            if (currentUser != null) {
                val intent = Intent(this, BookingActivity::class.java)
                intent.putExtra("USER_ID", currentUser!!.id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Для бронирования войдите в систему", Toast.LENGTH_SHORT).show()
            }
        }

        cardMyTickets.setOnClickListener {
            if (currentUser != null) {
                showMyTickets()
            } else {
                Toast.makeText(this, "Для просмотра билетов войдите в систему", Toast.LENGTH_SHORT).show()
            }
        }

        cardSales.setOnClickListener {
            if (currentUser != null && (currentUser?.role == "Кассир" || currentUser?.role == "Администратор")) {
                startActivity(Intent(this, SalesActivity::class.java))
            } else {
                Toast.makeText(this, "Доступно только для кассиров и администраторов", Toast.LENGTH_SHORT).show()
            }
        }

        cardReports.setOnClickListener {
            if (currentUser != null && currentUser?.role == "Администратор") {
                startActivity(Intent(this, ReportsActivity::class.java))
            } else {
                Toast.makeText(this, "Доступно только для администраторов", Toast.LENGTH_SHORT).show()
            }
        }

        cardUserManagement.setOnClickListener {
            if (currentUser != null && currentUser?.role == "Администратор") {
                startActivity(Intent(this, UserManagementActivity::class.java))
            } else {
                Toast.makeText(this, "Доступно только для администраторов", Toast.LENGTH_SHORT).show()
            }
        }

        cardAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REGISTRATION_REQUEST_CODE && resultCode == RESULT_OK) {
            val username = data?.getStringExtra("REGISTERED_USERNAME") ?: ""
            val password = data?.getStringExtra("REGISTERED_PASSWORD") ?: ""

            if (username.isNotEmpty() && password.isNotEmpty()) {
                etUsername.setText(username)
                etPassword.setText(password)
                login()
            }
        }
    }

    private fun login() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty()) {
            usernameLayout.error = "Введите логин"
            return
        } else {
            usernameLayout.error = null
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Введите пароль"
            return
        } else {
            passwordLayout.error = null
        }

        currentUser = dbHelper.getUser(username, password)

        if (currentUser != null) {
            showMainMenu()
            Toast.makeText(this, "Добро пожаловать, ${currentUser?.fullName}!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMainMenu() {
        currentUser?.let { user ->
            loginLayout.visibility = View.GONE
            mainMenuLayout.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE
            btnProfile.visibility = View.VISIBLE
            tvCurrentUser.text = "${user.fullName} (${user.role})"

            setupMenuForRole(user.role)
        }
    }

    private fun setupMenuForRole(role: String) {
        // Скрываем все карточки сначала
        cardTrips.visibility = View.GONE
        cardBooking.visibility = View.GONE
        cardSales.visibility = View.GONE
        cardReports.visibility = View.GONE
        cardUserManagement.visibility = View.GONE
        cardAbout.visibility = View.GONE
        cardMyTickets.visibility = View.GONE

        when (role) {
            "Администратор" -> {
                cardTrips.visibility = View.VISIBLE
                cardBooking.visibility = View.VISIBLE
                cardMyTickets.visibility = View.VISIBLE
                cardSales.visibility = View.VISIBLE
                cardReports.visibility = View.VISIBLE
                cardUserManagement.visibility = View.VISIBLE
                cardAbout.visibility = View.VISIBLE
            }
            "Кассир" -> {
                cardTrips.visibility = View.VISIBLE
                cardBooking.visibility = View.VISIBLE
                cardMyTickets.visibility = View.VISIBLE
                cardSales.visibility = View.VISIBLE
                cardAbout.visibility = View.VISIBLE
            }
            "Пассажир" -> {
                cardTrips.visibility = View.VISIBLE
                cardBooking.visibility = View.VISIBLE
                cardMyTickets.visibility = View.VISIBLE
                cardAbout.visibility = View.VISIBLE
            }
        }
    }

    private fun showProfile() {
        currentUser?.let { user ->
            val allBookings = dbHelper.getBookingsByUserIdFull(user.id)

            val profileInfo = """
                👤 Профиль пользователя
                ------------------------
                📝 ФИО: ${user.fullName}
                📧 Email: ${user.email}
                📱 Телефон: ${if (user.phone.isNotEmpty()) user.phone else "Не указан"}
                🎯 Роль: ${user.role}
                📅 Дата регистрации: ${user.createdDate}
                🎫 Всего бронирований: ${allBookings.size}
                ------------------------
            """.trimIndent()

            MaterialAlertDialogBuilder(this)
                .setTitle("Профиль пользователя")
                .setMessage(profileInfo)
                .setPositiveButton("Показать мои билеты") { dialog, which ->
                    showMyTickets()
                }
                .setNegativeButton("Закрыть", null)
                .show()
        }
    }

    private fun showMyTickets() {
        currentUser?.let { user ->
            val allBookings = dbHelper.getBookingsByUserIdFull(user.id)

            if (allBookings.isEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Мои билеты")
                    .setMessage("У вас пока нет активных билетов.")
                    .setPositiveButton("Забронировать") { dialog, which ->
                        val intent = Intent(this, BookingActivity::class.java)
                        intent.putExtra("USER_ID", user.id)
                        startActivity(intent)
                    }
                    .setNegativeButton("Закрыть", null)
                    .show()
                return
            }

            val bookingItems = allBookings.mapIndexed { index, booking ->
                val tripInfo = dbHelper.getTripById(booking.tripId)
                val tripText = tripInfo?.let {
                    "${it.fromCity} → ${it.toCity} (${it.departureTime})"
                } ?: "Рейс не найден"
                "${index + 1}. $tripText\n   Пассажир: ${booking.passengerName}\n   Email: ${booking.passengerEmail}\n   Место: ${booking.seatNumber}\n   Статус: ${booking.status}"
            }.toTypedArray()

            MaterialAlertDialogBuilder(this)
                .setTitle("Мои билеты (${allBookings.size})")
                .setItems(bookingItems) { dialog, which ->
                    if (which < allBookings.size) {
                        val booking = allBookings[which]
                        showBookingActionsDialog(booking)
                    }
                }
                .setPositiveButton("Закрыть", null)
                .show()
        }
    }

    private fun showBookingActionsDialog(booking: com.example.busticketapp2.models.Booking) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Действия с билетом")
            .setItems(arrayOf("📄 Показать чек", "❌ Удалить билет")) { dialog, which ->
                when (which) {
                    0 -> showReceipt(booking)
                    1 -> showCancelBookingDialog(booking) // Теперь это удаление
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showReceipt(booking: com.example.busticketapp2.models.Booking) {
        val intent = Intent(this, ReceiptActivity::class.java)
        intent.putExtra("BOOKING_ID", booking.id)
        startActivity(intent)
    }

    private fun showCancelBookingDialog(booking: com.example.busticketapp2.models.Booking) {
        MaterialAlertDialogBuilder(this)
            .setTitle("❌ Удаление билета")
            .setMessage("Вы действительно хотите удалить билет №${booking.id}?\n\n" +
                    "⚠️ Это действие необратимо! Билет будет полностью удален из системы.")
            .setPositiveButton("Да, удалить") { dialog, which ->
                // УДАЛЯЕМ билет вместо изменения статуса
                val success = dbHelper.deleteBooking(booking.id)
                if (success) {
                    Toast.makeText(this, "Билет №${booking.id} успешно удален", Toast.LENGTH_SHORT).show()
                    showMyTickets() // Обновляем список
                } else {
                    Toast.makeText(this, "Ошибка при удалении билета", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun logout() {
        currentUser = null
        etUsername.text?.clear()
        etPassword.text?.clear()
        loginLayout.visibility = View.VISIBLE
        mainMenuLayout.visibility = View.GONE
        btnLogout.visibility = View.GONE
        btnProfile.visibility = View.GONE
        tvCurrentUser.text = "Гость"

        resetMenuVisibility()
        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show()
    }

    private fun resetMenuVisibility() {
        cardTrips.visibility = View.GONE
        cardBooking.visibility = View.GONE
        cardSales.visibility = View.GONE
        cardReports.visibility = View.GONE
        cardUserManagement.visibility = View.GONE
        cardAbout.visibility = View.GONE
        cardMyTickets.visibility = View.GONE
    }
}
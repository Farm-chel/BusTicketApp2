package com.example.busticketapp2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import java.util.*

class RegistrationActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var emailService: EmailIntentService

    // Переменная для контроля повторных нажатий
    private var isRegistering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        supportActionBar?.hide()

        // Инициализация
        dbHelper = DatabaseHelper(this)
        emailService = EmailIntentService(this)

        // Находим все View элементы
        initViews()

        // Настраиваем обработчики
        setupClickListeners()

        // Настраиваем очистку ошибок при вводе
        setupTextWatchers()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        btnRegister = findViewById(R.id.btnRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
    }

    private fun setupClickListeners() {
        // Кнопка регистрации - ОСНОВНОЙ МЕТОД
        btnRegister.setOnClickListener {
            if (!isRegistering) {
                isRegistering = true
                registerUser()
            }
        }

        // Кнопка назад
        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupTextWatchers() {
        // Простая очистка ошибок при вводе
        val clearErrorWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Находим какой EditText вызвал изменение
                when (s?.hashCode()) {
                    etUsername.text.hashCode() -> etUsername.error = null
                    etPassword.text.hashCode() -> etPassword.error = null
                    etConfirmPassword.text.hashCode() -> etConfirmPassword.error = null
                    etFullName.text.hashCode() -> etFullName.error = null
                    etEmail.text.hashCode() -> etEmail.error = null
                    etPhone.text.hashCode() -> etPhone.error = null
                }
            }
        }

        etUsername.addTextChangedListener(clearErrorWatcher)
        etPassword.addTextChangedListener(clearErrorWatcher)
        etConfirmPassword.addTextChangedListener(clearErrorWatcher)
        etFullName.addTextChangedListener(clearErrorWatcher)
        etEmail.addTextChangedListener(clearErrorWatcher)
        etPhone.addTextChangedListener(clearErrorWatcher)
    }

    private fun registerUser() {
        try {
            // Сбрасываем статус при завершении
            isRegistering = true

            // Получаем значения
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            // === ВАЛИДАЦИЯ ПОЛЕЙ ===
            var hasError = false

            // 1. Проверка логина
            if (username.isEmpty()) {
                etUsername.error = "Введите логин"
                etUsername.requestFocus()
                hasError = true
            } else if (username.length < 3) {
                etUsername.error = "Логин должен быть не менее 3 символов"
                etUsername.requestFocus()
                hasError = true
            } else if (dbHelper.isUsernameExists(username)) {
                etUsername.error = "Этот логин уже занят"
                etUsername.requestFocus()
                hasError = true
            }

            // 2. Проверка пароля
            if (password.isEmpty()) {
                if (!hasError) {
                    etPassword.error = "Введите пароль"
                    etPassword.requestFocus()
                    hasError = true
                }
            } else if (password.length < 6) {
                if (!hasError) {
                    etPassword.error = "Пароль должен быть не менее 6 символов"
                    etPassword.requestFocus()
                    hasError = true
                }
            }

            // 3. Проверка подтверждения пароля
            if (confirmPassword.isEmpty()) {
                if (!hasError) {
                    etConfirmPassword.error = "Подтвердите пароль"
                    etConfirmPassword.requestFocus()
                    hasError = true
                }
            } else if (password != confirmPassword) {
                if (!hasError) {
                    etConfirmPassword.error = "Пароли не совпадают"
                    etConfirmPassword.requestFocus()
                    hasError = true
                }
            }

            // 4. Проверка имени
            if (fullName.isEmpty()) {
                if (!hasError) {
                    etFullName.error = "Введите полное имя"
                    etFullName.requestFocus()
                    hasError = true
                }
            }

            // 5. Проверка email
            if (email.isEmpty()) {
                if (!hasError) {
                    etEmail.error = "Введите email"
                    etEmail.requestFocus()
                    hasError = true
                }
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (!hasError) {
                    etEmail.error = "Введите корректный email"
                    etEmail.requestFocus()
                    hasError = true
                }
            } else if (dbHelper.isEmailExists(email)) {
                if (!hasError) {
                    etEmail.error = "Этот email уже используется"
                    etEmail.requestFocus()
                    hasError = true
                }
            }

            // Если есть ошибки - выходим
            if (hasError) {
                isRegistering = false
                return
            }

            // === ПРОЦЕСС РЕГИСТРАЦИИ ===

            // Показываем прогресс
            btnRegister.text = "Регистрируем..."
            btnRegister.isEnabled = false

            // 1. Пробуем зарегистрировать
            val userId = dbHelper.registerUser(username, password, "Пассажир", fullName, email, phone)

            // 2. Проверяем результат
            if (userId != -1L) {
                // УСПЕХ!
                showSuccessAndContinue(userId, email)
            } else {
                // ОШИБКА
                showRegistrationError(username, email)
            }

        } catch (e: Exception) {
            // Обработка неожиданных ошибок
            Toast.makeText(this, "Неожиданная ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            // Восстанавливаем кнопку
            btnRegister.text = "Зарегистрироваться"
            btnRegister.isEnabled = true
            isRegistering = false
        }
    }

    private fun showSuccessAndContinue(userId: Long, email: String) {
        // Генерируем код
        val verificationCode = (100000..999999).random().toString()

        // Сохраняем в базу
        dbHelper.saveVerificationCode(userId.toInt(), email, verificationCode)

        // Показываем успех
        AlertDialog.Builder(this)
            .setTitle("✅ Регистрация успешна!")
            .setMessage("""
                Ваш ID: $userId
                Email: $email
                
                Генерируем код подтверждения...
            """.trimIndent())
            .setCancelable(false)
            .setPositiveButton("Продолжить") { dialog, which ->
                // Пробуем отправить email
                val isEmailSent = emailService.sendVerificationCodeViaIntent(email, verificationCode)

                if (isEmailSent) {
                    // Email отправлен - переходим к подтверждению
                    goToVerification(userId, email, verificationCode)
                } else {
                    // Нет почтового приложения - показываем код в диалоге
                    showCodeManually(verificationCode, email, userId)
                }
            }
            .show()
    }

    private fun showRegistrationError(username: String, email: String) {
        // Подробный анализ ошибки
        val errorMessage = StringBuilder("❌ Ошибка регистрации:\n\n")

        // Проверяем причины
        val usernameExists = dbHelper.isUsernameExists(username)
        val emailExists = dbHelper.isEmailExists(email)

        if (usernameExists) {
            errorMessage.append("• Логин '$username' уже занят\n")
            etUsername.error = "Этот логин уже занят"
            etUsername.requestFocus()
        }

        if (emailExists) {
            errorMessage.append("• Email '$email' уже используется\n")
            etEmail.error = "Этот email уже используется"
            if (!usernameExists) etEmail.requestFocus()
        }

        if (!usernameExists && !emailExists) {
            errorMessage.append("• Неизвестная ошибка базы данных\n")
            errorMessage.append("• Проверьте структуру таблицы users\n")
        }

        // Показываем диалог с подробностями
        AlertDialog.Builder(this)
            .setTitle("Ошибка регистрации")
            .setMessage(errorMessage.toString())
            .setPositiveButton("Понятно") { dialog, which ->
                // Сбрасываем фокус
                etUsername.clearFocus()
                etEmail.clearFocus()
            }
            .setNeutralButton("Попробовать другой email") { dialog, which ->
                // Генерируем случайный email для теста
                val randomEmail = "user${Random().nextInt(10000)}@test.com"
                etEmail.setText(randomEmail)
                etEmail.requestFocus()
            }
            .show()
    }

    private fun goToVerification(userId: Long, email: String, code: String) {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra("USER_ID", userId.toInt())
        intent.putExtra("USER_EMAIL", email)
        intent.putExtra("VERIFICATION_CODE", code)
        startActivity(intent)
        finish()
    }

    private fun showCodeManually(code: String, email: String, userId: Long) {
        AlertDialog.Builder(this)
            .setTitle("📧 Код подтверждения")
            .setMessage("""
                На вашем устройстве не найдено почтовое приложение.
                
                Ваш код подтверждения:
                
                🔐 **$code**
                
                Email: $email
                ID пользователя: $userId
                
                Скопируйте код и введите его на следующем экране.
            """.trimIndent())
            .setPositiveButton("Ввести код") { dialog, which ->
                goToVerification(userId, email, code)
            }
            .setNegativeButton("Установить Gmail") { dialog, which ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("market://details?id=com.google.android.gm")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gm")
                    }
                    startActivity(intent)
                }
            }
            .setNeutralButton("Отправить SMS") { dialog, which ->
                // Альтернатива - можно отправить код по SMS
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("smsto:")
                    putExtra("sms_body", "Ваш код подтверждения: $code")
                }
                startActivity(smsIntent)
            }
            .show()
    }

    // Дополнительный метод для тестирования
    private fun testRegistrationWithRandomData() {
        val random = Random()
        val randomNum = random.nextInt(100000)

        etUsername.setText("testuser_$randomNum")
        etPassword.setText("Test123!")
        etConfirmPassword.setText("Test123!")
        etFullName.setText("Тестовый Пользователь")
        etEmail.setText("test_$randomNum@test.com")
        etPhone.setText("+7912${String.format("%07d", random.nextInt(10000000))}")

        // Автоматически запускаем регистрацию
        registerUser()
    }

    // Метод для быстрого теста (можно вызвать из другой кнопки или меню)
    fun quickTest() {
        AlertDialog.Builder(this)
            .setTitle("Быстрый тест")
            .setMessage("Заполнить поля случайными данными и попробовать зарегистрироваться?")
            .setPositiveButton("Да") { dialog, which ->
                testRegistrationWithRandomData()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}
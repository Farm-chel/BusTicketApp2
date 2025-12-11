package com.example.busticketapp2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_registration)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupClickListeners()
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

        // Устанавливаем черный цвет текста программно (на всякий случай)
        etUsername.setTextColor(Color.BLACK)
        etPassword.setTextColor(Color.BLACK)
        etConfirmPassword.setTextColor(Color.BLACK)
        etFullName.setTextColor(Color.BLACK)
        etEmail.setTextColor(Color.BLACK)
        etPhone.setTextColor(Color.BLACK)
    }

    private fun setupTextWatchers() {
        // Валидация в реальном времени
        etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateUsername(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePassword(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePasswordConfirmation()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateFullName(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            registerUser()
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        // Валидация всех полей
        if (!validateAllFields(username, password, confirmPassword, fullName, email)) {
            return
        }

        // Проверяем, не занят ли логин
        if (dbHelper.isUsernameExists(username)) {
            etUsername.error = "Этот логин уже занят"
            etUsername.requestFocus()
            return
        }

        // Создаем пользователя
        val userId = dbHelper.registerUser(username, password, fullName, email, phone)

        if (userId != -1L) {
            // Успешная регистрация
            showSuccessDialog(username)
        } else {
            Toast.makeText(this, "❌ Ошибка при регистрации", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAllFields(
        username: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        email: String
    ): Boolean {
        var isValid = true

        if (!validateUsername(username)) isValid = false
        if (!validatePassword(password)) isValid = false
        if (!validatePasswordConfirmation()) isValid = false
        if (!validateFullName(fullName)) isValid = false
        if (!validateEmail(email)) isValid = false

        return isValid
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                etUsername.error = "Введите логин"
                false
            }
            username.length < 3 -> {
                etUsername.error = "Логин должен быть не менее 3 символов"
                false
            }
            username.length > 20 -> {
                etUsername.error = "Логин должен быть не более 20 символов"
                false
            }
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                etUsername.error = "Логин может содержать только буквы, цифры и _"
                false
            }
            else -> {
                etUsername.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                etPassword.error = "Введите пароль"
                false
            }
            password.length < 6 -> {
                etPassword.error = "Пароль должен быть не менее 6 символов"
                false
            }
            !password.matches(Regex(".*[A-Z].*")) -> {
                etPassword.error = "Пароль должен содержать хотя бы одну заглавную букву"
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                etPassword.error = "Пароль должен содержать хотя бы одну цифру"
                false
            }
            else -> {
                etPassword.error = null
                validatePasswordConfirmation()
                true
            }
        }
    }

    private fun validatePasswordConfirmation(): Boolean {
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        return when {
            confirmPassword.isEmpty() -> {
                etConfirmPassword.error = "Подтвердите пароль"
                false
            }
            password != confirmPassword -> {
                etConfirmPassword.error = "Пароли не совпадают"
                false
            }
            else -> {
                etConfirmPassword.error = null
                true
            }
        }
    }

    private fun validateFullName(fullName: String): Boolean {
        return when {
            fullName.isEmpty() -> {
                etFullName.error = "Введите ФИО"
                false
            }
            fullName.length < 5 -> {
                etFullName.error = "ФИО должно быть не менее 5 символов"
                false
            }
            !fullName.matches(Regex("^[А-Яа-яЁё\\s-]+$")) -> {
                etFullName.error = "ФИО должно содержать только русские буквы, пробелы и дефисы"
                false
            }
            fullName.split(" ").size < 2 -> {
                etFullName.error = "Введите Фамилию и Имя через пробел"
                false
            }
            else -> {
                etFullName.error = null
                true
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                etEmail.error = "Введите email"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Введите корректный email адрес"
                false
            }
            else -> {
                etEmail.error = null
                true
            }
        }
    }

    private fun showSuccessDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("✅ Регистрация успешна!")
            .setMessage("""
                🎉 Поздравляем!
                
                Вы успешно зарегистрированы!
                
                📝 Ваши данные:
                • Логин: $username
                • Роль: Пассажир
                
                Теперь вы можете войти в систему и бронировать билеты.
            """.trimIndent())
            .setPositiveButton("Войти") { dialog, which ->
                // Возвращаемся на экран входа с заполненными данными
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("REGISTERED_USERNAME", username)
                intent.putExtra("REGISTERED_PASSWORD", etPassword.text.toString())
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
package com.example.busticketapp2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper

class EmailVerificationActivity : AppCompatActivity() {

    // Объявляем все переменные для элементов layout
    private lateinit var txtEmail: TextView
    private lateinit var editCode1: EditText
    private lateinit var editCode2: EditText
    private lateinit var editCode3: EditText
    private lateinit var editCode4: EditText
    private lateinit var editCode5: EditText
    private lateinit var editCode6: EditText
    private lateinit var btnVerify: Button
    private lateinit var btnResend: Button
    private lateinit var txtTimer: TextView
    private lateinit var txtError: TextView

    private var userId: Int = -1
    private var userEmail: String = ""
    private var verificationCode: String = ""
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftMillis: Long = 900000 // 15 минут в миллисекундах

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        supportActionBar?.hide()

        // Получаем данные из RegistrationActivity
        userId = intent.getIntExtra("USER_ID", -1)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        verificationCode = intent.getStringExtra("VERIFICATION_CODE") ?: ""

        initViews()
        setupCodeInput()
        startTimer()

        // Проверяем, что данные получены
        if (userId == -1 || userEmail.isEmpty() || verificationCode.isEmpty()) {
            Toast.makeText(this, "Ошибка: данные не получены", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        // Привязываем элементы из layout
        txtEmail = findViewById(R.id.txtEmail)
        editCode1 = findViewById(R.id.editCode1)
        editCode2 = findViewById(R.id.editCode2)
        editCode3 = findViewById(R.id.editCode3)
        editCode4 = findViewById(R.id.editCode4)
        editCode5 = findViewById(R.id.editCode5)
        editCode6 = findViewById(R.id.editCode6)
        btnVerify = findViewById(R.id.btnVerify)
        btnResend = findViewById(R.id.btnResend)
        txtTimer = findViewById(R.id.txtTimer)
        txtError = findViewById(R.id.txtError)

        // Устанавливаем email пользователя
        txtEmail.text = "Код отправлен на: $userEmail"

        // Настраиваем обработчики кнопок
        btnVerify.setOnClickListener {
            verifyCode()
        }

        btnResend.setOnClickListener {
            resendCode()
        }
    }

    private fun setupCodeInput() {
        val codeFields = listOf(editCode1, editCode2, editCode3, editCode4, editCode5, editCode6)

        codeFields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s?.length == 1 && index < 5) {
                        // Автоматический переход к следующему полю
                        codeFields[index + 1].requestFocus()
                    } else if (s?.length == 0 && index > 0) {
                        // Возврат к предыдущему полю при удалении
                        codeFields[index - 1].requestFocus()
                    }

                    // Проверяем, все ли поля заполнены
                    if (codeFields.all { it.text.length == 1 }) {
                        // Автоматическая проверка кода
                        verifyCode()
                    }
                }
            })
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                txtTimer.text = "Время истекло"
                btnVerify.isEnabled = false
                txtError.text = "Код устарел. Запросите новый код."
                txtError.visibility = android.view.View.VISIBLE
            }
        }.start()
    }

    private fun updateTimerText() {
        val minutes = (timeLeftMillis / 1000 / 60).toInt()
        val seconds = (timeLeftMillis / 1000 % 60).toInt()
        txtTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun getEnteredCode(): String {
        return editCode1.text.toString() +
                editCode2.text.toString() +
                editCode3.text.toString() +
                editCode4.text.toString() +
                editCode5.text.toString() +
                editCode6.text.toString()
    }

    private fun verifyCode() {
        val enteredCode = getEnteredCode()

        if (enteredCode.length != 6) {
            showError("Введите все 6 цифр кода")
            shakeCodeFields()
            return
        }

        // Проверяем код через базу данных
        val dbHelper = DatabaseHelper(this)
        val isValid = dbHelper.verifyCode(userId, userEmail, enteredCode)

        if (isValid) {
            // Успешная проверка - активируем пользователя
            dbHelper.activateUser(userId) // Этот метод теперь существует
            showSuccessDialog()
        } else {
            showError("Неверный код. Попробуйте еще раз.")
            shakeCodeFields()
        }
    }

    private fun showError(message: String) {
        txtError.text = message
        txtError.visibility = android.view.View.VISIBLE
    }

    private fun shakeCodeFields() {
        val codeFields = listOf(editCode1, editCode2, editCode3, editCode4, editCode5, editCode6)
        val originalColors = codeFields.map { it.currentTextColor }

        // Меняем цвет на красный
        codeFields.forEach { field ->
            field.setTextColor(Color.RED)
        }

        // Возвращаем исходный цвет через 1 секунду
        Handler(Looper.getMainLooper()).postDelayed({
            codeFields.forEachIndexed { index, field ->
                field.setTextColor(originalColors[index])
            }
        }, 1000)
    }

    private fun resendCode() {
        // 1. Код всегда отправляется на userEmail (тот, который указал пользователь)
        val targetEmail = userEmail // Используем email из данных регистрации

        // 2. Генерируем новый код
        verificationCode = (100000..999999).random().toString()

        // 3. Сохраняем новый код в базу данных для проверки
        val dbHelper = DatabaseHelper(this)
        dbHelper.saveVerificationCode(userId, targetEmail, verificationCode)

        // 4. Сбрасываем таймер
        countDownTimer?.cancel()
        timeLeftMillis = 900000
        startTimer()

        // 5. Очищаем поля ввода
        listOf(editCode1, editCode2, editCode3, editCode4, editCode5, editCode6).forEach {
            it.text.clear()
        }
        editCode1.requestFocus()

        // 6. Скрываем ошибку
        txtError.visibility = android.view.View.GONE
        btnVerify.isEnabled = true

        // 7. Отправляем новый код на ИСХОДНЫЙ email пользователя
        val emailService = EmailIntentService(this)
        val isSent = emailService.sendVerificationCodeViaIntent(targetEmail, verificationCode)

        if (isSent) {
            Toast.makeText(this, "✅ Новый код отправлен на $targetEmail", Toast.LENGTH_SHORT).show()
        } else {
            // Нет почтового приложения - показываем код
            showCodeDialog(verificationCode, targetEmail)
        }
    }

    private fun showCodeDialog(code: String, email: String) {
        AlertDialog.Builder(this)
            .setTitle("📧 Демо-режим")
            .setMessage("Ваш код подтверждения для $email:\n\n🔢 $code\n\nСкопируйте его и введите в поля выше.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCodeDialog(code: String) {
        AlertDialog.Builder(this)
            .setTitle("📧 Демо-режим")
            .setMessage("Ваш код подтверждения:\n\n🔢 $code\n\nСкопируйте его и введите в поля выше.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("✅ Email подтвержден!")
            .setMessage("Поздравляем! Ваш email успешно подтвержден.\n\nТеперь вы можете пользоваться всеми функциями приложения.")
            .setPositiveButton("Войти в приложение") { dialog, which ->
                // Возвращаемся в MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
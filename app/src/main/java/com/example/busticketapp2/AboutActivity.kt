package com.example.busticketapp2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.hide()

        val btnBack: Button = findViewById(R.id.btnBack)
        val txtAbout: TextView = findViewById(R.id.txtAbout)

        val aboutText = """
            ИНФОРМАЦИЯ О СТУДЕНТЕ РАЗРАБОТАВШЕМУ ПРОГРАММУ

            Образование:
            • Среднее – специальное образование
            • Специализация: Информационные системы и программирование

            Профессиональные навыки:
            • Работа с базами данных 
            • Интеграция Google Maps API
            • Владею несколькими языками

            О программе "Автобусные билеты":
            • Понятный интерфейс
            • Интеграция с картами
            • Система отчетности

            Технологии использованные в проекте:
            • Kotlin + Android SDK
            • SQLite для локального хранения данных
            • Google Maps API для отображения маршрутов
            • Material Design для современного UI/UX
            • PDF генерация для чеков

            📞 Контакты:
            • Gmail: g92100199@gmail.com
            • GitHub: github.com/Farm-chel

        """.trimIndent()

        txtAbout.text = aboutText

        btnBack.setOnClickListener {
            finish()
        }
    }
}
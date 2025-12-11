package com.example.busticketapp2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_reports)

        dbHelper = DatabaseHelper(this)

        val btnBack: Button = findViewById(R.id.btnBack)
        val btnToday: Button = findViewById(R.id.btnToday)
        val btnWeek: Button = findViewById(R.id.btnWeek)
        val btnMonth: Button = findViewById(R.id.btnMonth)
        val txtReport: TextView = findViewById(R.id.txtReport)

        btnToday.setOnClickListener {
            val report = dbHelper.getTodaySales()
            txtReport.text = "📊 ОТЧЕТ ЗА СЕГОДНЯ\n\n$report\n\n📅 ${SimpleDateFormat("dd.MM.yyyy").format(Date())}"
        }

        btnWeek.setOnClickListener {
            val report = dbHelper.getWeekSales()
            txtReport.text = "📊 ОТЧЕТ ЗА НЕДЕЛЮ\n\n$report\n\n📅 ${SimpleDateFormat("dd.MM.yyyy").format(Date())}"
        }

        btnMonth.setOnClickListener {
            val report = dbHelper.getMonthSales()
            txtReport.text = "📊 ОТЧЕТ ЗА МЕСЯЦ\n\n$report\n\n📅 ${SimpleDateFormat("dd.MM.yyyy").format(Date())}"
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Показываем отчет за сегодня по умолчанию
        btnToday.performClick()
    }
}
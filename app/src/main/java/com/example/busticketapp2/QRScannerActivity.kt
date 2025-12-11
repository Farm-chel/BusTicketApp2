package com.example.busticketapp2

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class QRScannerActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        dbHelper = DatabaseHelper(this)

        val btnCancel: Button = findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener {
            finish()
        }

        // Показываем сообщение, что сканирование недоступно
        showScanUnavailableMessage()
    }

    private fun showScanUnavailableMessage() {
        AlertDialog.Builder(this)
            .setTitle("Сканирование QR-кодов")
            .setMessage("Функция сканирования QR-кодов временно недоступна.\n\n" +
                    "Вы можете:\n" +
                    "1. Проверить билет по номеру вручную\n" +
                    "2. Сгенерировать QR-код для своего билета\n" +
                    "3. Использовать PDF версию билета")
            .setPositiveButton("Проверить вручную") { dialog, which ->
                showManualCheckDialog()
            }
            .setNegativeButton("Закрыть") { dialog, which ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showManualCheckDialog() {
        // Создаем диалог для ручного ввода номера билета
        AlertDialog.Builder(this)
            .setTitle("Проверка билета")
            .setMessage("Введите номер билета:")
            .setView(android.R.layout.simple_list_item_1) // Используем системный layout
            .setPositiveButton("Проверить") { dialog, which ->
                // Для демо проверяем билет с ID 1
                checkTicketById(1)
            }
            .setNegativeButton("Отмена") { dialog, which ->
                finish()
            }
            .show()
    }

    private fun checkTicketById(bookingId: Int) {
        val bookingWithTrip = dbHelper.getBookingWithTripInfo(bookingId)
        if (bookingWithTrip != null) {
            val (booking, trip) = bookingWithTrip
            showBookingInfo(booking, trip)
        } else {
            Toast.makeText(this, "Билет №$bookingId не найден", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showBookingInfo(booking: com.example.busticketapp2.models.Booking,
                                trip: com.example.busticketapp2.models.Trip) {
        val formattedDate = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(booking.tripDate)
            if (date != null) {
                outputFormat.format(date)
            } else {
                booking.tripDate
            }
        } catch (e: Exception) {
            booking.tripDate
        }

        val message = """
            ✅ Билет проверен
            Номер: ${booking.id}
            Пассажир: ${booking.passengerName}
            Маршрут: ${trip.fromCity} → ${trip.toCity}
            Дата: $formattedDate
            Время: ${trip.departureTime}
            Место: ${booking.seatNumber}
            Статус: ${booking.status}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("✅ Результат проверки")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, which ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
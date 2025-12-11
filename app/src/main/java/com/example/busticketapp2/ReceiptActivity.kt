package com.example.busticketapp2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Booking
import com.example.busticketapp2.models.Trip
import java.io.File

class ReceiptActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var pdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        val txtReceipt: TextView = findViewById(R.id.txtReceipt)
        val btnBack: Button = findViewById(R.id.btnBack)
        val btnShare: Button = findViewById(R.id.btnShare)
        val btnGeneratePdf: Button = findViewById(R.id.btnGeneratePdf)

        val bookingId = intent.getIntExtra("BOOKING_ID", -1)
        Log.d("ReceiptActivity", "Booking ID received: $bookingId")

        if (bookingId != -1) {
            val bookingWithTrip = dbHelper.getBookingWithTripInfo(bookingId)
            if (bookingWithTrip != null) {
                val (booking, trip) = bookingWithTrip
                Log.d("ReceiptActivity", "Booking found: $booking")
                Log.d("ReceiptActivity", "Trip found: $trip")

                val user = dbHelper.getUserById(booking.userId)
                Log.d("ReceiptActivity", "User found: $user")

                // Показываем информацию о билете
                showTicketInfo(txtReceipt, booking, trip, user)

                // Генерируем PDF при создании активности
                generatePdf(booking, trip, user)
            } else {
                txtReceipt.text = "Ошибка: билет не найден (ID: $bookingId)"
                Log.e("ReceiptActivity", "Booking not found for ID: $bookingId")
            }
        } else {
            txtReceipt.text = "Ошибка: ID билета не передан"
            Log.e("ReceiptActivity", "No booking ID received")
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnShare.setOnClickListener {
            sharePdf()
        }

        btnGeneratePdf.setOnClickListener {
            val currentBookingId = intent.getIntExtra("BOOKING_ID", -1)
            if (currentBookingId != -1) {
                val bookingWithTrip = dbHelper.getBookingWithTripInfo(currentBookingId)
                if (bookingWithTrip != null) {
                    val (booking, trip) = bookingWithTrip
                    val user = dbHelper.getUserById(booking.userId)
                    generatePdf(booking, trip, user)
                }
            }
        }
    }

    private fun showTicketInfo(textView: TextView, booking: Booking,
                               trip: Trip, user: com.example.busticketapp2.models.User?) {
        val formattedDate = try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(booking.tripDate)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            booking.tripDate
        }

        val ticketInfo = """
            🎫 АВТОБУСНЫЙ БИЛЕТ
            
            📅 Дата поездки: $formattedDate
            📍 Маршрут: ${trip.fromCity} → ${trip.toCity}
            ⏰ Время: ${trip.departureTime}
            👤 Пассажир: ${booking.passengerName}
            📧 Email: ${booking.passengerEmail}
            💺 Место: ${booking.seatNumber}
            💰 Стоимость: ${trip.price.toInt()} руб.
            📅 Дата брони: ${booking.bookingDate}
            👑 Владелец: ${user?.fullName ?: "Не указан"}
            📧 Email владельца: ${user?.email ?: "Не указан"}
            🔢 Номер билета: ${booking.id}
            ✅ Статус: ${booking.status}
            
            Для получения PDF версии нажмите "Создать PDF"
        """.trimIndent()

        textView.text = ticketInfo
    }

    private fun generatePdf(booking: Booking,
                            trip: Trip,
                            user: com.example.busticketapp2.models.User?) {
        try {
            Log.d("ReceiptActivity", "Starting PDF generation...")
            val pdfGenerator = PdfGenerator(this)
            pdfFile = pdfGenerator.generateTicketPdf(booking, trip, user)

            if (pdfFile != null) {
                Log.d("ReceiptActivity", "PDF created successfully: ${pdfFile?.absolutePath}")
                Toast.makeText(this, "PDF билет создан!", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("ReceiptActivity", "PDF creation returned null")
                Toast.makeText(this, "Ошибка создания PDF - файл не создан", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ReceiptActivity", "Error generating PDF: ${e.message}", e)
            Toast.makeText(this, "Ошибка создания PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdf() {
        pdfFile?.let { file ->
            if (file.exists()) {
                try {
                    Log.d("ReceiptActivity", "Sharing PDF: ${file.absolutePath}")
                    val uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Автобусный билет №${file.nameWithoutExtension}")
                        putExtra(Intent.EXTRA_TEXT, "Ваш автобусный билет")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(shareIntent, "Поделиться билетом"))
                } catch (e: Exception) {
                    Log.e("ReceiptActivity", "Error sharing PDF: ${e.message}", e)
                    Toast.makeText(this, "Ошибка при открытии PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e("ReceiptActivity", "PDF file does not exist: ${file.absolutePath}")
                Toast.makeText(this, "PDF файл не найден. Создайте его сначала.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e("ReceiptActivity", "PDF file is null")
            Toast.makeText(this, "Сначала создайте PDF файл", Toast.LENGTH_SHORT).show()
        }
    }
}
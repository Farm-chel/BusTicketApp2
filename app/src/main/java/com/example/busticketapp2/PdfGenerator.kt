package com.example.busticketapp2

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.busticketapp2.models.Booking
import com.example.busticketapp2.models.Trip
import com.example.busticketapp2.models.User
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {

    private val borderWidth = 2f
    private val pageWidth = 595f
    private val pageHeight = 842f
    private val margin = 40f
    private val contentWidth = pageWidth - 2 * margin

    fun generateTicketPdf(booking: Booking, trip: Trip, user: User?): File? {
        return try {
            Log.d("PdfGenerator", "Starting PDF generation for booking ${booking.id}")

            val document = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                pageWidth.toInt(),
                pageHeight.toInt(),
                1
            ).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawBackground(canvas)
            drawHeader(canvas, booking.id)
            drawTicketInfo(canvas, booking, trip, user)
            drawDivider(canvas)

            // Добавляем сообщение вместо QR-кода
            drawQRPlaceholder(canvas, booking.id)

            drawInstructions(canvas)
            drawFooter(canvas)

            document.finishPage(page)

            val file = createPdfFile(booking.id)
            Log.d("PdfGenerator", "PDF file path: ${file.absolutePath}")

            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            Log.d("PdfGenerator", "PDF successfully created")
            file
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error creating PDF: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(context, "Ошибка создания PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint()
        borderPaint.color = Color.parseColor("#E0E0E0")
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1f

        canvas.drawRect(
            margin - 5,
            margin - 5,
            pageWidth - margin + 5,
            pageHeight - margin + 5,
            borderPaint
        )

        val gradientPaint = Paint()
        gradientPaint.shader = LinearGradient(
            0f, 0f,
            pageWidth, 100f,
            Color.parseColor("#E3F2FD"),
            Color.WHITE,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, pageWidth, 100f, gradientPaint)
    }

    private fun drawHeader(canvas: Canvas, bookingId: Int) {
        val titlePaint = Paint()
        titlePaint.color = Color.parseColor("#1976D2")
        titlePaint.textSize = 22f
        titlePaint.isFakeBoldText = true
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("АВТОБУСНЫЙ БИЛЕТ", pageWidth / 2, 50f, titlePaint)

        val subtitlePaint = Paint()
        subtitlePaint.color = Color.parseColor("#2196F3")
        subtitlePaint.textSize = 14f
        subtitlePaint.textAlign = Paint.Align.CENTER
        subtitlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText("Электронный билет №$bookingId", pageWidth / 2, 70f, subtitlePaint)

        val linePaint = Paint()
        linePaint.color = Color.parseColor("#1976D2")
        linePaint.strokeWidth = 2f

        canvas.drawLine(margin, 85f, pageWidth - margin, 85f, linePaint)
    }

    private fun drawTicketInfo(canvas: Canvas, booking: Booking, trip: Trip, user: User?) {
        var yPosition = 120f
        val column1X = margin
        val column2X = pageWidth / 2
        val rowHeight = 25f

        val sectionPaint = Paint()
        sectionPaint.color = Color.parseColor("#2E7D32")
        sectionPaint.textSize = 16f
        sectionPaint.isFakeBoldText = true
        sectionPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("ИНФОРМАЦИЯ О ПОЕЗДКЕ", margin, yPosition, sectionPaint)
        yPosition += 30f

        val formattedTripDate = formatDate(booking.tripDate, "yyyy-MM-dd", "dd.MM.yyyy")
        val formattedBookingDate = formatDate(booking.bookingDate, "yyyy-MM-dd", "dd.MM.yyyy")

        val labelPaint = Paint()
        labelPaint.color = Color.parseColor("#424242")
        labelPaint.textSize = 11f
        labelPaint.isFakeBoldText = true

        val valuePaint = Paint()
        valuePaint.color = Color.BLACK
        valuePaint.textSize = 11f
        valuePaint.isFakeBoldText = false

        // Колонка 1
        drawInfoRow(canvas, column1X, yPosition, "📅 Дата поездки:", formattedTripDate, labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column1X, yPosition, "📍 Маршрут:",
            "${trip.fromCity} → ${trip.toCity}", labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column1X, yPosition, "⏰ Время отправления:",
            trip.departureTime, labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column1X, yPosition, "⏱️ Время прибытия:",
            trip.arrivalTime, labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column1X, yPosition, "👤 Пассажир:",
            booking.passengerName, labelPaint, valuePaint)
        yPosition += rowHeight

        // Колонка 2
        yPosition = 150f

        drawInfoRow(canvas, column2X, yPosition, "📧 Email пассажира:",
            booking.passengerEmail, labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column2X, yPosition, "💺 Номер места:",
            booking.seatNumber.toString(), labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column2X, yPosition, "💰 Стоимость:",
            "${trip.price.toInt()} руб.", labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column2X, yPosition, "📅 Дата бронирования:",
            formattedBookingDate, labelPaint, valuePaint)
        yPosition += rowHeight

        drawInfoRow(canvas, column2X, yPosition, "✅ Статус билета:",
            booking.status, labelPaint, valuePaint)
        yPosition += rowHeight

        // Информация о владельце аккаунта
        if (user != null) {
            yPosition += 10f
            val ownerLabelPaint = Paint()
            ownerLabelPaint.color = Color.parseColor("#757575")
            ownerLabelPaint.textSize = 10f
            ownerLabelPaint.isFakeBoldText = true

            val ownerValuePaint = Paint()
            ownerValuePaint.color = Color.parseColor("#424242")
            ownerValuePaint.textSize = 10f

            canvas.drawText(
                "Информация о владельце аккаунта:",
                margin, yPosition, ownerLabelPaint
            )
            yPosition += 15f

            drawInfoRow(
                canvas, margin, yPosition, "👑 ФИО:", user.fullName,
                ownerLabelPaint, ownerValuePaint
            )
            yPosition += rowHeight - 5

            drawInfoRow(
                canvas, margin, yPosition, "📧 Email:", user.email,
                ownerLabelPaint, ownerValuePaint
            )

            // Добавить больше отступа перед QR-кодом
            yPosition += 10f
        }
    }

    private fun drawInfoRow(canvas: Canvas, x: Float, y: Float,
                            label: String, value: String,
                            labelPaint: Paint, valuePaint: Paint) {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x + 120f, y, valuePaint)
    }

    private fun drawDivider(canvas: Canvas) {
        val dividerY = 280f

        val dashPaint = Paint()
        dashPaint.color = Color.parseColor("#BDBDBD")
        dashPaint.strokeWidth = 1f
        dashPaint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)

        canvas.drawLine(margin, dividerY, pageWidth - margin, dividerY, dashPaint)
    }

    private fun drawQRPlaceholder(canvas: Canvas, bookingId: Int) {
        val qrSize = 180f
        // Смещаем QR-код вправо и ОПУСКАЕМ НИЖЕ
        val qrX = pageWidth - margin - qrSize - 30f
        val qrY = 350f  // Было 310f, теперь 350f (опустили на 40 пикселей)

        val backgroundPaint = Paint()
        backgroundPaint.color = Color.WHITE
        backgroundPaint.style = Paint.Style.FILL

        val qrBackground = RectF(qrX - 15f, qrY - 15f,
            qrX + qrSize + 15f, qrY + qrSize + 15f)
        canvas.drawRoundRect(qrBackground, 10f, 10f, backgroundPaint)

        val borderPaint = Paint()
        borderPaint.color = Color.parseColor("#1976D2")
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(qrBackground, 10f, 10f, borderPaint)

        // Рисуем плюс вместо QR-кода
        val plusPaint = Paint()
        plusPaint.color = Color.parseColor("#1976D2")
        plusPaint.strokeWidth = 8f
        plusPaint.style = Paint.Style.STROKE

        val centerX = qrX + qrSize / 2
        val centerY = qrY + qrSize / 2
        val lineLength = qrSize / 3

        // Вертикальная линия
        canvas.drawLine(centerX, centerY - lineLength, centerX, centerY + lineLength, plusPaint)
        // Горизонтальная линия
        canvas.drawLine(centerX - lineLength, centerY, centerX + lineLength, centerY, plusPaint)

        val qrTitlePaint = Paint()
        qrTitlePaint.color = Color.parseColor("#1976D2")
        qrTitlePaint.textSize = 12f
        qrTitlePaint.isFakeBoldText = true
        qrTitlePaint.textAlign = Paint.Align.CENTER
        qrTitlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // Текст над QR-кодом тоже опускаем
        canvas.drawText("QR-КОД БИЛЕТА", qrX + qrSize / 2, qrY - 25f, qrTitlePaint)

        val qrTextPaint = Paint()
        qrTextPaint.color = Color.BLACK
        qrTextPaint.textSize = 9f
        qrTextPaint.textAlign = Paint.Align.CENTER

        canvas.drawText("ID билета: #$bookingId",
            qrX + qrSize / 2, qrY + qrSize + 20f, qrTextPaint)
    }

    private fun drawInstructions(canvas: Canvas) {
        val startY = 580f  // Было 550f, теперь 580f (опустили на 30 пикселей)

        val titlePaint = Paint()
        titlePaint.color = Color.parseColor("#2E7D32")
        titlePaint.textSize = 14f
        titlePaint.isFakeBoldText = true
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("ИНСТРУКЦИЯ ПО ИСПОЛЬЗОВАНИЮ", margin, startY, titlePaint)

        val textPaint = Paint()
        textPaint.color = Color.parseColor("#424242")
        textPaint.textSize = 10f

        var yPos = startY + 25f
        val textX = margin
        val lineSpacing = 18f

        canvas.drawText("• Распечатайте этот билет или сохраните на устройстве",
            textX, yPos, textPaint)
        yPos += lineSpacing

        canvas.drawText("• Предъявите билет контролеру при посадке в автобус",
            textX, yPos, textPaint)
        yPos += lineSpacing

        canvas.drawText("• Имейте при себе документ, удостоверяющий личность",
            textX, yPos, textPaint)
        yPos += lineSpacing

        canvas.drawText("• Приходите на посадку за 15 минут до отправления",
            textX, yPos, textPaint)
    }

    private fun drawFooter(canvas: Canvas) {
        val footerY = pageHeight - margin - 50f

        val linePaint = Paint()
        linePaint.color = Color.parseColor("#1976D2")
        linePaint.strokeWidth = 1f
        canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)

        val footerPaint = Paint()
        footerPaint.color = Color.parseColor("#757575")
        footerPaint.textSize = 8f
        footerPaint.textAlign = Paint.Align.CENTER

        // Год 2025
        canvas.drawText("© 2025 Автобусные билеты. Все права защищены.",
            pageWidth / 2, footerY, footerPaint)

        canvas.drawText("Тел. поддержки: 8-800-123-45-67 | Email: support@bus-tickets.ru",
            pageWidth / 2, footerY + 12f, footerPaint)

        val timeStamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Билет сгенерирован: $timeStamp",
            pageWidth / 2, footerY + 24f, footerPaint)
    }

    private fun formatDate(dateString: String, inputFormat: String, outputFormat: String): String {
        return try {
            val input = SimpleDateFormat(inputFormat, Locale.getDefault())
            val output = SimpleDateFormat(outputFormat, Locale.getDefault())
            val date = input.parse(dateString)
            if (date != null) {
                output.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    private fun createPdfFile(bookingId: Int): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())

        val fileName = "BusTicket_${bookingId}_${currentDate}_${currentTime}.pdf"

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        val ticketsDir = File(storageDir, "Tickets")
        if (!ticketsDir.exists()) {
            ticketsDir.mkdirs()
        }

        return File(ticketsDir, fileName)
    }
}
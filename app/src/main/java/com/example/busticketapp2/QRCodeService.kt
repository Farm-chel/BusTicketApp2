package com.example.busticketapp2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

class QRCodeService(private val context: Context) {

    /**
     * Создает чистый QR-код без изображения внутри
     */
    fun generateQRCode(content: String, size: Int): Bitmap {
        return SimpleQRGenerator.generateStyledQRCode(content, size, Color.BLACK, Color.WHITE)
    }

    /**
     * Создает QR-код с кастомными цветами
     */
    fun generateQRCode(
        content: String,
        size: Int,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        return SimpleQRGenerator.generateStyledQRCode(content, size, foregroundColor, backgroundColor)
    }

    /**
     * Создает QR-код для билета
     */
    fun generateTicketQRCode(
        bookingId: Int,
        passengerName: String,
        fromCity: String,
        toCity: String,
        departureTime: String,
        seatNumber: Int,
        tripDate: String,
        size: Int = 400
    ): Bitmap {
        return SimpleQRGenerator.generateTicketQR(
            bookingId, passengerName, fromCity, toCity, departureTime, seatNumber, tripDate, size
        )
    }
}
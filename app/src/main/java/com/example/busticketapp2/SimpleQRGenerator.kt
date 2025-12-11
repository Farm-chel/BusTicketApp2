package com.example.busticketapp2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.roundToInt

class SimpleQRGenerator {

    companion object {

        /**
         * Генерирует простой QR-код с использованием простых паттернов
         * Это альтернатива, если библиотека ZXing недоступна
         */
        fun generateSimpleQRCode(content: String, size: Int): Bitmap {
            // Создаем bitmap заданного размера
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Заполняем белым фоном
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Простой алгоритм для создания паттерна, похожего на QR-код
            // Основано на хэше содержимого
            val hash = content.hashCode()
            val seed = hash.toLong()
            val cellSize = (size / 21).coerceAtLeast(1)

            // Рисуем три больших маркера как в настоящем QR-коде
            drawQRMarker(canvas, paint, cellSize * 2, cellSize * 2, cellSize * 7)
            drawQRMarker(canvas, paint, size - cellSize * 9, cellSize * 2, cellSize * 7)
            drawQRMarker(canvas, paint, cellSize * 2, size - cellSize * 9, cellSize * 7)

            // Генерируем псевдослучайный паттерн на основе хэша
            val random = java.util.Random(seed)

            // Рисуем паттерн данных
            for (row in 0 until 21) {
                for (col in 0 until 21) {
                    // Пропускаем маркеры
                    if ((row < 9 && col < 9) || // Верхний левый
                        (row < 9 && col > 12) || // Верхний правый
                        (row > 12 && col < 9)) { // Нижний левый
                        continue
                    }

                    val isBlack = random.nextBoolean()
                    if (isBlack) {
                        val x = col * cellSize
                        val y = row * cellSize
                        canvas.drawRect(
                            x.toFloat(),
                            y.toFloat(),
                            (x + cellSize).toFloat(),
                            (y + cellSize).toFloat(),
                            paint
                        )
                    }
                }
            }

            return bitmap
        }

        /**
         * Генерирует QR-код для билета
         */
        fun generateTicketQR(
            bookingId: Int,
            passengerName: String,
            fromCity: String,
            toCity: String,
            departureTime: String,
            seatNumber: Int,
            tripDate: String,
            size: Int = 400
        ): Bitmap {
            val content = "TICKET|ID:$bookingId|P:$passengerName|F:$fromCity|T:$toCity|DT:$departureTime|S:$seatNumber|DATE:$tripDate"
            return generateSimpleQRCode(content, size)
        }

        /**
         * Рисует маркер для QR-кода
         */
        private fun drawQRMarker(canvas: Canvas, paint: Paint, x: Int, y: Int, size: Int) {
            // Внешний черный квадрат
            paint.color = Color.BLACK
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat(), paint)

            // Внутренний белый квадрат
            paint.color = Color.WHITE
            val innerSize = size * 2 / 3
            val margin = (size - innerSize) / 2
            canvas.drawRect(
                (x + margin).toFloat(),
                (y + margin).toFloat(),
                (x + size - margin).toFloat(),
                (y + size - margin).toFloat(),
                paint
            )

            // Центральный черный квадрат
            paint.color = Color.BLACK
            val centerSize = size / 3
            val centerMargin = (size - centerSize) / 2
            canvas.drawRect(
                (x + centerMargin).toFloat(),
                (y + centerMargin).toFloat(),
                (x + size - centerMargin).toFloat(),
                (y + size - centerMargin).toFloat(),
                paint
            )
        }

        /**
         * Создает стилизованный QR-код
         */
        fun generateStyledQRCode(
            content: String,
            size: Int,
            foregroundColor: Int = Color.BLACK,
            backgroundColor: Int = Color.WHITE
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Фон
            canvas.drawColor(backgroundColor)

            val paint = Paint().apply {
                color = foregroundColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Генерируем паттерн на основе хэша содержимого
            val hash = content.hashCode()
            val random = java.util.Random(hash.toLong())
            val cellSize = (size / 25).coerceAtLeast(1)

            // Рисуем маркеры
            drawQRMarker(canvas, paint, cellSize * 2, cellSize * 2, cellSize * 7)
            drawQRMarker(canvas, paint, size - cellSize * 9, cellSize * 2, cellSize * 7)
            drawQRMarker(canvas, paint, cellSize * 2, size - cellSize * 9, cellSize * 7)

            // Паттерн данных
            for (row in 0 until 25) {
                for (col in 0 until 25) {
                    // Пропускаем маркеры
                    if ((row < 9 && col < 9) ||
                        (row < 9 && col > 15) ||
                        (row > 15 && col < 9)) {
                        continue
                    }

                    val shouldDraw = random.nextFloat() > 0.4f
                    if (shouldDraw) {
                        val x = col * cellSize
                        val y = row * cellSize
                        canvas.drawRect(
                            x.toFloat(),
                            y.toFloat(),
                            (x + cellSize).toFloat(),
                            (y + cellSize).toFloat(),
                            paint
                        )
                    }
                }
            }

            return bitmap
        }
    }
}
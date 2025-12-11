package com.example.busticketapp2.Data

import android.util.Log
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.busticketapp2.models.Booking
import com.example.busticketapp2.models.Stop
import com.example.busticketapp2.models.Trip
import com.example.busticketapp2.models.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BusTicketDB"
        private const val DATABASE_VERSION = 6

        // Таблица пользователей
        private const val TABLE_USERS = "users"
        private const val KEY_USER_ID = "id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ROLE = "role"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_CREATED_DATE = "created_date"

        // Таблица рейсов
        private const val TABLE_TRIPS = "trips"
        private const val KEY_TRIP_ID = "id"
        private const val KEY_FROM_CITY = "from_city"
        private const val KEY_TO_CITY = "to_city"
        private const val KEY_DEPARTURE_TIME = "departure_time"
        private const val KEY_ARRIVAL_TIME = "arrival_time"
        private const val KEY_PRICE = "price"
        private const val KEY_STATUS = "status"

        // Таблица остановок
        private const val TABLE_STOPS = "stops"
        private const val KEY_STOP_ID = "id"
        private const val KEY_TRIP_ID_FK = "trip_id"
        private const val KEY_STOP_NAME = "stop_name"
        private const val KEY_ARRIVAL_TIME_STOP = "arrival_time"
        private const val KEY_DEPARTURE_TIME_STOP = "departure_time"
        private const val KEY_PRICE_FROM_START = "price_from_start"

        // Таблица бронирований
        private const val TABLE_BOOKINGS = "bookings"
        private const val KEY_BOOKING_ID = "id"
        private const val KEY_USER_ID_FK = "user_id"
        private const val KEY_TRIP_ID_BOOKING = "trip_id"
        private const val KEY_PASSENGER_NAME = "passenger_name"
        private const val KEY_PASSENGER_EMAIL = "passenger_email"
        private const val KEY_BOOKING_DATE = "booking_date"
        private const val KEY_BOOKING_STATUS = "status"
        private const val KEY_SEAT_NUMBER = "seat_number"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Создание таблиц
        val CREATE_USERS_TABLE = """
            CREATE TABLE $TABLE_USERS (
                $KEY_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USERNAME TEXT UNIQUE NOT NULL,
                $KEY_PASSWORD TEXT NOT NULL,
                $KEY_ROLE TEXT NOT NULL,
                $KEY_FULL_NAME TEXT NOT NULL,
                $KEY_EMAIL TEXT,
                $KEY_PHONE TEXT,
                $KEY_CREATED_DATE TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        val CREATE_TRIPS_TABLE = """
            CREATE TABLE $TABLE_TRIPS (
                $KEY_TRIP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_FROM_CITY TEXT NOT NULL,
                $KEY_TO_CITY TEXT NOT NULL,
                $KEY_DEPARTURE_TIME TEXT NOT NULL,
                $KEY_ARRIVAL_TIME TEXT NOT NULL,
                $KEY_PRICE REAL NOT NULL,
                $KEY_STATUS TEXT DEFAULT 'Активен'
            )
        """.trimIndent()

        val CREATE_STOPS_TABLE = """
            CREATE TABLE $TABLE_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TRIP_ID_FK INTEGER NOT NULL,
                $KEY_STOP_NAME TEXT NOT NULL,
                $KEY_ARRIVAL_TIME_STOP TEXT NOT NULL,
                $KEY_DEPARTURE_TIME_STOP TEXT NOT NULL,
                $KEY_PRICE_FROM_START REAL NOT NULL,
                FOREIGN KEY ($KEY_TRIP_ID_FK) REFERENCES $TABLE_TRIPS($KEY_TRIP_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val CREATE_BOOKINGS_TABLE = """
            CREATE TABLE $TABLE_BOOKINGS (
                $KEY_BOOKING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID_FK INTEGER NOT NULL,
                $KEY_TRIP_ID_BOOKING INTEGER NOT NULL,
                $KEY_PASSENGER_NAME TEXT NOT NULL,
                $KEY_PASSENGER_EMAIL TEXT NOT NULL,
                $KEY_BOOKING_DATE TEXT DEFAULT CURRENT_TIMESTAMP,
                $KEY_BOOKING_STATUS TEXT DEFAULT 'Активен',
                $KEY_SEAT_NUMBER INTEGER DEFAULT 0,
                FOREIGN KEY ($KEY_USER_ID_FK) REFERENCES $TABLE_USERS($KEY_USER_ID),
                FOREIGN KEY ($KEY_TRIP_ID_BOOKING) REFERENCES $TABLE_TRIPS($KEY_TRIP_ID)
            )
        """.trimIndent()

        db.execSQL(CREATE_USERS_TABLE)
        db.execSQL(CREATE_TRIPS_TABLE)
        db.execSQL(CREATE_STOPS_TABLE)
        db.execSQL(CREATE_BOOKINGS_TABLE)

        insertInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            Log.d("DB_DEBUG", "Upgrading DB from $oldVersion to $newVersion")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_STOPS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TRIPS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
            Log.d("DB_DEBUG", "DB upgraded successfully")
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error during DB upgrade: ${e.message}", e)
            throw e
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun insertInitialData(db: SQLiteDatabase) {
        // Пользователи
        insertUser(db, "admin", "admin", "Администратор", "Администратор Системы", "admin@example.com", "+79123456789")
        insertUser(db, "cashier", "cashier", "Кассир", "Иванова Анна Петровна", "cashier@example.com", "+79123456790")
        insertUser(db, "user", "user", "Пассажир", "Петров Петр Петрович", "user@example.com", "+79123456791")

        // Рейсы - 4 РЕЙСОВ
        val trip1Id = insertTrip(db, "Слободской", "Киров", "08:00", "10:30", 240.0)
        val trip2Id = insertTrip(db, "Киров", "Слободской", "14:00", "16:30", 240.0)
        val trip3Id = insertTrip(db, "Киров", "Котельнич", "09:30", "15:30", 600.0)
        val trip4Id = insertTrip(db, "Котельнич", "Киров", "16:00", "22:00", 600.0)
        // Маршрут 1: Слободской-Киров (42 остановки)
        insertStop(db, trip1Id, "Автостанция Слободского", "08:00", "08:00", 0.0)
        insertStop(db, trip1Id, "Рождественская улица", "08:02", "08:03", 5.7)
        insertStop(db, trip1Id, "Улица Грина", "08:04", "08:05", 11.4)
        insertStop(db, trip1Id, "ДЭП-4", "08:06", "08:07", 17.1)
        insertStop(db, trip1Id, "Стулово", "08:08", "08:09", 22.8)
        insertStop(db, trip1Id, "ПМК-14", "08:10", "08:11", 28.5)
        insertStop(db, trip1Id, "Ситники", "08:12", "08:13", 34.2)
        insertStop(db, trip1Id, "Первомайский поворот", "08:14", "08:15", 39.9)
        insertStop(db, trip1Id, "Подсобное хозяйство", "08:16", "08:17", 45.6)
        insertStop(db, trip1Id, "По требованию (Вахруши)", "08:18", "08:19", 51.3)
        insertStop(db, trip1Id, "Школа", "08:20", "08:21", 57.0)
        insertStop(db, trip1Id, "Вахруши", "08:22", "08:23", 62.7)
        insertStop(db, trip1Id, "Рубежница", "08:24", "08:25", 68.4)
        insertStop(db, trip1Id, "Логуновы", "08:26", "08:27", 74.1)
        insertStop(db, trip1Id, "Осинцы", "08:28", "08:29", 79.8)
        insertStop(db, trip1Id, "По требованию (Луза)", "08:30", "08:31", 85.5)
        insertStop(db, trip1Id, "Луза", "08:32", "08:33", 91.2)
        insertStop(db, trip1Id, "Сады Биохимик-2", "08:34", "08:35", 96.9)
        insertStop(db, trip1Id, "Зониха", "08:36", "08:37", 102.6)
        insertStop(db, trip1Id, "Пантелеевы", "08:38", "08:39", 108.3)
        insertStop(db, trip1Id, "Столбово", "08:40", "08:41", 114.0)
        insertStop(db, trip1Id, "Шихово", "08:42", "08:43", 119.7)
        insertStop(db, trip1Id, "По требованию", "08:44", "08:45", 125.4)
        insertStop(db, trip1Id, "Трушковы", "08:46", "08:47", 131.1)
        insertStop(db, trip1Id, "Бобинский поворот", "08:48", "08:49", 136.8)
        insertStop(db, trip1Id, "Новомакарьевское кладбище", "08:50", "08:51", 142.5)
        insertStop(db, trip1Id, "Порошинский поворот", "08:52", "08:53", 148.2)
        insertStop(db, trip1Id, "Слобода Макарье", "08:54", "08:55", 153.9)
        insertStop(db, trip1Id, "Троицкая церковь", "08:56", "08:57", 159.6)
        insertStop(db, trip1Id, "Проезжая улица", "08:58", "08:59", 165.3)
        insertStop(db, trip1Id, "Заповедная улица", "09:00", "09:01", 171.0)
        insertStop(db, trip1Id, "Улица Красный Химик", "09:02", "09:03", 176.7)
        insertStop(db, trip1Id, "Слобода Дымково", "09:04", "09:05", 182.4)
        insertStop(db, trip1Id, "Профсоюзная улица", "09:06", "09:07", 188.1)
        insertStop(db, trip1Id, "Улица МОПРа", "09:08", "09:09", 193.8)
        insertStop(db, trip1Id, "Храм Иоанна Предтечи", "09:10", "09:11", 199.5)
        insertStop(db, trip1Id, "Трифонов монастырь", "09:12", "09:13", 205.2)
        insertStop(db, trip1Id, "Филармония", "09:14", "09:15", 210.9)
        insertStop(db, trip1Id, "Областная больница", "09:16", "09:17", 216.6)
        insertStop(db, trip1Id, "ЦУМ", "09:18", "09:19", 222.3)
        insertStop(db, trip1Id, "Автовокзал Киров", "10:30", "10:30", 240.0)

        // Маршрут 2: Киров-Слободской (21 остановка)
        insertStop(db, trip2Id, "Автовокзал Киров", "14:00", "14:00", 0.0)
        insertStop(db, trip2Id, "ЦУМ", "14:02", "14:03", 11.4)
        insertStop(db, trip2Id, "Областная больница", "14:04", "14:05", 22.8)
        insertStop(db, trip2Id, "Филармония", "14:06", "14:07", 34.2)
        insertStop(db, trip2Id, "Трифонов монастырь", "14:08", "14:09", 45.6)
        insertStop(db, trip2Id, "Храм Иоанна Предтечи", "14:10", "14:11", 57.0)
        insertStop(db, trip2Id, "Улица МОПРа", "14:12", "14:13", 68.4)
        insertStop(db, trip2Id, "Профсоюзная улица", "14:14", "14:15", 79.8)
        insertStop(db, trip2Id, "Слобода Дымково", "14:16", "14:17", 91.2)
        insertStop(db, trip2Id, "Улица Красный Химик", "14:18", "14:19", 102.6)
        insertStop(db, trip2Id, "Заповедная улица", "14:20", "14:21", 114.0)
        insertStop(db, trip2Id, "Проезжая улица", "14:22", "14:23", 125.4)
        insertStop(db, trip2Id, "Троицкая церковь", "14:24", "14:25", 136.8)
        insertStop(db, trip2Id, "Слобода Макарье", "14:26", "14:27", 148.2)
        insertStop(db, trip2Id, "Порошинский поворот", "14:28", "14:29", 159.6)
        insertStop(db, trip2Id, "Новомакарьевское кладбище", "14:30", "14:31", 171.0)
        insertStop(db, trip2Id, "Бобинский поворот", "14:32", "14:33", 182.4)
        insertStop(db, trip2Id, "Трушковы", "14:34", "14:35", 193.8)
        insertStop(db, trip2Id, "По требованию", "14:36", "14:37", 205.2)
        insertStop(db, trip2Id, "Шихово", "14:38", "14:39", 216.6)
        insertStop(db, trip2Id, "Столбово", "14:40", "14:41", 228.0)
        insertStop(db, trip2Id, "Пантелеевы", "14:42", "14:43", 239.4)
        insertStop(db, trip2Id, "Зониха", "14:44", "14:45", 250.8)
        insertStop(db, trip2Id, "Сады Биохимик", "14:46", "14:47", 262.2)
        insertStop(db, trip2Id, "Луза", "14:48", "14:49", 273.6)
        insertStop(db, trip2Id, "По требованию (Луза)", "14:50", "14:51", 285.0)
        insertStop(db, trip2Id, "Осинцы", "14:52", "14:53", 296.4)
        insertStop(db, trip2Id, "Логуновы", "14:54", "14:55", 307.8)
        insertStop(db, trip2Id, "Рубежница", "14:56", "14:57", 319.2)
        insertStop(db, trip2Id, "Вахруши", "14:58", "14:59", 330.6)
        insertStop(db, trip2Id, "Школа", "15:00", "15:01", 342.0)
        insertStop(db, trip2Id, "По требованию (Вахруши)", "15:02", "15:03", 353.4)
        insertStop(db, trip2Id, "Подсобное хозяйство", "15:04", "15:05", 364.8)
        insertStop(db, trip2Id, "Первомайский поворот", "15:06", "15:07", 376.2)
        insertStop(db, trip2Id, "Ситники", "15:08", "15:09", 387.6)
        insertStop(db, trip2Id, "ПМК-14", "15:10", "15:11", 399.0)
        insertStop(db, trip2Id, "Стулово", "15:12", "15:13", 410.4)
        insertStop(db, trip2Id, "ДЭП-4", "15:14", "15:15", 421.8)
        insertStop(db, trip2Id, "Улица Грина", "15:16", "15:17", 433.2)
        insertStop(db, trip2Id, "Рождественская улица", "15:18", "15:19", 444.6)
        insertStop(db, trip2Id, "Автостанция Слободского", "16:30", "16:30", 480.0)

        // Маршрут 3: Киров-Котельнич (ПОЛНЫЙ маршрут - 70+ остановок)
        insertStop(db, trip3Id, "Автовокзал Киров", "09:30", "09:30", 0.0)
        insertStop(db, trip3Id, "Улица Дзержинского", "09:35", "09:36", 8.5)
        insertStop(db, trip3Id, "Поворот на Гирсово", "09:40", "09:41", 17.1)
        insertStop(db, trip3Id, "Поворот на Мурыгино", "09:45", "09:46", 25.7)
        insertStop(db, trip3Id, "Горцы", "09:50", "09:51", 34.2)
        insertStop(db, trip3Id, "Сады Урожай-1", "09:55", "09:56", 42.8)
        insertStop(db, trip3Id, "Поворот на Юрью", "10:00", "10:01", 51.4)
        insertStop(db, trip3Id, "Поворот на Медяны", "10:05", "10:06", 60.0)
        insertStop(db, trip3Id, "Поворот на Малое Чураково", "10:10", "10:11", 68.5)
        insertStop(db, trip3Id, "Лаптевы", "10:15", "10:16", 77.1)
        insertStop(db, trip3Id, "Река Великая", "10:20", "10:21", 85.7)
        insertStop(db, trip3Id, "Поворот на Цепели", "10:25", "10:26", 94.2)
        insertStop(db, trip3Id, "Красногоры", "10:30", "10:31", 102.8)
        insertStop(db, trip3Id, "Верхняя Боярщина", "10:35", "10:36", 111.4)
        insertStop(db, trip3Id, "Зоновщина", "10:40", "10:41", 120.0)
        insertStop(db, trip3Id, "Юркичи", "10:45", "10:46", 128.5)
        insertStop(db, trip3Id, "Раменье", "10:50", "10:51", 137.1)
        insertStop(db, trip3Id, "Боярщина", "10:55", "10:56", 145.7)
        insertStop(db, trip3Id, "Колеватовы", "11:00", "11:01", 154.2)
        insertStop(db, trip3Id, "Кузнецы-Орлов", "11:05", "11:06", 162.8)
        insertStop(db, trip3Id, "Нижние Опарины", "11:10", "11:11", 171.4)
        insertStop(db, trip3Id, "Щенники", "11:15", "11:16", 180.0)
        insertStop(db, trip3Id, "Казаковцевы", "11:20", "11:21", 188.5)
        insertStop(db, trip3Id, "Весниничи", "11:25", "11:26", 197.1)
        insertStop(db, trip3Id, "Назаровы", "11:30", "11:31", 205.7)
        insertStop(db, trip3Id, "Поворот на Криничи", "11:35", "11:36", 214.2)
        insertStop(db, trip3Id, "Автостанция Орлов", "11:40", "11:41", 222.8)
        insertStop(db, trip3Id, "Магазин Золотая марка", "11:45", "11:46", 231.4)
        insertStop(db, trip3Id, "Детские ясли", "11:50", "11:51", 240.0)
        insertStop(db, trip3Id, "Магазин Петушок", "11:55", "11:56", 248.5)
        insertStop(db, trip3Id, "ТЦ Муравейник", "12:00", "12:01", 257.1)
        insertStop(db, trip3Id, "Больница", "12:05", "12:06", 265.7)
        insertStop(db, trip3Id, "Магазин Наш дом", "12:10", "12:11", 274.2)
        insertStop(db, trip3Id, "Мебельная фабрика", "12:15", "12:16", 282.8)
        insertStop(db, trip3Id, "Юбилейная улица", "12:20", "12:21", 291.4)
        insertStop(db, trip3Id, "Высоково", "12:25", "12:26", 300.0)
        insertStop(db, trip3Id, "Осинки", "12:30", "12:31", 308.5)
        insertStop(db, trip3Id, "Балванская", "12:35", "12:36", 317.1)
        insertStop(db, trip3Id, "Поворот на Юрьево", "12:40", "12:41", 325.7)
        insertStop(db, trip3Id, "Скурихинская", "12:45", "12:46", 334.2)
        insertStop(db, trip3Id, "Овчинниковы", "12:50", "12:51", 342.8)
        insertStop(db, trip3Id, "Минины", "12:55", "12:56", 351.4)
        insertStop(db, trip3Id, "Кардаковы", "13:00", "13:01", 360.0)
        insertStop(db, trip3Id, "Фадеевцы / Липичи / Жохи", "13:05", "13:06", 368.5)
        insertStop(db, trip3Id, "Хаустовы", "13:10", "13:11", 377.1)
        insertStop(db, trip3Id, "Гулины", "13:15", "13:16", 385.7)
        insertStop(db, trip3Id, "Поворот на Ленинскую Искру", "13:20", "13:21", 394.2)
        insertStop(db, trip3Id, "Климичи", "13:25", "13:26", 402.8)
        insertStop(db, trip3Id, "Пост ГИБДД", "13:30", "13:31", 411.4)
        insertStop(db, trip3Id, "Автостанция Котельнич", "13:35", "13:36", 420.0)
        insertStop(db, trip3Id, "Широченки", "13:40", "13:41", 428.5)
        insertStop(db, trip3Id, "Шестаковы", "13:45", "13:46", 437.1)
        insertStop(db, trip3Id, "Копылы", "13:50", "13:51", 445.7)
        insertStop(db, trip3Id, "Борки", "13:55", "13:56", 454.2)
        insertStop(db, trip3Id, "Ванюшенки", "14:00", "14:01", 462.8)
        insertStop(db, trip3Id, "Вишкиль", "14:05", "14:06", 471.4)
        insertStop(db, trip3Id, "Мамаи", "14:10", "14:11", 480.0)
        insertStop(db, trip3Id, "Смирновы", "14:15", "14:16", 488.5)
        insertStop(db, trip3Id, "Боровка", "14:20", "14:21", 497.1)
        insertStop(db, trip3Id, "Криуша", "14:25", "14:26", 505.7)
        insertStop(db, trip3Id, "Горбуновщина", "14:30", "14:31", 514.2)
        insertStop(db, trip3Id, "Сорвижи", "14:35", "14:36", 522.8)
        insertStop(db, trip3Id, "Горбуновщина (обратный)", "14:40", "14:41", 531.4)
        insertStop(db, trip3Id, "Криуша (обратный)", "14:45", "14:46", 540.0)
        insertStop(db, trip3Id, "Поворот на Кормино", "14:50", "14:51", 548.5)
        insertStop(db, trip3Id, "Поворот на Шабры", "14:55", "14:56", 557.1)
        insertStop(db, trip3Id, "Поворот на Шембеть", "15:00", "15:01", 565.7)
        insertStop(db, trip3Id, "Поворот на Арбаж", "15:05", "15:06", 574.2)
        insertStop(db, trip3Id, "Мосуны", "15:10", "15:11", 582.8)
        insertStop(db, trip3Id, "Чернушка", "15:15", "15:16", 591.4)
        insertStop(db, trip3Id, "Мостолыги", "15:20", "15:21", 600.0)
        insertStop(db, trip3Id, "Лобасты", "15:25", "15:26", 608.5)
        insertStop(db, trip3Id, "Автостанция Арбаж", "15:30", "15:30", 617.1)

        // Маршрут 4: Котельнич-Киров (обратный, в обратном порядке)
        insertStop(db, trip4Id, "Автостанция Арбаж", "16:00", "16:00", 0.0)
        insertStop(db, trip4Id, "Лобасты", "16:05", "16:06", 8.5)
        insertStop(db, trip4Id, "Мостолыги", "16:10", "16:11", 17.1)
        insertStop(db, trip4Id, "Чернушка", "16:15", "16:16", 25.7)
        insertStop(db, trip4Id, "Мосуны", "16:20", "16:21", 34.2)
        insertStop(db, trip4Id, "Поворот на Арбаж", "16:25", "16:26", 42.8)
        insertStop(db, trip4Id, "Поворот на Шембеть", "16:30", "16:31", 51.4)
        insertStop(db, trip4Id, "Поворот на Шабры", "16:35", "16:36", 60.0)
        insertStop(db, trip4Id, "Поворот на Кормино", "16:40", "16:41", 68.5)
        insertStop(db, trip4Id, "Криуша (обратный)", "16:45", "16:46", 77.1)
        insertStop(db, trip4Id, "Горбуновщина (обратный)", "16:50", "16:51", 85.7)
        insertStop(db, trip4Id, "Сорвижи", "16:55", "16:56", 94.2)
        insertStop(db, trip4Id, "Горбуновщина", "17:00", "17:01", 102.8)
        insertStop(db, trip4Id, "Криуша", "17:05", "17:06", 111.4)
        insertStop(db, trip4Id, "Боровка", "17:10", "17:11", 120.0)
        insertStop(db, trip4Id, "Смирновы", "17:15", "17:16", 128.5)
        insertStop(db, trip4Id, "Мамаи", "17:20", "17:21", 137.1)
        insertStop(db, trip4Id, "Вишкиль", "17:25", "17:26", 145.7)
        insertStop(db, trip4Id, "Ванюшенки", "17:30", "17:31", 154.2)
        insertStop(db, trip4Id, "Борки", "17:35", "17:36", 162.8)
        insertStop(db, trip4Id, "Копылы", "17:40", "17:41", 171.4)
        insertStop(db, trip4Id, "Шестаковы", "17:45", "17:46", 180.0)
        insertStop(db, trip4Id, "Широченки", "17:50", "17:51", 188.5)
        insertStop(db, trip4Id, "Автостанция Котельнич", "17:55", "17:56", 197.1)
        insertStop(db, trip4Id, "Пост ГИБДД", "18:00", "18:01", 205.7)
        insertStop(db, trip4Id, "Климичи", "18:05", "18:06", 214.2)
        insertStop(db, trip4Id, "Поворот на Ленинскую Искру", "18:10", "18:11", 222.8)
        insertStop(db, trip4Id, "Гулины", "18:15", "18:16", 231.4)
        insertStop(db, trip4Id, "Хаустовы", "18:20", "18:21", 240.0)
        insertStop(db, trip4Id, "Фадеевцы / Липичи / Жохи", "18:25", "18:26", 248.5)
        insertStop(db, trip4Id, "Кардаковы", "18:30", "18:31", 257.1)
        insertStop(db, trip4Id, "Минины", "18:35", "18:36", 265.7)
        insertStop(db, trip4Id, "Овчинниковы", "18:40", "18:41", 274.2)
        insertStop(db, trip4Id, "Скурихинская", "18:45", "18:46", 282.8)
        insertStop(db, trip4Id, "Поворот на Юрьево", "18:50", "18:51", 291.4)
        insertStop(db, trip4Id, "Балванская", "18:55", "18:56", 300.0)
        insertStop(db, trip4Id, "Осинки", "19:00", "19:01", 308.5)
        insertStop(db, trip4Id, "Высоково", "19:05", "19:06", 317.1)
        insertStop(db, trip4Id, "Юбилейная улица", "19:10", "19:11", 325.7)
        insertStop(db, trip4Id, "Мебельная фабрика", "19:15", "19:16", 334.2)
        insertStop(db, trip4Id, "Магазин Наш дом", "19:20", "19:21", 342.8)
        insertStop(db, trip4Id, "Больница", "19:25", "19:26", 351.4)
        insertStop(db, trip4Id, "ТЦ Муравейник", "19:30", "19:31", 360.0)
        insertStop(db, trip4Id, "Магазин Петушок", "19:35", "19:36", 368.5)
        insertStop(db, trip4Id, "Детские ясли", "19:40", "19:41", 377.1)
        insertStop(db, trip4Id, "Магазин Золотая марка", "19:45", "19:46", 385.7)
        insertStop(db, trip4Id, "Автостанция Орлов", "19:50", "19:51", 394.2)
        insertStop(db, trip4Id, "Поворот на Криничи", "19:55", "19:56", 402.8)
        insertStop(db, trip4Id, "Назаровы", "20:00", "20:01", 411.4)
        insertStop(db, trip4Id, "Весниничи", "20:05", "20:06", 420.0)
        insertStop(db, trip4Id, "Казаковцевы", "20:10", "20:11", 428.5)
        insertStop(db, trip4Id, "Щенники", "20:15", "20:16", 437.1)
        insertStop(db, trip4Id, "Нижние Опарины", "20:20", "20:21", 445.7)
        insertStop(db, trip4Id, "Кузнецы-Орлов", "20:25", "20:26", 454.2)
        insertStop(db, trip4Id, "Колеватовы", "20:30", "20:31", 462.8)
        insertStop(db, trip4Id, "Боярщина", "20:35", "20:36", 471.4)
        insertStop(db, trip4Id, "Раменье", "20:40", "20:41", 480.0)
        insertStop(db, trip4Id, "Юркичи", "20:45", "20:46", 488.5)
        insertStop(db, trip4Id, "Зоновщина", "20:50", "20:51", 497.1)
        insertStop(db, trip4Id, "Верхняя Боярщина", "20:55", "20:56", 505.7)
        insertStop(db, trip4Id, "Красногоры", "21:00", "21:01", 514.2)
        insertStop(db, trip4Id, "Поворот на Цепели", "21:05", "21:06", 522.8)
        insertStop(db, trip4Id, "Река Великая", "21:10", "21:11", 531.4)
        insertStop(db, trip4Id, "Лаптевы", "21:15", "21:16", 540.0)
        insertStop(db, trip4Id, "Поворот на Малое Чураково", "21:20", "21:21", 548.5)
        insertStop(db, trip4Id, "Поворот на Медяны", "21:25", "21:26", 557.1)
        insertStop(db, trip4Id, "Поворот на Юрью", "21:30", "21:31", 565.7)
        insertStop(db, trip4Id, "Сады Урожай-1", "21:35", "21:36", 574.2)
        insertStop(db, trip4Id, "Горцы", "21:40", "21:41", 582.8)
        insertStop(db, trip4Id, "Поворот на Мурыгино", "21:45", "21:46", 591.4)
        insertStop(db, trip4Id, "Поворот на Гирсово", "21:50", "21:51", 600.0)
        insertStop(db, trip4Id, "Улица Дзержинского", "21:55", "21:56", 608.5)
        insertStop(db, trip4Id, "Автовокзал Киров", "22:00", "22:00", 617.1)
    }

    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С ПОЛЬЗОВАТЕЛЯМИ ==========

    private fun insertUser(db: SQLiteDatabase, username: String, password: String, role: String, fullName: String, email: String, phone: String = ""): Long {
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
            put(KEY_ROLE, role)
            put(KEY_FULL_NAME, fullName)
            put(KEY_EMAIL, email)
            put(KEY_PHONE, phone)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUser(username: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_USER_ID, KEY_USERNAME, KEY_PASSWORD, KEY_ROLE, KEY_FULL_NAME, KEY_EMAIL, KEY_PHONE, KEY_CREATED_DATE),
            "$KEY_USERNAME = ? AND $KEY_PASSWORD = ?",
            arrayOf(username, password),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            try {
                val user = User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                    password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                    role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                    fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                    createdDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_DATE)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE))
                )
                Log.d("DB_DEBUG", "User found: ${user.username}")
                user
            } catch (e: Exception) {
                Log.e("DB_ERROR", "Error parsing user: ${e.message}", e)
                null
            }
        } else {
            Log.d("DB_DEBUG", "User not found: $username")
            null
        }.also { cursor.close() }
    }

    fun getUserById(userId: Int): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_USER_ID, KEY_USERNAME, KEY_PASSWORD, KEY_ROLE, KEY_FULL_NAME, KEY_EMAIL, KEY_PHONE, KEY_CREATED_DATE),
            "$KEY_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_DATE)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE))
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun addUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, user.username)
            put(KEY_PASSWORD, user.password)
            put(KEY_ROLE, user.role)
            put(KEY_FULL_NAME, user.fullName)
            put(KEY_EMAIL, user.email)
            put(KEY_PHONE, user.phone)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_USER_ID, KEY_USERNAME, KEY_PASSWORD, KEY_ROLE, KEY_FULL_NAME, KEY_EMAIL, KEY_PHONE, KEY_CREATED_DATE),
            null, null, null, null, "$KEY_ROLE ASC, $KEY_USERNAME ASC"
        )

        while (cursor.moveToNext()) {
            val user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_DATE)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE))
            )
            users.add(user)
        }
        cursor.close()
        return users
    }

    fun updateUser(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, user.username)
            put(KEY_PASSWORD, user.password)
            put(KEY_ROLE, user.role)
            put(KEY_FULL_NAME, user.fullName)
            put(KEY_EMAIL, user.email)
            put(KEY_PHONE, user.phone)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$KEY_USER_ID = ?",
            arrayOf(user.id.toString())
        )
        return rowsAffected > 0
    }

    fun deleteUser(userId: Int): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_USERS,
            "$KEY_USER_ID = ?",
            arrayOf(userId.toString())
        )
        return rowsAffected > 0
    }

    fun registerUser(username: String, password: String, fullName: String, email: String, phone: String = ""): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
            put(KEY_ROLE, "Пассажир")
            put(KEY_FULL_NAME, fullName)
            put(KEY_EMAIL, email)
            put(KEY_PHONE, phone)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun isUsernameExists(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_USER_ID),
            "$KEY_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С РЕЙСАМИ ==========

    private fun insertTrip(db: SQLiteDatabase, fromCity: String, toCity: String, departureTime: String, arrivalTime: String, price: Double): Long {
        val values = ContentValues().apply {
            put(KEY_FROM_CITY, fromCity)
            put(KEY_TO_CITY, toCity)
            put(KEY_DEPARTURE_TIME, departureTime)
            put(KEY_ARRIVAL_TIME, arrivalTime)
            put(KEY_PRICE, price)
        }
        return db.insert(TABLE_TRIPS, null, values)
    }

    fun getAllTrips(): List<Trip> {
        val trips = mutableListOf<Trip>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRIPS,
            arrayOf(KEY_TRIP_ID, KEY_FROM_CITY, KEY_TO_CITY, KEY_DEPARTURE_TIME, KEY_ARRIVAL_TIME, KEY_PRICE, KEY_STATUS),
            null, null, null, null, "$KEY_DEPARTURE_TIME ASC"
        )

        while (cursor.moveToNext()) {
            val trip = Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS))
            )
            trips.add(trip)
        }
        cursor.close()
        return trips
    }

    fun getTripById(tripId: Int): Trip? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRIPS,
            arrayOf(KEY_TRIP_ID, KEY_FROM_CITY, KEY_TO_CITY, KEY_DEPARTURE_TIME, KEY_ARRIVAL_TIME, KEY_PRICE, KEY_STATUS),
            "$KEY_TRIP_ID = ?",
            arrayOf(tripId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS))
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun addTrip(trip: Trip): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_FROM_CITY, trip.fromCity)
            put(KEY_TO_CITY, trip.toCity)
            put(KEY_DEPARTURE_TIME, trip.departureTime)
            put(KEY_ARRIVAL_TIME, trip.arrivalTime)
            put(KEY_PRICE, trip.price)
            put(KEY_STATUS, trip.status)
        }
        return db.insert(TABLE_TRIPS, null, values)
    }

    fun updateTrip(trip: Trip): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_FROM_CITY, trip.fromCity)
            put(KEY_TO_CITY, trip.toCity)
            put(KEY_DEPARTURE_TIME, trip.departureTime)
            put(KEY_ARRIVAL_TIME, trip.arrivalTime)
            put(KEY_PRICE, trip.price)
            put(KEY_STATUS, trip.status)
        }
        val rowsAffected = db.update(
            TABLE_TRIPS,
            values,
            "$KEY_TRIP_ID = ?",
            arrayOf(trip.id.toString())
        )
        return rowsAffected > 0
    }

    fun deleteTrip(tripId: Int): Boolean {
        val db = writableDatabase
        db.delete(TABLE_STOPS, "$KEY_TRIP_ID_FK = ?", arrayOf(tripId.toString()))
        val rowsAffected = db.delete(
            TABLE_TRIPS,
            "$KEY_TRIP_ID = ?",
            arrayOf(tripId.toString())
        )
        return rowsAffected > 0
    }

    fun searchTrips(fromCity: String, toCity: String, date: String): List<Trip> {
        val trips = mutableListOf<Trip>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRIPS,
            arrayOf(KEY_TRIP_ID, KEY_FROM_CITY, KEY_TO_CITY, KEY_DEPARTURE_TIME, KEY_ARRIVAL_TIME, KEY_PRICE, KEY_STATUS),
            "$KEY_FROM_CITY LIKE ? AND $KEY_TO_CITY LIKE ?",
            arrayOf("%$fromCity%", "%$toCity%"),
            null, null, "$KEY_DEPARTURE_TIME ASC"
        )

        while (cursor.moveToNext()) {
            val trip = Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS))
            )
            trips.add(trip)
        }
        cursor.close()
        return trips
    }

    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С ОСТАНОВКАМИ ==========

    private fun insertStop(db: SQLiteDatabase, tripId: Long, name: String, arrivalTime: String, departureTime: String, priceFromStart: Double) {
        val values = ContentValues().apply {
            put(KEY_TRIP_ID_FK, tripId)
            put(KEY_STOP_NAME, name)
            put(KEY_ARRIVAL_TIME_STOP, arrivalTime)
            put(KEY_DEPARTURE_TIME_STOP, departureTime)
            put(KEY_PRICE_FROM_START, priceFromStart)
        }
        db.insert(TABLE_STOPS, null, values)
    }

    fun getStopsByTripId(tripId: Int): List<Stop> {
        val stops = mutableListOf<Stop>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_STOPS,
            arrayOf(KEY_STOP_ID, KEY_STOP_NAME, KEY_ARRIVAL_TIME_STOP, KEY_DEPARTURE_TIME_STOP, KEY_PRICE_FROM_START),
            "$KEY_TRIP_ID_FK = ?",
            arrayOf(tripId.toString()),
            null, null, "$KEY_ARRIVAL_TIME_STOP ASC"
        )

        while (cursor.moveToNext()) {
            val stop = Stop(
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STOP_NAME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME_STOP)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME_STOP)),
                priceFromStart = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE_FROM_START))
            )
            stops.add(stop)
        }
        cursor.close()
        return stops
    }

    fun addStop(stop: Stop, tripId: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TRIP_ID_FK, tripId)
            put(KEY_STOP_NAME, stop.name)
            put(KEY_ARRIVAL_TIME_STOP, stop.arrivalTime)
            put(KEY_DEPARTURE_TIME_STOP, stop.departureTime)
            put(KEY_PRICE_FROM_START, stop.priceFromStart)
        }
        return db.insert(TABLE_STOPS, null, values)
    }

    fun deleteStopsByTripId(tripId: Int): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_STOPS,
            "$KEY_TRIP_ID_FK = ?",
            arrayOf(tripId.toString())
        )
        return rowsAffected > 0
    }

    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С БРОНИРОВАНИЯМИ ==========

    fun getBookedSeats(tripId: Int): List<Int> {
        val bookedSeats = mutableListOf<Int>()
        val db = readableDatabase
        val query = """
        SELECT $KEY_SEAT_NUMBER 
        FROM $TABLE_BOOKINGS 
        WHERE $KEY_TRIP_ID_BOOKING = ? AND $KEY_BOOKING_STATUS = 'Активен'
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(tripId.toString()))
        while (cursor.moveToNext()) {
            bookedSeats.add(cursor.getInt(0))
        }
        cursor.close()
        return bookedSeats
    }

    fun addBookingWithSeat(userId: Int, tripId: Int, passengerName: String, passengerEmail: String, seatNumber: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID_FK, userId)
            put(KEY_TRIP_ID_BOOKING, tripId)
            put(KEY_PASSENGER_NAME, passengerName)
            put(KEY_PASSENGER_EMAIL, passengerEmail)
            put(KEY_SEAT_NUMBER, seatNumber)
            put(KEY_BOOKING_STATUS, "Активен") // Добавляем статус по умолчанию
        }
        return db.insert(TABLE_BOOKINGS, null, values)
    }

    fun addBooking(userId: Int, tripId: Int, passengerName: String, passengerEmail: String): Long {
        val bookedSeats = getBookedSeats(tripId)
        var seatNumber = 1
        while (bookedSeats.contains(seatNumber) && seatNumber <= 45) {
            seatNumber++
        }
        return addBookingWithSeat(userId, tripId, passengerName, passengerEmail, seatNumber)
    }

    fun getBookingsByUserId(userId: Int): List<String> {
        val bookings = mutableListOf<String>()
        val db = readableDatabase
        val query = """
        SELECT t.$KEY_FROM_CITY, t.$KEY_TO_CITY, t.$KEY_DEPARTURE_TIME, b.$KEY_BOOKING_DATE, b.$KEY_BOOKING_STATUS, b.$KEY_SEAT_NUMBER
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
        WHERE b.$KEY_USER_ID_FK = ? 
        AND b.$KEY_BOOKING_STATUS = 'Активен'  
        ORDER BY b.$KEY_BOOKING_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            val booking = "${cursor.getString(0)} → ${cursor.getString(1)} | ${cursor.getString(2)} | Место ${cursor.getInt(5)} | ${cursor.getString(3)} | ${cursor.getString(4)}"
            bookings.add(booking)
        }
        cursor.close()
        return bookings
    }

    fun getBookingsByUserIdFull(userId: Int): List<Booking> {
        val bookings = mutableListOf<Booking>()
        val db = readableDatabase
        val query = """
        SELECT b.*, t.$KEY_FROM_CITY, t.$KEY_TO_CITY, t.$KEY_DEPARTURE_TIME, t.$KEY_ARRIVAL_TIME, t.$KEY_PRICE
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
        WHERE b.$KEY_USER_ID_FK = ? 
        AND b.$KEY_BOOKING_STATUS = 'Активен'  
        ORDER BY b.$KEY_BOOKING_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            val booking = Booking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BOOKING_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID_FK)),
                tripId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID_BOOKING)),
                passengerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME)),
                passengerEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL)),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_STATUS)),
                seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            )
            bookings.add(booking)
        }
        cursor.close()
        return bookings
    }

    fun getBookingCountByTripId(tripId: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_BOOKINGS WHERE $KEY_TRIP_ID_BOOKING = ? AND $KEY_BOOKING_STATUS = 'Активен'",
        arrayOf(tripId.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getBookingWithTripInfo(bookingId: Int): Pair<Booking, Trip>? {
        val db = readableDatabase
        val query = """
        SELECT b.*, t.* 
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
        WHERE b.$KEY_BOOKING_ID = ?
        AND b.$KEY_BOOKING_STATUS = 'Активен'  
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(bookingId.toString()))
        return if (cursor.moveToFirst()) {
            val booking = Booking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BOOKING_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID_FK)),
                tripId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID_BOOKING)),
                passengerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME)),
                passengerEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL)),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_STATUS)),
                seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            )
            val trip = Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE))
            )
            Pair(booking, trip)
        } else {
            null
        }.also { cursor.close() }
    }

    // ДОБАВЛЕН НОВЫЙ МЕТОД: Удалить бронирование
    fun deleteBooking(bookingId: Int): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_BOOKINGS,
            "$KEY_BOOKING_ID = ?",
            arrayOf(bookingId.toString())
        )
        return rowsAffected > 0
    }

    // СТАРЫЙ МЕТОД: Отменить бронирование (оставим, но не будем использовать)
    fun cancelBooking(bookingId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_BOOKING_STATUS, "Отменен")
        }
        val rowsAffected = db.update(
            TABLE_BOOKINGS,
            values,
            "$KEY_BOOKING_ID = ?",
            arrayOf(bookingId.toString())
        )
        return rowsAffected > 0
    }

    fun getTodayBookings(userId: Int): List<Booking> {
        val bookings = mutableListOf<Booking>()
        val db = readableDatabase
        val query = """
        SELECT b.*, t.$KEY_FROM_CITY, t.$KEY_TO_CITY, t.$KEY_DEPARTURE_TIME, t.$KEY_PRICE
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
        WHERE b.$KEY_USER_ID_FK = ? 
        AND date(b.$KEY_BOOKING_DATE) = date('now')
        AND b.$KEY_BOOKING_STATUS = 'Активен'  
        ORDER BY b.$KEY_BOOKING_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            val booking = Booking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BOOKING_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID_FK)),
                tripId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID_BOOKING)),
                passengerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME)),
                passengerEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL)),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_STATUS)),
                seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            )
            bookings.add(booking)
        }
        cursor.close()
        return bookings
    }

    fun getAllBookings(): List<Pair<Booking, Trip>> {
        val bookings = mutableListOf<Pair<Booking, Trip>>()
        val db = readableDatabase
        val query = """
        SELECT b.*, t.* 
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
        WHERE b.$KEY_BOOKING_STATUS = 'Активен'  
        ORDER BY b.$KEY_BOOKING_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val booking = Booking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BOOKING_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID_FK)),
                tripId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID_BOOKING)),
                passengerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME)),
                passengerEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL)),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_STATUS)),
                seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            )
            val trip = Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE))
            )
            bookings.add(Pair(booking, trip))
        }
        cursor.close()
        return bookings
    }

    fun addBookingForUser(userId: Int, tripId: Int, passengerName: String, passengerEmail: String, seatNumber: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID_FK, userId)
            put(KEY_TRIP_ID_BOOKING, tripId)
            put(KEY_PASSENGER_NAME, passengerName)
            put(KEY_PASSENGER_EMAIL, passengerEmail)
            put(KEY_SEAT_NUMBER, seatNumber)
            put(KEY_BOOKING_STATUS, "Активен") // Добавляем статус по умолчанию
        }
        return db.insert(TABLE_BOOKINGS, null, values)
    }

    // ========== МЕТОДЫ ДЛЯ ОТЧЕТОВ ==========

    fun getTodaySales(): String {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) as count, SUM(t.$KEY_PRICE) as total 
            FROM $TABLE_BOOKINGS b 
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID 
            WHERE date(b.$KEY_BOOKING_DATE) = date('now')
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        return if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            val total = cursor.getDouble(1)
            "Продано билетов: $count\nОбщая выручка: ${total.toInt()} руб."
        } else {
            "Нет продаж за сегодня"
        }.also { cursor.close() }
    }

    fun getWeekSales(): String {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) as count, SUM(t.$KEY_PRICE) as total 
            FROM $TABLE_BOOKINGS b 
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID 
            WHERE b.$KEY_BOOKING_DATE >= date('now', '-7 days')
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        return if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            val total = cursor.getDouble(1)
            val avg = if (count > 0) (total / count).toInt() else 0
            "Продано билетов: $count\nОбщая выручка: ${total.toInt()} руб.\nСредний чек: $avg руб."
        } else {
            "Нет продаж за неделю"
        }.also { cursor.close() }
    }

    fun getMonthSales(): String {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) as count, SUM(t.$KEY_PRICE) as total 
            FROM $TABLE_BOOKINGS b 
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID 
            WHERE b.$KEY_BOOKING_DATE >= date('now', '-30 days')
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        return if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            val total = cursor.getDouble(1)

            val popularRouteQuery = """
                SELECT t.$KEY_FROM_CITY, t.$KEY_TO_CITY, COUNT(*) as route_count
                FROM $TABLE_BOOKINGS b 
                JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID 
                WHERE b.$KEY_BOOKING_DATE >= date('now', '-30 days')
                GROUP BY t.$KEY_FROM_CITY, t.$KEY_TO_CITY 
                ORDER BY route_count DESC 
                LIMIT 1
            """.trimIndent()

            val routeCursor = db.rawQuery(popularRouteQuery, null)
            val popularRoute = if (routeCursor.moveToFirst()) {
                "${routeCursor.getString(0)} → ${routeCursor.getString(1)}"
            } else {
                "Нет данных"
            }.also { routeCursor.close() }

            "Продано билетов: $count\nОбщая выручка: ${total.toInt()} руб.\nПопулярный маршрут: $popularRoute"
        } else {
            "Нет продаж за месяц"
        }.also { cursor.close() }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    fun getTotalBookingsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_BOOKINGS", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getTotalRevenue(): Double {
        val db = readableDatabase
        val query = """
            SELECT SUM(t.$KEY_PRICE) as total 
            FROM $TABLE_BOOKINGS b 
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        val total = if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
        cursor.close()
        return total
    }

    fun getUsersCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_USERS", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getTripsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_TRIPS", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getActiveBookingsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_BOOKINGS WHERE $KEY_BOOKING_STATUS = 'Активен'", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    // ========== МЕТОДЫ ДЛЯ ЧЕКА ==========

    fun getBookingReceipt(bookingId: Int): String {
        val db = readableDatabase
        val query = """
            SELECT b.*, t.*, u.$KEY_FULL_NAME as user_name, u.$KEY_USERNAME as user_login, u.$KEY_EMAIL as user_email
            FROM $TABLE_BOOKINGS b
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_TRIP_ID
            LEFT JOIN $TABLE_USERS u ON b.$KEY_USER_ID_FK = u.$KEY_USER_ID
            WHERE b.$KEY_BOOKING_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(bookingId.toString()))
        return if (cursor.moveToFirst()) {
            val trip = "${cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY))} → ${cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY))}"
            val time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME))
            val passenger = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME))
            val email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL))
            val seat = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE))
            val bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE))
            val userName = cursor.getString(cursor.getColumnIndexOrThrow("user_name")) ?: "Не указан"
            val userLogin = cursor.getString(cursor.getColumnIndexOrThrow("user_login")) ?: "Не указан"
            val userEmail = cursor.getString(cursor.getColumnIndexOrThrow("user_email")) ?: "Не указан"

            """
                ╔═══════════════════════════════╗
                ║         АВТОБУСНЫЙ БИЛЕТ      ║
                ╠═══════════════════════════════╣
                ║ Маршрут: $trip
                ║ Время: $time
                ║ Пассажир: $passenger
                ║ Email: $email
                ║ Место: $seat
                ║ Стоимость: ${price.toInt()} руб.
                ║ Дата покупки: $bookingDate
                ║ Владелец: $userName
                ║ Логин: $userLogin
                ║ Email владельца: $userEmail
                ║ Номер билета: $bookingId
                ║ Статус: Активен
                ╚═══════════════════════════════╝
            """.trimIndent()
        } else {
            "Чек не найден"
        }.also { cursor.close() }
    }

    // ========== КООРДИНАТЫ ДЛЯ КАРТЫ ==========

    fun getStopCoordinates(stopName: String): Pair<Double, Double>? {
        // Полная база координат для всех остановок с реальными координатами
        val coordinates = mapOf(
            // ========== МАРШРУТ СЛОБОДСКОЙ → КИРОВ ==========
            "Автостанция Слободского" to Pair(58.721271, 50.181422),
            "Автостанция" to Pair(58.722341, 50.181914),
            "Рождественская улица" to Pair(58.724178, 50.180442),
            "Улица Грина" to Pair(58.723944, 50.167182),
            "ДЭП-4" to Pair(58.723479, 50.155136),
            "Стулово" to Pair(58.719358, 50.138668),
            "ПМК-14" to Pair(58.715826, 50.127703),
            "Ситники" to Pair(58.712931, 50.118128),
            "Первомайский поворот" to Pair(58.700771, 50.090956),
            "Подсобное хозяйство" to Pair(58.692292, 50.058324),
            "По требованию (Вахруши)" to Pair(58.686488, 50.038007),
            "Школа" to Pair(58.682776, 50.032523),
            "Вахруши" to Pair(58.678954, 50.024328),
            "Рубежница" to Pair(58.676495, 50.005969),
            "Логуновы" to Pair(58.677337, 49.975695),
            "Осинцы" to Pair(58.670765, 49.941844),
            "По требованию (Луза)" to Pair(58.665673, 49.929962),
            "Луза" to Pair(58.658791, 49.917796),
            "Сады Биохимик-2" to Pair(58.657271, 49.910616),
            "Зониха" to Pair(58.650529, 49.877381),
            "Пантелеевы" to Pair(58.646046, 49.860089),
            "Столбово" to Pair(58.639777, 49.846169),
            "Шихово" to Pair(58.635727, 49.834186),
            "По требованию" to Pair(58.632290, 49.821560),
            "Трушковы" to Pair(58.629728, 49.811047),
            "Бобинский поворот" to Pair(58.623457, 49.784353),
            "Новомакарьевское кладбище" to Pair(58.620520, 49.775688),
            "Порошинский поворот" to Pair(58.616520, 49.765303),
            "Слобода Макарье" to Pair(58.614844, 49.755896),
            "Троицкая церковь" to Pair(58.613077, 49.750204),
            "Проезжая улица" to Pair(58.613343, 49.743892),
            "Заповедная улица" to Pair(58.618765, 49.718881),
            "Улица Красный Химик" to Pair(58.618483, 49.710419),
            "Слобода Дымково" to Pair(58.617178, 49.698415),
            "Профсоюзная улица" to Pair(58.615356, 49.683786),
            "Улица МОПРа" to Pair(58.609771, 49.680656),
            "Храм Иоанна Предтечи" to Pair(58.606565, 49.680864),
            "Трифонов монастырь" to Pair(58.600451, 49.681552),
            "Филармония" to Pair(58.594588, 49.681941),
            "Областная больница" to Pair(58.593655, 49.660874),
            "ЦУМ" to Pair(58.593526, 49.655366),
            "Автовокзал Киров" to Pair(58.583651, 49.650495),

            // ========== МАРШРУТ КИРОВ → СЛОБОДСКОЙ (ОБРАТНЫЙ) ==========
            "Центр Слободского" to Pair(58.724178, 50.180442),
            "Выезд из Слободского" to Pair(58.723479, 50.155136),
            "Дороничи" to Pair(58.719358, 50.138668),
            "Вахруши (обратный)" to Pair(58.678954, 50.024328),
            "Рубежница (обратный)" to Pair(58.676495, 50.005969),
            "Логуновы (обратный)" to Pair(58.677337, 49.975695),
            "Осинцы (обратный)" to Pair(58.670765, 49.941844),
            "Луза (обратный)" to Pair(58.658791, 49.917796),
            "Сады Биохимик" to Pair(58.657271, 49.910616),
            "Зониха (обратный)" to Pair(58.650529, 49.877381),
            "Пантелеевы (обратный)" to Pair(58.646046, 49.860089),
            "Столбово (обратный)" to Pair(58.639777, 49.846169),
            "Шихово (обратный)" to Pair(58.635727, 49.834186),
            "Трушковы (обратный)" to Pair(58.629728, 49.811047),
            "Бобинский поворот (обратный)" to Pair(58.623457, 49.784353),
            "Юго-Западный район" to Pair(58.618765, 49.718881),
            "Центр Кирова" to Pair(58.606565, 49.680864),

            // Маршрут Киров-Котельнич
            "Автовокзал Киров" to Pair(58.583651, 49.650495),
            "Улица Дзержинского" to Pair(58.633361, 49.617675),
            "Поворот на Гирсово" to Pair(58.737164, 49.552364),
            "Поворот на Мурыгино" to Pair(58.747287, 49.531892),
            "Горцы" to Pair(58.759871, 49.512871),
            "Сады Урожай-1" to Pair(58.770222, 49.482504),
            "Поворот на Юрью" to Pair(58.772118, 49.473849),
            "Поворот на Медяны" to Pair(58.771935, 49.384934),
            "Поворот на Малое Чураково" to Pair(58.755541, 49.315456),
            "Лаптевы" to Pair(58.745547, 49.287798),
            "Река Великая" to Pair(58.737099, 49.250816),
            "Поворот на Цепели" to Pair(58.722459, 49.221294),
            "Красногоры" to Pair(58.715344, 49.210115),
            "Верхняя Боярщина" to Pair(58.707308, 49.192200),
            "Зоновщина" to Pair(58.702078, 49.169786),
            "Юркичи" to Pair(58.692492, 49.129097),
            "Раменье" to Pair(58.685231, 49.106490),
            "Боярщина" to Pair(58.675677, 49.069284),
            "Колеватовы" to Pair(58.670739, 49.056520),
            "Кузнецы-Орлов" to Pair(58.658588, 49.029802),
            "Нижние Опарины" to Pair(58.647280, 49.006306),
            "Щенники" to Pair(58.630891, 48.987304),
            "Казаковцевы" to Pair(58.612166, 48.975044),
            "Весниничи" to Pair(58.597590, 48.964840),
            "Назаровы" to Pair(58.574886, 48.939167),
            "Поворот на Криничи" to Pair(58.560212, 48.918384),
            "Автостанция Орлов" to Pair(58.548402, 48.898684),
            "Магазин Золотая марка" to Pair(58.542541, 48.903440),
            "Детские ясли" to Pair(58.540691, 48.901498),
            "Магазин Петушок" to Pair(58.536763, 48.895470),
            "ТЦ Муравейник" to Pair(58.534106, 48.891417),
            "Больница" to Pair(58.531279, 48.886428),
            "Магазин Наш дом" to Pair(58.533788, 48.880280),
            "Мебельная фабрика" to Pair(58.532313, 48.875484),
            "Юбилейная улица" to Pair(58.531889, 48.870343),
            "Высоково" to Pair(58.543928, 48.753756),
            "Осинки" to Pair(58.489731, 48.587237),
            "Балванская" to Pair(58.480585, 48.572046),
            "Поворот на Юрьево" to Pair(58.452485, 48.531782),
            "Скурихинская" to Pair(58.438628, 48.516946),
            "Овчинниковы" to Pair(58.419006, 48.483533),
            "Минины" to Pair(58.408897, 48.474876),
            "Кардаковы" to Pair(58.400170, 48.467495),
            "Фадеевцы / Липичи / Жохи" to Pair(58.376832, 48.453930),
            "Хаустовы" to Pair(58.359602, 48.437854),
            "Гулины" to Pair(58.348800, 48.428992),
            "Поворот на Ленинскую Искру" to Pair(58.334175, 48.417026),
            "Климичи" to Pair(58.324498, 48.403480),
            "Пост ГИБДД" to Pair(58.318261, 48.396782),
            "Автостанция Котельнич" to Pair(58.312207, 48.341900),
            "Широченки" to Pair(58.260866, 48.306513),
            "Шестаковы" to Pair(58.247822, 48.306982),
            "Копылы" to Pair(58.212727, 48.302973),
            "Ванюшенки" to Pair(58.151193, 48.330589),
            "Вишкиль" to Pair(58.092038, 48.318224),
            "Мамаи" to Pair(58.004120, 48.280065),
            "Смирновы" to Pair(57.985803, 48.296416),
            "Боровка" to Pair(57.948613, 48.328677),
            "Криуша" to Pair(57.908502, 48.412161),
            "Горбуновщина" to Pair(57.886925, 48.447575),
            "Сорвижи" to Pair(57.864274, 48.534764),
            "Поворот на Кормино" to Pair(57.887566, 48.355890),
            "Поворот на Шабры" to Pair(57.845882, 48.312336),
            "Поворот на Шембеть" to Pair(57.810725, 48.283248),
            "Поворот на Арбаж" to Pair(57.791532, 48.269725),
            "Мосуны" to Pair(57.763075, 48.274191),
            "Чернушка" to Pair(57.743750, 48.268683),
            "Мостолыги" to Pair(57.712196, 48.265604),
            "Лобасты" to Pair(57.690938, 48.290700),
            "Автостанция Арбаж" to Pair(57.680673, 48.307524),

            // Маршрут Котельнич-Киров (обратный порядок)
            "Сорвижи" to Pair(57.864274, 48.534764),
            "Горбуновщина" to Pair(57.886925, 48.447575),
            "Криуша" to Pair(57.908502, 48.412161),
            "Боровка" to Pair(57.948613, 48.328677),
            "Смирновы" to Pair(57.985803, 48.296416),
            "Мамаи" to Pair(58.004120, 48.280065),
            "Вишкиль" to Pair(58.092038, 48.318224),
            "Ванюшенки" to Pair(58.151193, 48.330589),
            "Копылы" to Pair(58.212727, 48.302973),
            "Шестаковы" to Pair(58.247822, 48.306982),
            "Широченки" to Pair(58.260866, 48.306513),
            "Автостанция Котельнич" to Pair(58.312207, 48.341900),
            "Пост ГИБДД" to Pair(58.318261, 48.396782),
            "Климичи" to Pair(58.324498, 48.403480),
            "Поворот на Ленинскую Искру" to Pair(58.334175, 48.417026),
            "Гулины" to Pair(58.348800, 48.428992),
            "Хаустовы" to Pair(58.359602, 48.437854),
            "Фадеевцы / Липичи / Жохи" to Pair(58.376832, 48.453930),
            "Кардаковы" to Pair(58.400170, 48.467495),
            "Минины" to Pair(58.408897, 48.474876),
            "Овчинниковы" to Pair(58.419006, 48.483533),
            "Скурихинская" to Pair(58.438628, 48.516946),
            "Поворот на Юрьево" to Pair(58.452485, 48.531782),
            "Балванская" to Pair(58.480585, 48.572046),
            "Осинки" to Pair(58.489731, 48.587237),
            "Высоково" to Pair(58.543928, 48.753756),
            "Юбилейная улица" to Pair(58.531889, 48.870343),
            "Мебельная фабрика" to Pair(58.532313, 48.875484),
            "Магазин Наш дом" to Pair(58.533788, 48.880280),
            "Больница" to Pair(58.531279, 48.886428),
            "ТЦ Муравейник" to Pair(58.534106, 48.891417),
            "Магазин Петушок" to Pair(58.536763, 48.895470),
            "Детские ясли" to Pair(58.540691, 48.901498),
            "Магазин Золотая марка" to Pair(58.542541, 48.903440),
            "Автостанция Орлов" to Pair(58.548402, 48.898684),
            "Поворот на Криничи" to Pair(58.560212, 48.918384),
            "Назаровы" to Pair(58.574886, 48.939167),
            "Весниничи" to Pair(58.597590, 48.964840),
            "Казаковцевы" to Pair(58.612166, 48.975044),
            "Щенники" to Pair(58.630891, 48.987304),
            "Нижние Опарины" to Pair(58.647280, 49.006306),
            "Кузнецы-Орлов" to Pair(58.658588, 49.029802),
            "Колеватовы" to Pair(58.670739, 49.056520),
            "Боярщина" to Pair(58.675677, 49.069284),
            "Раменье" to Pair(58.685231, 49.106490),
            "Юркичи" to Pair(58.692492, 49.129097),
            "Зоновщина" to Pair(58.702078, 49.169786),
            "Верхняя Боярщина" to Pair(58.707308, 49.192200),
            "Красногоры" to Pair(58.715344, 49.210115),
            "Поворот на Цепели" to Pair(58.722459, 49.221294),
            "Река Великая" to Pair(58.737099, 49.250816),
            "Лаптевы" to Pair(58.745547, 49.287798),
            "Поворот на Малое Чураково" to Pair(58.755541, 49.315456),
            "Поворот на Медяны" to Pair(58.771935, 49.384934),
            "Поворот на Юрью" to Pair(58.772118, 49.473849),
            "Сады Урожай-1" to Pair(58.770222, 49.482504),
            "Горцы" to Pair(58.759871, 49.512871),
            "Поворот на Мурыгино" to Pair(58.747287, 49.531892),
            "Поворот на Гирсово" to Pair(58.737164, 49.552364),
            "Улица Дзержинского" to Pair(58.633361, 49.617675),
            "Автовокзал Киров" to Pair(58.583651, 49.650495),

        )

        // Нормализация названий остановок
        val normalizedStopName = when {
            // Для маршрута Слободской → Киров
            stopName.contains("Автостанция Слободского", ignoreCase = true) -> "Автостанция Слободского"
            stopName.contains("Центр Слободского", ignoreCase = true) -> "Рождественская улица"
            stopName.contains("Выезд из Слободского", ignoreCase = true) -> "ДЭП-4"
            stopName.contains("Дороничи", ignoreCase = true) -> "Стулово"
            stopName.contains("ПМК", ignoreCase = true) -> "ПМК-14"
            stopName.contains("Ситники", ignoreCase = true) -> "Ситники"
            stopName.contains("Первомайский", ignoreCase = true) -> "Первомайский поворот"
            stopName.contains("Подсобное", ignoreCase = true) -> "Подсобное хозяйство"
            stopName.contains("Школа", ignoreCase = true) -> "Школа"
            stopName.contains("Вахруши") && !stopName.contains("обратный") && !stopName.contains("Слободского") -> "Вахруши"
            stopName.contains("Рубежница") && !stopName.contains("обратный") -> "Рубежница"
            stopName.contains("Логуновы") && !stopName.contains("обратный") -> "Логуновы"
            stopName.contains("Осинцы") && !stopName.contains("обратный") -> "Осинцы"
            stopName.contains("Луза") && !stopName.contains("обратный") -> "Луза"
            stopName.contains("Сады Биохимик") && !stopName.contains("2") -> "Сады Биохимик-2"
            stopName.contains("Зониха") && !stopName.contains("обратный") -> "Зониха"
            stopName.contains("Пантелеевы") && !stopName.contains("обратный") -> "Пантелеевы"
            stopName.contains("Столбово") && !stopName.contains("обратный") -> "Столбово"
            stopName.contains("Шихово") && !stopName.contains("обратный") -> "Шихово"
            stopName.contains("Трушковы") && !stopName.contains("обратный") -> "Трушковы"
            stopName.contains("Бобинский", ignoreCase = true) && !stopName.contains("обратный") -> "Бобинский поворот"
            stopName.contains("Новомакарьевское", ignoreCase = true) -> "Новомакарьевское кладбище"
            stopName.contains("Порошинский", ignoreCase = true) -> "Порошинский поворот"
            stopName.contains("Слобода Макарье", ignoreCase = true) -> "Слобода Макарье"
            stopName.contains("Троицкая", ignoreCase = true) -> "Троицкая церковь"
            stopName.contains("Проезжая", ignoreCase = true) -> "Проезжая улица"
            stopName.contains("Заповедная", ignoreCase = true) -> "Заповедная улица"
            stopName.contains("Красный Химик", ignoreCase = true) -> "Улица Красный Химик"
            stopName.contains("Дымково", ignoreCase = true) -> "Слобода Дымково"
            stopName.contains("Профсоюзная", ignoreCase = true) -> "Профсоюзная улица"
            stopName.contains("МОПРа", ignoreCase = true) -> "Улица МОПРа"
            stopName.contains("Иоанна Предтечи", ignoreCase = true) -> "Храм Иоанна Предтечи"
            stopName.contains("Трифонов", ignoreCase = true) -> "Трифонов монастырь"
            stopName.contains("Филармония", ignoreCase = true) -> "Филармония"
            stopName.contains("Областная больница", ignoreCase = true) -> "Областная больница"
            stopName.contains("ЦУМ", ignoreCase = true) -> "ЦУМ"
            stopName.contains("Автовокзал Киров", ignoreCase = true) -> "Автовокзал Киров"

            // ========== МАРШРУТЫ КИРОВ-КОТЕЛЬНИЧ И КОТЕЛЬНИЧ-КИРОВ ==========
            stopName.contains("Улица Дзержинского", ignoreCase = true) -> "Улица Дзержинского"
            stopName.contains("Поворот на Гирсово", ignoreCase = true) -> "Поворот на Гирсово"
            stopName.contains("Поворот на Мурыгино", ignoreCase = true) -> "Поворот на Мурыгино"
            stopName.contains("Горцы", ignoreCase = true) -> "Горцы"
            stopName.contains("Сады Урожай-1", ignoreCase = true) -> "Сады Урожай-1"
            stopName.contains("Поворот на Юрью", ignoreCase = true) -> "Поворот на Юрью"
            stopName.contains("Поворот на Медяны", ignoreCase = true) -> "Поворот на Медяны"
            stopName.contains("Поворот на Малое Чураково", ignoreCase = true) -> "Поворот на Малое Чураково"
            stopName.contains("Лаптевы", ignoreCase = true) -> "Лаптевы"
            stopName.contains("Река Великая", ignoreCase = true) -> "Река Великая"
            stopName.contains("Поворот на Цепели", ignoreCase = true) -> "Поворот на Цепели"
            stopName.contains("Красногоры", ignoreCase = true) -> "Красногоры"
            stopName.contains("Верхняя Боярщина", ignoreCase = true) -> "Верхняя Боярщина"
            stopName.contains("Зоновщина", ignoreCase = true) -> "Зоновщина"
            stopName.contains("Юркичи", ignoreCase = true) -> "Юркичи"
            stopName.contains("Раменье", ignoreCase = true) -> "Раменье"
            stopName.contains("Боярщина", ignoreCase = true) && !stopName.contains("Верхняя") -> "Боярщина"
            stopName.contains("Колеватовы", ignoreCase = true) -> "Колеватовы"
            stopName.contains("Кузнецы-Орлов", ignoreCase = true) -> "Кузнецы-Орлов"
            stopName.contains("Нижние Опарины", ignoreCase = true) -> "Нижние Опарины"
            stopName.contains("Щенники", ignoreCase = true) -> "Щенники"
            stopName.contains("Казаковцевы", ignoreCase = true) -> "Казаковцевы"
            stopName.contains("Весниничи", ignoreCase = true) -> "Весниничи"
            stopName.contains("Назаровы", ignoreCase = true) -> "Назаровы"
            stopName.contains("Поворот на Криничи", ignoreCase = true) -> "Поворот на Криничи"
            stopName.contains("Автостанция Орлов", ignoreCase = true) -> "Автостанция Орлов"
            stopName.contains("Магазин Золотая марка", ignoreCase = true) -> "Магазин Золотая марка"
            stopName.contains("Детские ясли", ignoreCase = true) -> "Детские ясли"
            stopName.contains("Магазин Петушок", ignoreCase = true) -> "Магазин Петушок"
            stopName.contains("ТЦ Муравейник", ignoreCase = true) -> "ТЦ Муравейник"
            stopName.contains("Больница", ignoreCase = true) -> "Больница"
            stopName.contains("Магазин Наш дом", ignoreCase = true) -> "Магазин Наш дом"
            stopName.contains("Мебельная фабрика", ignoreCase = true) -> "Мебельная фабрика"
            stopName.contains("Юбилейная улица", ignoreCase = true) -> "Юбилейная улица"
            stopName.contains("Высоково", ignoreCase = true) -> "Высоково"
            stopName.contains("Осинки", ignoreCase = true) -> "Осинки"
            stopName.contains("Балванская", ignoreCase = true) -> "Балванская"
            stopName.contains("Поворот на Юрьево", ignoreCase = true) -> "Поворот на Юрьево"
            stopName.contains("Скурихинская", ignoreCase = true) -> "Скурихинская"
            stopName.contains("Овчинниковы", ignoreCase = true) -> "Овчинниковы"
            stopName.contains("Минины", ignoreCase = true) -> "Минины"
            stopName.contains("Кардаковы", ignoreCase = true) -> "Кардаковы"
            stopName.contains("Фадеевцы", ignoreCase = true) || stopName.contains("Липичи", ignoreCase = true) || stopName.contains("Жохи", ignoreCase = true) -> "Фадеевцы / Липичи / Жохи"
            stopName.contains("Хаустовы", ignoreCase = true) -> "Хаустовы"
            stopName.contains("Гулины", ignoreCase = true) -> "Гулины"
            stopName.contains("Поворот на Ленинскую Искру", ignoreCase = true) -> "Поворот на Ленинскую Искру"
            stopName.contains("Климичи", ignoreCase = true) -> "Климичи"
            stopName.contains("Пост ГИБДД", ignoreCase = true) -> "Пост ГИБДД"
            stopName.contains("Автостанция Котельнич", ignoreCase = true) -> "Автостанция Котельнич"
            stopName.contains("Широченки", ignoreCase = true) -> "Широченки"
            stopName.contains("Шестаковы", ignoreCase = true) -> "Шестаковы"
            stopName.contains("Копылы", ignoreCase = true) -> "Копылы"
            stopName.contains("Ванюшенки", ignoreCase = true) -> "Ванюшенки"
            stopName.contains("Вишкиль", ignoreCase = true) -> "Вишкиль"
            stopName.contains("Мамаи", ignoreCase = true) -> "Мамаи"
            stopName.contains("Смирновы", ignoreCase = true) -> "Смирновы"
            stopName.contains("Боровка", ignoreCase = true) -> "Боровка"
            stopName.contains("Криуша", ignoreCase = true) -> "Криуша"
            stopName.contains("Горбуновщина", ignoreCase = true) -> "Горбуновщина"
            stopName.contains("Сорвижи", ignoreCase = true) -> "Сорвижи"
            stopName.contains("Поворот на Кормино", ignoreCase = true) -> "Поворот на Кормино"
            stopName.contains("Поворот на Шабры", ignoreCase = true) -> "Поворот на Шабры"
            stopName.contains("Поворот на Шембеть", ignoreCase = true) -> "Поворот на Шембеть"
            stopName.contains("Поворот на Арбаж", ignoreCase = true) -> "Поворот на Арбаж"
            stopName.contains("Мосуны", ignoreCase = true) -> "Мосуны"
            stopName.contains("Чернушка", ignoreCase = true) -> "Чернушка"
            stopName.contains("Мостолыги", ignoreCase = true) -> "Мостолыги"
            stopName.contains("Лобасты", ignoreCase = true) -> "Лобасты"
            stopName.contains("Автостанция Арбаж", ignoreCase = true) -> "Автостанция Арбаж"
            // Для остановок "Горбуновщина (обратный)" и "Криуша (обратный)" используем основные координаты
            stopName.contains("Горбуновщина (обратный)", ignoreCase = true) -> "Горбуновщина"
            stopName.contains("Криуша (обратный)", ignoreCase = true) -> "Криуша"

            // Общие случаи
            stopName.contains("По требованию", ignoreCase = true) -> "По требованию"
            else -> stopName
        }

        // Сначала ищем по нормализованному имени
        return coordinates[normalizedStopName] ?: coordinates[stopName]
    }

    // ========== ОТЛАДОЧНЫЕ МЕТОДЫ ==========

    fun debugGetUserBookings(userId: Int): String {
        val bookings = getBookingsByUserIdFull(userId)
        return "Пользователь ID=$userId имеет ${bookings.size} бронирований:\n" +
                bookings.joinToString("\n") {
                    "Билет №${it.id}: ${it.passengerName} - ${it.bookingDate} - Место ${it.seatNumber}"
                }
    }

    fun debugGetAllBookings(): String {
        val allBookings = getAllBookings()
        return "Всего бронирований в системе: ${allBookings.size}\n" +
                allBookings.joinToString("\n") { (booking, trip) ->
                    "Билет №${booking.id}: Пользователь=${booking.userId}, Пассажир=${booking.passengerName}, Email=${booking.passengerEmail}, Рейс=${trip.fromCity}→${trip.toCity}, Место=${booking.seatNumber}"
                }
    }

}
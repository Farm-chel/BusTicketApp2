package com.example.busticketapp2.Data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.busticketapp2.models.*
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BusTicketDB"
        private const val DATABASE_VERSION = 15
        const val KEY_TRIP_DATE = "trip_date"
        // Таблицы
        const val TABLE_USERS = "users"
        const val TABLE_TRIPS = "trips"
        const val TABLE_STOPS = "stops"
        const val TABLE_BOOKINGS = "bookings"
        const val TABLE_VERIFICATION_CODES = "verification_codes"

        // Общие ключи
        const val KEY_ID = "id"

        // Пользователи
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_ROLE = "role"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
        const val KEY_CREATED_DATE = "created_date"
        const val KEY_IS_ACTIVE = "is_active"

        // Рейсы
        const val KEY_FROM_CITY = "from_city"
        const val KEY_TO_CITY = "to_city"
        const val KEY_DEPARTURE_TIME = "departure_time"
        const val KEY_ARRIVAL_TIME = "arrival_time"
        const val KEY_PRICE = "price"
        const val KEY_STATUS = "status"

        // Остановки
        const val KEY_TRIP_ID_FK = "trip_id"
        const val KEY_STOP_NAME = "stop_name"
        const val KEY_ARRIVAL_TIME_STOP = "arrival_time"
        const val KEY_DEPARTURE_TIME_STOP = "departure_time"
        const val KEY_PRICE_FROM_START = "price_from_start"

        // Бронирования
        const val KEY_USER_ID_FK = "user_id"
        const val KEY_TRIP_ID_BOOKING = "trip_id"
        const val KEY_PASSENGER_NAME = "passenger_name"
        const val KEY_PASSENGER_EMAIL = "passenger_email"
        const val KEY_BOOKING_DATE = "booking_date"
        const val KEY_BOOKING_STATUS = "status"
        const val KEY_SEAT_NUMBER = "seat_number"


        // Коды подтверждения
        const val KEY_CODE_USER_ID = "user_id"
        const val KEY_CODE_EMAIL = "email"
        const val KEY_CODE = "code"
        const val KEY_CREATED_AT = "created_at"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_IS_USED = "is_used"
    }

    // Ленивая инициализация кэша координат
    private val coordinatesCache by lazy {
        mutableMapOf<String, Pair<Double, Double>>().apply {
            initializeAllCoordinates()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DB_DEBUG", "========== onCreate() ==========")

        try {
            // 1. Таблица пользователей
            val CREATE_USERS_TABLE = """
                CREATE TABLE $TABLE_USERS (
                    $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_USERNAME TEXT UNIQUE NOT NULL,
                    $KEY_PASSWORD TEXT NOT NULL,
                    $KEY_ROLE TEXT NOT NULL DEFAULT 'Пассажир',
                    $KEY_FULL_NAME TEXT NOT NULL,
                    $KEY_EMAIL TEXT UNIQUE NOT NULL,
                    $KEY_PHONE TEXT,
                    $KEY_CREATED_DATE TEXT DEFAULT CURRENT_TIMESTAMP,
                    $KEY_IS_ACTIVE INTEGER DEFAULT 1
                )
            """.trimIndent()

            // 2. Таблица рейсов
            val CREATE_TRIPS_TABLE = """
                CREATE TABLE $TABLE_TRIPS (
                    $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_FROM_CITY TEXT NOT NULL,
                    $KEY_TO_CITY TEXT NOT NULL,
                    $KEY_DEPARTURE_TIME TEXT NOT NULL,
                    $KEY_ARRIVAL_TIME TEXT NOT NULL,
                    $KEY_PRICE REAL NOT NULL,
                    $KEY_STATUS TEXT DEFAULT 'Активен'
                )
            """.trimIndent()

            // 3. Таблица остановок
            val CREATE_STOPS_TABLE = """
                CREATE TABLE $TABLE_STOPS (
                    $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_TRIP_ID_FK INTEGER NOT NULL,
                    $KEY_STOP_NAME TEXT NOT NULL,
                    $KEY_ARRIVAL_TIME_STOP TEXT NOT NULL,
                    $KEY_DEPARTURE_TIME_STOP TEXT NOT NULL,
                    $KEY_PRICE_FROM_START REAL NOT NULL,
                    FOREIGN KEY ($KEY_TRIP_ID_FK) REFERENCES $TABLE_TRIPS($KEY_ID) ON DELETE CASCADE
                )
            """.trimIndent()

            // 4. Таблица бронирований
            val CREATE_BOOKINGS_TABLE = """
    CREATE TABLE $TABLE_BOOKINGS (
        $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $KEY_USER_ID_FK INTEGER NOT NULL,
        $KEY_TRIP_ID_BOOKING INTEGER NOT NULL,
        $KEY_PASSENGER_NAME TEXT NOT NULL,
        $KEY_PASSENGER_EMAIL TEXT NOT NULL,
        $KEY_BOOKING_DATE TEXT DEFAULT CURRENT_TIMESTAMP,
        $KEY_TRIP_DATE TEXT DEFAULT '${Trip.getCurrentDate()}', 
        $KEY_BOOKING_STATUS TEXT DEFAULT 'Активен',
        $KEY_SEAT_NUMBER INTEGER DEFAULT 0,
        FOREIGN KEY ($KEY_USER_ID_FK) REFERENCES $TABLE_USERS($KEY_ID),
        FOREIGN KEY ($KEY_TRIP_ID_BOOKING) REFERENCES $TABLE_TRIPS($KEY_ID)
    )
""".trimIndent()

            // 5. Таблица кодов подтверждения
            val CREATE_VERIFICATION_CODES_TABLE = """
                CREATE TABLE $TABLE_VERIFICATION_CODES (
                    $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_CODE_USER_ID INTEGER NOT NULL,
                    $KEY_CODE_EMAIL TEXT NOT NULL,
                    $KEY_CODE TEXT NOT NULL,
                    $KEY_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                    $KEY_EXPIRES_AT DATETIME NOT NULL,
                    $KEY_IS_USED INTEGER DEFAULT 0,
                    FOREIGN KEY ($KEY_CODE_USER_ID) REFERENCES $TABLE_USERS($KEY_ID) ON DELETE CASCADE
                )
            """.trimIndent()

            // Выполняем создание таблиц
            db.execSQL(CREATE_USERS_TABLE)
            db.execSQL(CREATE_TRIPS_TABLE)
            db.execSQL(CREATE_STOPS_TABLE)
            db.execSQL(CREATE_BOOKINGS_TABLE)
            db.execSQL(CREATE_VERIFICATION_CODES_TABLE)

            Log.d("DB_DEBUG", "Все таблицы созданы успешно")

            // Вставляем начальные данные
            insertInitialData(db)

        } catch (e: Exception) {
            Log.e("DB_ERROR", "Ошибка создания базы: ${e.message}", e)
            throw e
        }

        Log.d("DB_DEBUG", "========== onCreate() завершен ==========")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DB_DEBUG", "Обновление базы с $oldVersion до $newVersion")

        if (oldVersion < 14) {
            // Добавляем новое поле в таблицу бронирований
            db.execSQL("ALTER TABLE $TABLE_BOOKINGS ADD COLUMN $KEY_TRIP_DATE TEXT DEFAULT '${Trip.getCurrentDate()}'")
            Log.d("DB_DEBUG", "Добавлено поле trip_date в таблицу бронирований")
        }

        // Удаляем все таблицы
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VERIFICATION_CODES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STOPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRIPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")

        // Создаем заново
        onCreate(db)
    }

    // ========== МЕТОДЫ ДЛЯ ВСТАВКИ ДАННЫХ ==========
    private fun insertInitialData(db: SQLiteDatabase) {
        Log.d("DB_DEBUG", "Inserting initial data...")

        try {
            // Пользователи
            val adminId = insertUser(db, "admin", "admin", "Администратор", "Администратор Системы", "admin@example.com", "+79123456789")
            val cashierId = insertUser(db, "cashier", "cashier", "Кассир", "Иванова Анна Петровна", "cashier@example.com", "+79123456790")
            val userId = insertUser(db, "user", "user", "Пассажир", "Петров Петр Петрович", "user@example.com", "+79123456791")

            Log.d("DB_DEBUG", "Users inserted: adminId=$adminId, cashierId=$cashierId, userId=$userId")

            // Рейсы (только 6 маршрутов, без Киров-Вятские Поляны)
            val trip1Id = insertTrip(db, "Слободской", "Киров", "08:00", "09:00", 240.0)
            val trip2Id = insertTrip(db, "Киров", "Слободской", "14:00", "15:00", 240.0)
            val trip3Id = insertTrip(db, "Киров", "Котельнич", "09:30", "11:55", 600.0)
            val trip4Id = insertTrip(db, "Котельнич", "Киров", "16:00", "18:25", 600.0)
            val trip5Id = insertTrip(db, "Киров", "Советск", "08:30", "10:40", 450.0)
            val trip6Id = insertTrip(db, "Советск", "Киров", "11:40", "13:50", 450.0)

            Log.d("DB_DEBUG", "Trips inserted: trip1=$trip1Id, trip2=$trip2Id, trip3=$trip3Id, trip4=$trip4Id, trip5=$trip5Id, trip6=$trip6Id")

            // Вставляем ВСЕ остановки для каждого маршрута
            insertAllStopsForTrip1(db, trip1Id)
            insertAllStopsForTrip2(db, trip2Id)
            insertAllStopsForTrip3(db, trip3Id)
            insertAllStopsForTrip4(db, trip4Id)
            insertAllStopsForTrip5(db, trip5Id)
            insertAllStopsForTrip6(db, trip6Id)

            // Добавляем тестовые бронирования
            val booking1 = addBookingForUser(userId.toInt(), trip1Id.toInt(), "Иванов Иван Иванович", "ivanov@example.com", 1)
            val booking2 = addBookingForUser(userId.toInt(), trip2Id.toInt(), "Петров Петр Петрович", "petrov@example.com", 2)
            val booking3 = addBookingForUser(userId.toInt(), trip3Id.toInt(), "Сидоров Сергей Сергеевич", "sidorov@example.com", 3)

            Log.d("DB_DEBUG", "Test bookings inserted: booking1=$booking1, booking2=$booking2, booking3=$booking3")
            Log.d("DB_DEBUG", "Initial data insertion completed successfully")

        } catch (e: Exception) {
            Log.e("DB_ERROR", "Ошибка вставки начальных данных: ${e.message}", e)
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    private fun insertUser(db: SQLiteDatabase, username: String, password: String,
                           role: String, fullName: String, email: String, phone: String): Long {
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
            put(KEY_ROLE, role)
            put(KEY_FULL_NAME, fullName)
            put(KEY_EMAIL, email)
            put(KEY_PHONE, phone)
            put(KEY_IS_ACTIVE, 1)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    private fun insertTrip(db: SQLiteDatabase, fromCity: String, toCity: String,
                           departureTime: String, arrivalTime: String, price: Double): Long {
        val values = ContentValues().apply {
            put(KEY_FROM_CITY, fromCity)
            put(KEY_TO_CITY, toCity)
            put(KEY_DEPARTURE_TIME, departureTime)
            put(KEY_ARRIVAL_TIME, arrivalTime)
            put(KEY_PRICE, price)
        }
        return db.insert(TABLE_TRIPS, null, values)
    }

    private fun insertStop(db: SQLiteDatabase, tripId: Long, stopName: String,
                           arrivalTime: String, departureTime: String, priceFromStart: Double): Long {
        val values = ContentValues().apply {
            put(KEY_TRIP_ID_FK, tripId)
            put(KEY_STOP_NAME, stopName)
            put(KEY_ARRIVAL_TIME_STOP, arrivalTime)
            put(KEY_DEPARTURE_TIME_STOP, departureTime)
            put(KEY_PRICE_FROM_START, priceFromStart)
        }
        return db.insert(TABLE_STOPS, null, values)
    }

    // ========== ВСТАВКА ВСЕХ ОСТАНОВОК ДЛЯ КАЖДОГО МАРШРУТА ==========

    // Создаем data class для хранения 4 значений
    private data class StopData(
        val name: String,
        val arrivalTime: String,
        val departureTime: String,
        val price: Double
    )

    private fun insertAllStopsForTrip1(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Автостанция Слободского", "08:00", "08:00", 0.0),
            StopData("Рождественская улица", "08:02", "08:03", 5.7),
            StopData("Улица Грина", "08:04", "08:05", 11.4),
            StopData("ДЭП-4", "08:06", "08:07", 17.1),
            StopData("Стулово", "08:08", "08:09", 22.8),
            StopData("ПМК-14", "08:10", "08:11", 28.5),
            StopData("Ситники", "08:12", "08:13", 34.2),
            StopData("Первомайский поворот", "08:14", "08:15", 39.9),
            StopData("Подсобное хозяйство", "08:16", "08:17", 45.6),
            StopData("Вахруши", "08:18", "08:19", 51.3),
            StopData("Школа (Вахруши)", "08:20", "08:21", 57.0),
            StopData("Рубежница", "08:22", "08:23", 62.7),
            StopData("Логуновы", "08:24", "08:25", 68.4),
            StopData("Осинцы", "08:28", "08:29", 79.8),
            StopData("Луза", "08:30", "08:31", 85.5),
            StopData("Сады Биохимик-2", "08:34", "08:35", 96.9),
            StopData("Зониха", "08:36", "08:37", 102.6),
            StopData("Пантелеевы", "08:38", "08:39", 108.3),
            StopData("Столбово", "08:40", "08:41", 114.0),
            StopData("Шихово", "08:42", "08:43", 119.7),
            StopData("Поворот на Бобино", "08:44", "08:45", 125.4),
            StopData("Трушковы", "08:46", "08:47", 131.1),
            StopData("Новомакарьевское кладбище", "08:50", "08:51", 142.5),
            StopData("Порошинский поворот", "08:52", "08:53", 148.2),
            StopData("Слобода Макарье", "08:54", "08:55", 153.9),
            StopData("Троицкая церковь", "08:56", "08:57", 159.6),
            StopData("Проезжая улица", "08:58", "08:59", 165.3),
            StopData("Заповедная улица", "09:00", "09:01", 171.0),
            StopData("Улица Красный Химик", "09:02", "09:03", 176.7),
            StopData("Слобода Дымково", "09:04", "09:05", 182.4),
            StopData("Профсоюзная улица", "09:06", "09:07", 188.1),
            StopData("Улица МОПРа", "09:08", "09:09", 193.8),
            StopData("Храм Иоанна Предтечи", "09:10", "09:11", 199.5),
            StopData("Трифонов монастырь", "09:12", "09:13", 205.2),
            StopData("Филармония", "09:14", "09:15", 210.9),
            StopData("Областная больница", "09:16", "09:17", 216.6),
            StopData("ЦУМ", "09:18", "09:19", 222.3),
            StopData("Автовокзал Киров", "10:30", "10:30", 240.0)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 1 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip2(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Автовокзал Киров", "14:00", "14:00", 0.0),
            StopData("ЦУМ", "14:02", "14:03", 11.4),
            StopData("Областная больница", "14:04", "14:05", 22.8),
            StopData("Филармония", "14:06", "14:07", 34.2),
            StopData("Трифонов монастырь", "14:08", "14:09", 45.6),
            StopData("Храм Иоанна Предтечи", "14:10", "14:11", 57.0),
            StopData("Улица МОПРа", "14:12", "14:13", 68.4),
            StopData("Профсоюзная улица", "14:14", "14:15", 79.8),
            StopData("Слобода Дымково", "14:16", "14:17", 91.2),
            StopData("Улица Красный Химик", "14:18", "14:19", 102.6),
            StopData("Заповедная улица", "14:20", "14:21", 114.0),
            StopData("Проезжая улица", "14:22", "14:23", 125.4),
            StopData("Троицкая церковь", "14:24", "14:25", 136.8),
            StopData("Слобода Макарье", "14:26", "14:27", 148.2),
            StopData("Порошинский поворот", "14:28", "14:29", 159.6),
            StopData("Новомакарьевское кладбище", "14:30", "14:31", 171.0),
            StopData("Трушковы", "14:34", "14:35", 193.8),
            StopData("Поворот на Бобино", "14:36", "14:37", 205.2),
            StopData("Шихово", "14:38", "14:39", 216.6),
            StopData("Столбово", "14:40", "14:41", 228.0),
            StopData("Пантелеевы", "14:42", "14:43", 239.4),
            StopData("Зониха", "14:44", "14:45", 250.8),
            StopData("Сады Биохимик-2", "14:46", "14:47", 262.2),
            StopData("Луза", "14:48", "14:49", 273.6),
            StopData("Осинцы", "14:52", "14:53", 296.4),
            StopData("Логуновы", "14:54", "14:55", 307.8),
            StopData("Рубежница", "14:56", "14:57", 319.2),
            StopData("Школа (Вахруши)", "15:00", "15:01", 342.0),
            StopData("Вахруши", "15:02", "15:03", 353.4),
            StopData("Подсобное хозяйство", "15:04", "15:05", 364.8),
            StopData("Первомайский поворот", "15:06", "15:07", 376.2),
            StopData("Ситники", "15:08", "15:09", 387.6),
            StopData("ПМК-14", "15:10", "15:11", 399.0),
            StopData("Стулово", "15:12", "15:13", 410.4),
            StopData("ДЭП-4", "15:14", "15:15", 421.8),
            StopData("Улица Грина", "15:16", "15:17", 433.2),
            StopData("Рождественская улица", "15:18", "15:19", 444.6),
            StopData("Автостанция Слободского", "16:30", "16:30", 480.0)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 2 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip3(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Автовокзал Киров", "09:30", "09:30", 0.0),
            StopData("Улица Дзержинского", "09:35", "09:36", 8.5),
            StopData("Поворот на Гирсово", "09:40", "09:41", 17.1),
            StopData("Поворот на Мурыгино", "09:45", "09:46", 25.7),
            StopData("Горцы", "09:50", "09:51", 34.2),
            StopData("Сады Урожай-1", "09:55", "09:56", 42.8),
            StopData("Поворот на Юрью", "10:00", "10:01", 51.4),
            StopData("Поворот на Медяны", "10:05", "10:06", 60.0),
            StopData("Поворот на Малое Чураково", "10:10", "10:11", 68.5),
            StopData("Лаптевы", "10:15", "10:16", 77.1),
            StopData("Река Великая", "10:20", "10:21", 85.7),
            StopData("Поворот на Цепели", "10:25", "10:26", 94.2),
            StopData("Красногоры", "10:30", "10:31", 102.8),
            StopData("Верхняя Боярщина", "10:35", "10:36", 111.4),
            StopData("Зоновщина", "10:40", "10:41", 120.0),
            StopData("Юркичи", "10:45", "10:46", 128.5),
            StopData("Раменье", "10:50", "10:51", 137.1),
            StopData("Боярщина", "10:55", "10:56", 145.7),
            StopData("Колеватовы", "11:00", "11:01", 154.2),
            StopData("Кузнецы-Орлов", "11:05", "11:06", 162.8),
            StopData("Нижние Опарины", "11:10", "11:11", 171.4),
            StopData("Щенники", "11:15", "11:16", 180.0),
            StopData("Казаковцевы", "11:20", "11:21", 188.5),
            StopData("Весниничи", "11:25", "11:26", 197.1),
            StopData("Назаровы", "11:30", "11:31", 205.7),
            StopData("Поворот на Криничи", "11:35", "11:36", 214.2),
            StopData("Автостанция Орлов", "11:40", "11:41", 222.8),
            StopData("Магазин Золотая марка", "11:45", "11:46", 231.4),
            StopData("Детские ясли", "11:50", "11:51", 240.0),
            StopData("Магазин Петушок", "11:55", "11:56", 248.5),
            StopData("ТЦ Муравейник", "12:00", "12:01", 257.1),
            StopData("Больница", "12:05", "12:06", 265.7),
            StopData("Магазин Наш дом", "12:10", "12:11", 274.2),
            StopData("Мебельная фабрика", "12:15", "12:16", 282.8),
            StopData("Юбилейная улица", "12:20", "12:21", 291.4),
            StopData("Высоково", "12:25", "12:26", 300.0),
            StopData("Осинки", "12:30", "12:31", 308.5),
            StopData("Балванская", "12:35", "12:36", 317.1),
            StopData("Поворот на Юрьево", "12:40", "12:41", 325.7),
            StopData("Скурихинская", "12:45", "12:46", 334.2),
            StopData("Овчинниковы", "12:50", "12:51", 342.8),
            StopData("Минины", "12:55", "12:56", 351.4),
            StopData("Кардаковы", "13:00", "13:01", 360.0),
            StopData("Фадеевцы / Липичи / Жохи", "13:05", "13:06", 368.5),
            StopData("Хаустовы", "13:10", "13:11", 377.1),
            StopData("Гулины", "13:15", "13:16", 385.7),
            StopData("Поворот на Ленинскую Искру", "13:20", "13:21", 394.2),
            StopData("Климичи", "13:25", "13:26", 402.8),
            StopData("Пост ГИБДД", "13:30", "13:31", 411.4),
            StopData("Автостанция Котельнич", "13:35", "13:36", 420.0),
            StopData("Широченки", "13:40", "13:41", 428.5),
            StopData("Шестаковы", "13:45", "13:46", 437.1),
            StopData("Копылы", "13:50", "13:51", 445.7),
            StopData("Ванюшенки", "14:00", "14:01", 462.8),
            StopData("Вишкиль", "14:05", "14:06", 471.4),
            StopData("Мамаи", "14:10", "14:11", 480.0),
            StopData("Смирновы", "14:15", "14:16", 488.5),
            StopData("Криуша", "14:25", "14:26", 505.7),
            StopData("Горбуновщина", "14:30", "14:31", 514.2),
            StopData("Сорвижи", "14:35", "14:36", 522.8),
            StopData("Поворот на Кормино", "14:50", "14:51", 548.5),
            StopData("Поворот на Шабры", "14:55", "14:56", 557.1),
            StopData("Поворот на Шембеть", "15:00", "15:01", 565.7),
            StopData("Поворот на Арбаж", "15:05", "15:06", 574.2),
            StopData("Мосуны", "15:10", "15:11", 582.8),
            StopData("Чернушка", "15:15", "15:16", 591.4),
            StopData("Мостолыги", "15:20", "15:21", 600.0),
            StopData("Лобасты", "15:25", "15:26", 608.5),
            StopData("Автостанция Арбаж", "15:30", "15:30", 617.1)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 3 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip4(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Автостанция Арбаж", "16:00", "16:00", 0.0),
            StopData("Лобасты", "16:05", "16:06", 8.5),
            StopData("Мостолыги", "16:10", "16:11", 17.1),
            StopData("Чернушка", "16:15", "16:16", 25.7),
            StopData("Мосуны", "16:20", "16:21", 34.2),
            StopData("Поворот на Арбаж", "16:25", "16:26", 42.8),
            StopData("Поворот на Шембеть", "16:30", "16:31", 51.4),
            StopData("Поворот на Шабры", "16:35", "16:36", 60.0),
            StopData("Поворот на Кормино", "16:40", "16:41", 68.5),
            StopData("Криуша", "16:45", "16:46", 77.1),
            StopData("Горбуновщина", "16:50", "16:51", 85.7),
            StopData("Сорвижи", "16:55", "16:56", 94.2),
            StopData("Смирновы", "17:15", "17:16", 128.5),
            StopData("Мамаи", "17:20", "17:21", 137.1),
            StopData("Вишкиль", "17:25", "17:26", 145.7),
            StopData("Копылы", "17:40", "17:41", 171.4),
            StopData("Шестаковы", "17:45", "17:46", 180.0),
            StopData("Широченки", "17:50", "17:51", 188.5),
            StopData("Автостанция Котельнич", "17:55", "17:56", 197.1),
            StopData("Пост ГИБДД", "18:00", "18:01", 205.7),
            StopData("Климичи", "18:05", "18:06", 214.2),
            StopData("Поворот на Ленинскую Искру", "18:10", "18:11", 222.8),
            StopData("Гулины", "18:15", "18:16", 231.4),
            StopData("Хаустовы", "18:20", "18:21", 240.0),
            StopData("Фадеевцы / Липичи / Жохи", "18:25", "18:26", 248.5),
            StopData("Кардаковы", "18:30", "18:31", 257.1),
            StopData("Минины", "18:35", "18:36", 265.7),
            StopData("Овчинниковы", "18:40", "18:41", 274.2),
            StopData("Скурихинская", "18:45", "18:46", 282.8),
            StopData("Поворот на Юрьево", "18:50", "18:51", 291.4),
            StopData("Балванская", "18:55", "18:56", 300.0),
            StopData("Осинки", "19:00", "19:01", 308.5),
            StopData("Высоково", "19:05", "19:06", 317.1),
            StopData("Юбилейная улица", "19:10", "19:11", 325.7),
            StopData("Мебельная фабрика", "19:15", "19:16", 334.2),
            StopData("Магазин Наш дом", "19:20", "19:21", 342.8),
            StopData("Больница", "19:25", "19:26", 351.4),
            StopData("ТЦ Муравейник", "19:30", "19:31", 360.0),
            StopData("Магазин Петушок", "19:35", "19:36", 368.5),
            StopData("Детские ясли", "19:40", "19:41", 377.1),
            StopData("Магазин Золотая марка", "19:45", "19:46", 385.7),
            StopData("Автостанция Орлов", "19:50", "19:51", 394.2),
            StopData("Поворот на Криничи", "19:55", "19:56", 402.8),
            StopData("Назаровы", "20:00", "20:01", 411.4),
            StopData("Весниничи", "20:05", "20:06", 420.0),
            StopData("Казаковцевы", "20:10", "20:11", 428.5),
            StopData("Щенники", "20:15", "20:16", 437.1),
            StopData("Нижние Опарины", "20:20", "20:21", 445.7),
            StopData("Кузнецы-Орлов", "20:25", "20:26", 454.2),
            StopData("Колеватовы", "20:30", "20:31", 462.8),
            StopData("Боярщина", "20:35", "20:36", 471.4),
            StopData("Раменье", "20:40", "20:41", 480.0),
            StopData("Юркичи", "20:45", "20:46", 488.5),
            StopData("Зоновщина", "20:50", "20:51", 497.1),
            StopData("Верхняя Боярщина", "20:55", "20:56", 505.7),
            StopData("Красногоры", "21:00", "21:01", 514.2),
            StopData("Поворот на Цепели", "21:05", "21:06", 522.8),
            StopData("Река Великая", "21:10", "21:11", 531.4),
            StopData("Лаптевы", "21:15", "21:16", 540.0),
            StopData("Поворот на Малое Чураково", "21:20", "21:21", 548.5),
            StopData("Поворот на Медяны", "21:25", "21:26", 557.1),
            StopData("Поворот на Юрью", "21:30", "21:31", 565.7),
            StopData("Сады Урожай-1", "21:35", "21:36", 574.2),
            StopData("Горцы", "21:40", "21:41", 582.8),
            StopData("Поворот на Мурыгино", "21:45", "21:46", 591.4),
            StopData("Поворот на Гирсово", "21:50", "21:51", 600.0),
            StopData("Улица Дзержинского", "21:55", "21:56", 608.5),
            StopData("Автовокзал Киров", "22:00", "22:00", 617.1)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 4 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip5(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Автовокзал Киров", "08:30", "08:30", 0.0),
            StopData("Слобода Поскрёбышевы", "08:33", "08:34", 10.7),
            StopData("Советский тракт / Сады Дружба", "08:34", "08:35", 12.8),
            StopData("По требованию", "08:35", "08:36", 14.3),
            StopData("Сады Рассвет", "08:36", "08:37", 18.2),
            StopData("Дуркино / Пеньково", "08:38", "08:39", 22.4),
            StopData("Чирки", "08:39", "08:40", 25.7),
            StopData("Поворот на Пасегово", "08:40", "08:41", 29.3),
            StopData("Сады Искож", "08:41", "08:42", 32.8),
            StopData("Дряхловщина", "08:42", "08:43", 36.4),
            StopData("Сады Импульс", "08:43", "08:44", 39.8),
            StopData("Вахрёнки", "08:44", "08:45", 44.2),
            StopData("Камешник", "08:45", "08:46", 46.9),
            StopData("Козулинцы", "08:46", "08:47", 52.5),
            StopData("Исуповская", "08:47", "08:48", 58.3),
            StopData("Сады Исуповское", "08:48", "08:49", 60.7),
            StopData("Быстрицкий тубсанаторий", "08:55", "08:56", 85.5),
            StopData("Река Быстрица", "08:58", "08:59", 95.2),
            StopData("Полом", "09:03", "09:04", 120.8),
            StopData("Шабардёнки", "09:12", "09:13", 165.0),
            StopData("Мочалище", "09:17", "09:18", 190.5),
            StopData("Бонево", "09:22", "09:23", 212.3),
            StopData("Большое Рогово", "09:24", "09:25", 225.8),
            StopData("50-й километр", "09:27", "09:28", 245.2),
            StopData("Поворот на Коршик", "09:33", "09:34", 275.0),
            StopData("Кадесниково", "09:45", "09:46", 340.8),
            StopData("Пунгино", "09:48", "09:49", 360.5),
            StopData("Поворот на Скородум", "09:54", "09:55", 395.2),
            StopData("Поворот на Вьюги", "10:00", "10:01", 425.8),
            StopData("Поворот на Верхошижемье – 1", "10:03", "10:04", 445.0),
            StopData("Кафе Корона", "10:04", "10:05", 455.3),
            StopData("Автостанция Верхошижемье", "10:05", "10:06", 465.8),
            StopData("Поворот на Верхошижемье", "10:07", "10:08", 482.5),
            StopData("Москва", "10:09", "10:10", 505.2),
            StopData("Поворот на Зониху", "10:18", "10:19", 555.8),
            StopData("Рамеши", "10:20", "10:21", 575.0),
            StopData("Поворот на Беляки", "10:22", "10:23", 595.3),
            StopData("Поворот на Мякиши", "10:23", "10:24", 608.2),
            StopData("Дуброва", "10:28", "10:29", 655.0),
            StopData("Поворот на Кожу", "10:33", "10:34", 695.8),
            StopData("Поворот на Зашижемье", "10:37", "10:38", 735.2),
            StopData("Калугино", "10:41", "10:42", 775.0),
            StopData("Поворот на Лесозавод", "10:50", "10:51", 835.8),
            StopData("Поворот на Котельнич", "10:52", "10:53", 855.3),
            StopData("Поворот на Жерновогорье", "10:53", "10:54", 862.5),
            StopData("Мясокомбинат", "10:54", "10:55", 875.8),
            StopData("Автоколонна", "10:55", "10:56", 880.0),
            StopData("Улица Кондакова", "10:55", "10:56", 882.5),
            StopData("Рынок", "10:56", "10:57", 885.8),
            StopData("Почта", "10:56", "10:57", 890.0),
            StopData("Перчаточная фабрика", "10:57", "10:58", 895.2),
            StopData("Советск – Баня", "10:57", "10:58", 898.5)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 5 (Киров → Советск) inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip6(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Советск – Баня", "11:40", "11:40", 0.0),
            StopData("Перчаточная фабрика", "11:41", "11:42", 10.7),
            StopData("Почта", "11:42", "11:43", 21.4),
            StopData("Рынок", "11:43", "11:44", 32.1),
            StopData("Улица Кондакова", "11:44", "11:45", 42.8),
            StopData("Автоколонна", "11:45", "11:46", 53.5),
            StopData("Мясокомбинат", "11:46", "11:47", 64.2),
            StopData("Поворот на Жерновогорье", "11:47", "11:48", 74.9),
            StopData("Поворот на Котельнич", "11:48", "11:49", 85.6),
            StopData("Поворот на Лесозавод", "11:49", "11:50", 96.3),
            StopData("Калугино", "12:01", "12:02", 117.0),
            StopData("Поворот на Зашижемье", "12:07", "12:08", 127.7),
            StopData("Поворот на Кожу", "12:12", "12:13", 138.4),
            StopData("Дуброва", "12:15", "12:16", 149.1),
            StopData("Поворот на Мякиши", "12:18", "12:19", 159.8),
            StopData("Поворот на Беляки", "12:19", "12:20", 170.5),
            StopData("Рамеши", "12:20", "12:21", 181.2),
            StopData("Поворот на Зониху", "12:21", "12:22", 191.9),
            StopData("Москва", "12:29", "12:30", 202.6),
            StopData("Поворот на Верхошижемье", "12:31", "12:32", 213.3),
            StopData("Автостанция Верхошижемье", "12:33", "12:34", 224.0),
            StopData("Кафе Корона", "12:34", "12:35", 234.7),
            StopData("Поворот на Верхошижемье – 1", "12:35", "12:36", 245.4),
            StopData("Поворот на Вьюги", "12:37", "12:38", 256.1),
            StopData("Поворот на Скородум", "12:42", "12:43", 266.8),
            StopData("Пунгино", "12:46", "12:47", 277.5),
            StopData("Кадесниково", "12:47", "12:48", 288.2),
            StopData("Поворот на Коршик", "12:58", "12:59", 298.9),
            StopData("50-й километр", "13:02", "13:03", 309.6),
            StopData("Большое Рогово", "13:03", "13:04", 320.3),
            StopData("Бонево", "13:04", "13:05", 331.0),
            StopData("Мочалище", "13:07", "13:08", 341.7),
            StopData("Шабардёнки", "13:11", "13:12", 352.4),
            StopData("Полом", "13:19", "13:20", 363.1),
            StopData("Река Быстрица", "13:24", "13:25", 373.8),
            StopData("Быстрицкий тубсанаторий", "13:29", "13:30", 384.5),
            StopData("Сады Исуповское", "13:34", "13:35", 395.2),
            StopData("Исуповская", "13:35", "13:36", 405.9),
            StopData("Козулинцы", "13:36", "13:37", 416.6),
            StopData("Камешник", "13:37", "13:38", 427.3),
            StopData("Вахрёнки", "13:38", "13:39", 438.0),
            StopData("Сады Импульс", "13:39", "13:40", 448.7),
            StopData("Дряхловщина", "13:40", "13:41", 459.4),
            StopData("Сады Искож", "13:41", "13:42", 470.1),
            StopData("Поворот на Пасегово", "13:42", "13:43", 480.8),
            StopData("Чирки", "13:43", "13:44", 491.5),
            StopData("Дуркино / Пеньково", "13:44", "13:45", 502.2),
            StopData("Сады Рассвет", "13:45", "13:46", 512.9),
            StopData("По требованию", "13:46", "13:47", 523.6),
            StopData("Советский тракт / Сады Дружба", "13:47", "13:48", 534.3),
            StopData("Слобода Поскрёбышевы", "13:48", "13:49", 545.0),
            StopData("Автовокзал Киров", "13:50", "13:50", 555.7)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 6 (Советск → Киров) inserted: ${stops.size}")
    }

    // ========== КООРДИНАТЫ ВСЕХ ОСТАНОВОК ==========

    private fun MutableMap<String, Pair<Double, Double>>.initializeAllCoordinates() {
        clear()

        // ========== МАРШРУТ M1: СЛОБОДСКОЙ → КИРОВ ==========
        put("M1_Автостанция Слободского", Pair(58.721262, 50.181554))
        put("M1_Рождественская улица", Pair(58.732264, 50.180422))
        put("M1_Улица Грина", Pair(58.724187, 50.176146))
        put("M1_ДЭП-4", Pair(58.723925, 50.159868))
        put("M1_Стулово", Pair(58.722281, 50.150535))
        put("M1_ПМК-14", Pair(58.720846, 50.143582))
        put("M1_Ситники", Pair(58.716099, 50.128014))
        put("M1_Первомайский поворот", Pair(58.711846, 50.113291))
        put("M1_Подсобное хозяйство", Pair(58.699579, 50.088848))
        put("M1_Вахруши", Pair(58.692388, 50.058386))
        put("M1_Школа", Pair(58.690639, 50.043339))
        put("M1_Рубежница", Pair(58.677420, 50.019075))
        put("M1_Логуновы", Pair(58.677369, 50.002528))
        put("M1_Осинцы", Pair(58.676770, 49.968033))
        put("M1_Луза", Pair(58.662801, 49.926055))
        put("M1_Сады Биохимик-2", Pair(58.657788, 49.912724))
        put("M1_Зониха", Pair(58.651356, 49.881543))
        put("M1_Пантелеевы", Pair(58.648256, 49.865724))
        put("M1_Столбово", Pair(58.642150, 49.852492))
        put("M1_Шихово", Pair(58.634881, 49.831301))
        put("M1_Поворот на Бобино", Pair(58.630515, 49.813792))
        put("M1_Трушковы", Pair(58.627756, 49.804827))
        put("M1_Новомакарьевское кладбище", Pair(58.615108, 49.756363))
        put("M1_Порошинский поворот", Pair(58.615108, 49.756363))
        put("M1_Слобода Макарье", Pair(58.613060, 49.750308))
        put("M1_Троицкая церковь", Pair(58.618951, 49.719748))
        put("M1_Проезжая улица", Pair(58.618951, 49.719748))
        put("M1_Заповедная улица", Pair(58.618951, 49.719748))
        put("M1_Улица Красный Химик", Pair(58.609774, 49.686753))
        put("M1_Слобода Дымково", Pair(58.590357, 49.682018))
        put("M1_Профсоюзная улица", Pair(58.606530, 49.681039))
        put("M1_Улица МОПРа", Pair(58.590357, 49.682018))
        put("M1_Храм Иоанна Предтечи", Pair(58.594686, 49.682450))
        put("M1_Трифонов монастырь", Pair(58.593481, 49.659156))
        put("M1_Филармония", Pair(58.593326, 49.633375))
        put("M1_Областная больница", Pair(58.589119, 49.655913))
        put("M1_ЦУМ", Pair(58.589119, 49.655913))
        put("M1_Автовокзал Киров", Pair(58.583651, 49.650495))

        // ========== МАРШРУТ M2: КИРОВ → СЛОБОДСКОЙ ==========
        put("M2_Автовокзал Киров", Pair(58.583651, 49.650495))
        put("M2_ЦУМ", Pair(58.589119, 49.655913))
        put("M2_Областная больница", Pair(58.589119, 49.655913))
        put("M2_Филармония", Pair(58.593326, 49.633375))
        put("M2_Трифонов монастырь", Pair(58.593481, 49.659156))
        put("M2_Храм Иоанна Предтечи", Pair(58.594686, 49.682450))
        put("M2_Улица МОПРа", Pair(58.590357, 49.682018))
        put("M2_Профсоюзная улица", Pair(58.606530, 49.681039))
        put("M2_Слобода Дымково", Pair(58.590357, 49.682018))
        put("M2_Улица Красный Химик", Pair(58.609774, 49.686753))
        put("M2_Заповедная улица", Pair(58.618951, 49.719748))
        put("M2_Проезжая улица", Pair(58.618951, 49.719748))
        put("M2_Троицкая церковь", Pair(58.618951, 49.719748))
        put("M2_Слобода Макарье", Pair(58.613060, 49.750308))
        put("M2_Порошинский поворот", Pair(58.615108, 49.756363))
        put("M2_Новомакарьевское кладбище", Pair(58.615108, 49.756363))
        put("M2_Трушковы", Pair(58.627756, 49.804827))
        put("M2_Поворот на Бобино", Pair(58.630515, 49.813792))
        put("M2_Шихово", Pair(58.634881, 49.831301))
        put("M2_Столбово", Pair(58.642150, 49.852492))
        put("M2_Пантелеевы", Pair(58.648256, 49.865724))
        put("M2_Зониха", Pair(58.651356, 49.881543))
        put("M2_Сады Биохимик-2", Pair(58.657788, 49.912724))
        put("M2_Луза", Pair(58.662801, 49.926055))
        put("M2_Осинцы", Pair(58.676770, 49.968033))
        put("M2_Логуновы", Pair(58.677369, 50.002528))
        put("M2_Рубежница", Pair(58.677420, 50.019075))
        put("M2_Школа", Pair(58.690639, 50.043339))
        put("M2_Вахруши", Pair(58.692388, 50.058386))
        put("M2_Подсобное хозяйство", Pair(58.699579, 50.088848))
        put("M2_Первомайский поворот", Pair(58.711846, 50.113291))
        put("M2_Ситники", Pair(58.716099, 50.128014))
        put("M2_ПМК-14", Pair(58.720846, 50.143582))
        put("M2_Стулово", Pair(58.722281, 50.150535))
        put("M2_ДЭП-4", Pair(58.723925, 50.159868))
        put("M2_Улица Грина", Pair(58.724187, 50.176146))
        put("M2_Рождественская улица", Pair(58.732264, 50.180422))
        put("M2_Автостанция Слободского", Pair(58.721262, 50.181554))

        // ========== МАРШРУТ M3: КИРОВ → КОТЕЛЬНИЧ ==========
        put("M3_Автовокзал Киров", Pair(58.583651, 49.650495))
        put("M3_Улица Дзержинского", Pair(58.633361, 49.617675))
        put("M3_Поворот на Гирсово", Pair(58.737164, 49.552364))
        put("M3_Поворот на Мурыгино", Pair(58.747287, 49.531892))
        put("M3_Горцы", Pair(58.759871, 49.512871))
        put("M3_Сады Урожай-1", Pair(58.770222, 49.482504))
        put("M3_Поворот на Юрью", Pair(58.772118, 49.473849))
        put("M3_Поворот на Медяны", Pair(58.771935, 49.384934))
        put("M3_Поворот на Малое Чураково", Pair(58.755541, 49.315456))
        put("M3_Лаптевы", Pair(58.745547, 49.287798))
        put("M3_Река Великая", Pair(58.737099, 49.250816))
        put("M3_Поворот на Цепели", Pair(58.722459, 49.221294))
        put("M3_Красногоры", Pair(58.715344, 49.210115))
        put("M3_Верхняя Боярщина", Pair(58.707308, 49.192200))
        put("M3_Зоновщина", Pair(58.702078, 49.169786))
        put("M3_Юркичи", Pair(58.692492, 49.129097))
        put("M3_Раменье", Pair(58.685231, 49.106490))
        put("M3_Боярщина", Pair(58.675677, 49.069284))
        put("M3_Колеватовы", Pair(58.670739, 49.056520))
        put("M3_Кузнецы-Орлов", Pair(58.658588, 49.029802))
        put("M3_Нижние Опарины", Pair(58.647280, 49.006306))
        put("M3_Щенники", Pair(58.630891, 48.987304))
        put("M3_Казаковцевы", Pair(58.612166, 48.975044))
        put("M3_Весниничи", Pair(58.597590, 48.964840))
        put("M3_Назаровы", Pair(58.574886, 48.939167))
        put("M3_Поворот на Криничи", Pair(58.560212, 48.918384))
        put("M3_Автостанция Орлов", Pair(58.548402, 48.898684))
        put("M3_Магазин Золотая марка", Pair(58.542541, 48.903440))
        put("M3_Детские ясли", Pair(58.540691, 48.901498))
        put("M3_Магазин Петушок", Pair(58.536763, 48.895470))
        put("M3_ТЦ Муравейник", Pair(58.534106, 48.891417))
        put("M3_Больница", Pair(58.531279, 48.886428))
        put("M3_Магазин Наш дом", Pair(58.533788, 48.880280))
        put("M3_Мебельная фабрика", Pair(58.532313, 48.875484))
        put("M3_Юбилейная улица", Pair(58.531889, 48.870343))
        put("M3_Высоково", Pair(58.543928, 48.753756))
        put("M3_Осинки", Pair(58.489731, 48.587237))
        put("M3_Балванская", Pair(58.480585, 48.572046))
        put("M3_Поворот на Юрьево", Pair(58.452485, 48.531782))
        put("M3_Скурихинская", Pair(58.438628, 48.516946))
        put("M3_Овчинниковы", Pair(58.419006, 48.483533))
        put("M3_Минины", Pair(58.408897, 48.474876))
        put("M3_Кардаковы", Pair(58.400170, 48.467495))
        put("M3_Фадеевцы / Липичи / Жохи", Pair(58.376832, 48.453930))
        put("M3_Хаустовы", Pair(58.359602, 48.437854))
        put("M3_Гулины", Pair(58.348800, 48.428992))
        put("M3_Поворот на Ленинскую Искру", Pair(58.334175, 48.417026))
        put("M3_Климичи", Pair(58.324498, 48.403480))
        put("M3_Пост ГИБДД", Pair(58.318261, 48.396782))
        put("M3_Автостанция Котельнич", Pair(58.312207, 48.341900))
        put("M3_Широченки", Pair(58.260866, 48.306513))
        put("M3_Шестаковы", Pair(58.247822, 48.306982))
        put("M3_Копылы", Pair(58.212727, 48.302973))
        put("M3_Ванюшенки", Pair(58.151193, 48.330589))
        put("M3_Вишкиль", Pair(58.092038, 48.318224))
        put("M3_Мамаи", Pair(58.004120, 48.280065))
        put("M3_Смирновы", Pair(57.985803, 48.296416))
        put("M3_Криуша", Pair(57.908502, 48.412161))
        put("M3_Горбуновщина", Pair(57.886925, 48.447575))
        put("M3_Сорвижи", Pair(57.864274, 48.534764))
        put("M3_Поворот на Кормино", Pair(57.887566, 48.355890))
        put("M3_Поворот на Шабры", Pair(57.845882, 48.312336))
        put("M3_Поворот на Шембеть", Pair(57.810725, 48.283248))
        put("M3_Поворот на Арбаж", Pair(57.791532, 48.269725))
        put("M3_Мосуны", Pair(57.763075, 48.274191))
        put("M3_Чернушка", Pair(57.743750, 48.268683))
        put("M3_Мостолыги", Pair(57.712196, 48.265604))
        put("M3_Лобасты", Pair(57.690938, 48.290700))
        put("M3_Автостанция Арбаж", Pair(57.680673, 48.307524))

        // ========== МАРШРУТ M5: КИРОВ → СОВЕТСК ==========
        put("M5_Автовокзал Киров", Pair(58.583651, 49.650495))
        put("M5_Слобода Поскрёбышевы", Pair(58.582198, 49.651007))
        put("M5_Советский тракт / Сады Дружба", Pair(58.556697, 49.617762))
        put("M5_По требованию", Pair(58.552340, 49.609491))
        put("M5_Сады Рассвет", Pair(58.550355, 49.605025))
        put("M5_Дуркино / Пеньково", Pair(58.541718, 49.591076))
        put("M5_Чирки", Pair(58.532577, 49.583014))
        put("M5_Поворот на Пасегово", Pair(58.522409, 49.582045))
        put("M5_Сады Искож", Pair(58.510226, 49.580838))
        put("M5_Дряхловщина", Pair(58.502427, 49.577070))
        put("M5_Сады Импульс", Pair(58.491975, 49.577957))
        put("M5_Вахрёнки", Pair(58.481576, 49.582436))
        put("M5_Камешник", Pair(58.468829, 49.569956))
        put("M5_Козулинцы", Pair(58.463111, 49.564291))
        put("M5_Исуповская", Pair(58.449941, 49.554490))
        put("M5_Сады Исуповское", Pair(58.434498, 49.546991))
        put("M5_Быстрицкий тубсанаторий", Pair(58.387618, 49.506806))
        put("M5_Река Быстрица", Pair(58.377149, 49.489401))
        put("M5_Полом", Pair(58.350709, 49.461301))
        put("M5_Шабардёнки", Pair(58.318014, 49.391780))
        put("M5_Мочалище", Pair(58.284031, 49.369237))
        put("M5_Бонево", Pair(58.257448, 49.362716))
        put("M5_Большое Рогово", Pair(58.245484, 49.351817))
        put("M5_50-й километр", Pair(58.225738, 49.326817))
        put("M5_Поворот на Коршик", Pair(58.200094, 49.295026))
        put("M5_Кадесниково", Pair(58.146925, 49.177334))
        put("M5_Пунгино", Pair(58.131892, 49.137154))
        put("M5_Поворот на Скородум", Pair(58.092797, 49.103550))
        put("M5_Поворот на Вьюги", Pair(58.051555, 49.084740))
        put("M5_Поворот на Верхошижемье – 1", Pair(58.030566, 49.108676))
        put("M5_Кафе Корона", Pair(58.018583, 49.125480))
        put("M5_Автостанция Верхошижемье", Pair(58.007893, 49.106358))
        put("M5_Поворот на Верхошижемье", Pair(57.988572, 49.121569))
        put("M5_Москва", Pair(57.967823, 49.104849))
        put("M5_Поворот на Зониху", Pair(57.911039, 49.110130))
        put("M5_Рамеши", Pair(57.893767, 49.087572))
        put("M5_Поворот на Беляки", Pair(57.869751, 49.067881))
        put("M5_Поворот на Мякиши", Pair(57.859975, 49.045937))
        put("M5_Дуброва", Pair(57.826179, 48.967924))
        put("M5_Поворот на Кожу", Pair(57.793238, 48.930288))
        put("M5_Поворот на Зашижемье", Pair(57.751027, 48.887750))
        put("M5_Калугино", Pair(57.709416, 48.887900))
        put("M5_Поворот на Лесозавод", Pair(57.628964, 48.924445))
        put("M5_Поворот на Котельнич", Pair(57.604934, 48.920228))
        put("M5_Поворот на Жерновогорье", Pair(57.596660, 48.927875))
        put("M5_Мясокомбинат", Pair(57.582615, 48.941465))
        put("M5_Автоколонна", Pair(57.580973, 48.950117))
        put("M5_Улица Кондакова", Pair(57.583399, 48.953667))
        put("M5_Рынок", Pair(57.586212, 48.957449))
        put("M5_Почта", Pair(57.589558, 48.961872))
        put("M5_Перчаточная фабрика", Pair(57.592622, 48.966065))
        put("M5_Советск – Баня", Pair(57.592981, 48.969190))

        // ========== МАРШРУТ M6: СОВЕТСК → КИРОВ ==========
        put("M6_Советск – Баня", Pair(57.592981, 48.969190))
        put("M6_Перчаточная фабрика", Pair(57.592622, 48.966065))
        put("M6_Почта", Pair(57.589558, 48.961872))
        put("M6_Рынок", Pair(57.586212, 48.957449))
        put("M6_Улица Кондакова", Pair(57.583399, 48.953667))
        put("M6_Автоколонна", Pair(57.580973, 48.950117))
        put("M6_Мясокомбинат", Pair(57.582615, 48.941465))
        put("M6_Поворот на Жерновогорье", Pair(57.596660, 48.927875))
        put("M6_Поворот на Котельнич", Pair(57.604934, 48.920228))
        put("M6_Поворот на Лесозавод", Pair(57.628964, 48.924445))
        put("M6_Калугино", Pair(57.709416, 48.887900))
        put("M6_Поворот на Зашижемье", Pair(57.751027, 48.887750))
        put("M6_Поворот на Кожу", Pair(57.793238, 48.930288))
        put("M6_Дуброва", Pair(57.826179, 48.967924))
        put("M6_Поворот на Мякиши", Pair(57.859975, 49.045937))
        put("M6_Поворот на Беляки", Pair(57.869751, 49.067881))
        put("M6_Рамеши", Pair(57.893767, 49.087572))
        put("M6_Поворот на Зониху", Pair(57.911039, 49.110130))
        put("M6_Москва", Pair(57.967823, 49.104849))
        put("M6_Поворот на Верхошижемье", Pair(57.988572, 49.121569))
        put("M6_Автостанция Верхошижемье", Pair(58.007893, 49.106358))
        put("M6_Кафе Корона", Pair(58.018583, 49.125480))
        put("M6_Поворот на Верхошижемье – 1", Pair(58.030566, 49.108676))
        put("M6_Поворот на Вьюги", Pair(58.051555, 49.084740))
        put("M6_Поворот на Скородум", Pair(58.092797, 49.103550))
        put("M6_Пунгино", Pair(58.131892, 49.137154))
        put("M6_Кадесниково", Pair(58.146925, 49.177334))
        put("M6_Поворот на Коршик", Pair(58.200094, 49.295026))
        put("M6_50-й километр", Pair(58.225738, 49.326817))
        put("M6_Большое Рогово", Pair(58.245484, 49.351817))
        put("M6_Бонево", Pair(58.257448, 49.362716))
        put("M6_Мочалище", Pair(58.284031, 49.369237))
        put("M6_Шабардёнки", Pair(58.318014, 49.391780))
        put("M6_Полом", Pair(58.350709, 49.461301))
        put("M6_Река Быстрица", Pair(58.377149, 49.489401))
        put("M6_Быстрицкий тубсанаторий", Pair(58.387618, 49.506806))
        put("M6_Сады Исуповское", Pair(58.434498, 49.546991))
        put("M6_Исуповская", Pair(58.449941, 49.554490))
        put("M6_Козулинцы", Pair(58.463111, 49.564291))
        put("M6_Камешник", Pair(58.468829, 49.569956))
        put("M6_Вахрёнки", Pair(58.481576, 49.582436))
        put("M6_Сады Импульс", Pair(58.491975, 49.577957))
        put("M6_Дряхловщина", Pair(58.502427, 49.577070))
        put("M6_Сады Искож", Pair(58.510226, 49.580838))
        put("M6_Поворот на Пасегово", Pair(58.522409, 49.582045))
        put("M6_Чирки", Pair(58.532577, 49.583014))
        put("M6_Дуркино / Пеньково", Pair(58.541718, 49.591076))
        put("M6_Сады Рассвет", Pair(58.550355, 49.605025))
        put("M6_По требованию", Pair(58.552340, 49.609491))
        put("M6_Советский тракт / Сады Дружба", Pair(58.556697, 49.617762))
        put("M6_Слобода Поскрёбышевы", Pair(58.582198, 49.651007))
        put("M6_Автовокзал Киров", Pair(58.583651, 49.650495))


        // ========== МАРШРУТ M4: КОТЕЛЬНИЧ → КИРОВ ==========
        put("M4_Автостанция Арбаж", Pair(57.680673, 48.307524))
        put("M4_Лобасты", Pair(57.690938, 48.290700))
        put("M4_Мостолыги", Pair(57.712196, 48.265604))
        put("M4_Чернушка", Pair(57.743750, 48.268683))
        put("M4_Мосуны", Pair(57.763075, 48.274191))
        put("M4_Поворот на Арбаж", Pair(57.791532, 48.269725))
        put("M4_Поворот на Шембеть", Pair(57.810725, 48.283248))
        put("M4_Поворот на Шабры", Pair(57.845882, 48.312336))
        put("M4_Поворот на Кормино", Pair(57.887566, 48.355890))
        put("M4_Сорвижи", Pair(57.864274, 48.534764))
        put("M4_Горбуновщина", Pair(57.886925, 48.447575))
        put("M4_Криуша", Pair(57.908502, 48.412161))
        put("M4_Смирновы", Pair(57.985803, 48.296416))
        put("M4_Мамаи", Pair(58.004120, 48.280065))
        put("M4_Вишкиль", Pair(58.092038, 48.318224))
        put("M4_Копылы", Pair(58.212727, 48.302973))
        put("M4_Шестаковы", Pair(58.247822, 48.306982))
        put("M4_Широченки", Pair(58.260866, 48.306513))
        put("M4_Автостанция Котельнич", Pair(58.312207, 48.341900))
        put("M4_Пост ГИБДД", Pair(58.318261, 48.396782))
        put("M4_Климичи", Pair(58.324498, 48.403480))
        put("M4_Поворот на Ленинскую Искру", Pair(58.334175, 48.417026))
        put("M4_Гулины", Pair(58.348800, 48.428992))
        put("M4_Хаустовы", Pair(58.359602, 48.437854))
        put("M4_Фадеевцы / Липичи / Жохи", Pair(58.376832, 48.453930))
        put("M4_Кардаковы", Pair(58.400170, 48.467495))
        put("M4_Минины", Pair(58.408897, 48.474876))
        put("M4_Овчинниковы", Pair(58.419006, 48.483533))
        put("M4_Скурихинская", Pair(58.438628, 48.516946))
        put("M4_Поворот на Юрьево", Pair(58.452485, 48.531782))
        put("M4_Балванская", Pair(58.480585, 48.572046))
        put("M4_Осинки", Pair(58.489731, 48.587237))
        put("M4_Высоково", Pair(58.543928, 48.753756))
        put("M4_Юбилейная улица", Pair(58.531889, 48.870343))
        put("M4_Мебельная фабрика", Pair(58.532313, 48.875484))
        put("M4_Магазин Наш дом", Pair(58.533788, 48.880280))
        put("M4_Больница", Pair(58.531279, 48.886428))
        put("M4_ТЦ Муравейник", Pair(58.534106, 48.891417))
        put("M4_Магазин Петушок", Pair(58.536763, 48.895470))
        put("M4_Детские ясли", Pair(58.540691, 48.901498))
        put("M4_Магазин Золотая марка", Pair(58.542541, 48.903440))
        put("M4_Автостанция Орлов", Pair(58.548402, 48.898684))
        put("M4_Поворот на Криничи", Pair(58.560212, 48.918384))
        put("M4_Назаровы", Pair(58.574886, 48.939167))
        put("M4_Весниничи", Pair(58.597590, 48.964840))
        put("M4_Казаковцевы", Pair(58.612166, 48.975044))
        put("M4_Щенники", Pair(58.630891, 48.987304))
        put("M4_Нижние Опарины", Pair(58.647280, 49.006306))
        put("M4_Кузнецы-Орлов", Pair(58.658588, 49.029802))
        put("M4_Колеватовы", Pair(58.670739, 49.056520))
        put("M4_Боярщина", Pair(58.675677, 49.069284))
        put("M4_Раменье", Pair(58.685231, 49.106490))
        put("M4_Юркичи", Pair(58.692492, 49.129097))
        put("M4_Зоновщина", Pair(58.702078, 49.169786))
        put("M4_Верхняя Боярщина", Pair(58.707308, 49.192200))
        put("M4_Красногоры", Pair(58.715344, 49.210115))
        put("M4_Поворот на Цепели", Pair(58.722459, 49.221294))
        put("M4_Река Великая", Pair(58.737099, 49.250816))
        put("M4_Лаптевы", Pair(58.745547, 49.287798))
        put("M4_Поворот на Малое Чураково", Pair(58.755541, 49.315456))
        put("M4_Поворот на Медяны", Pair(58.771935, 49.384934))
        put("M4_Поворот на Юрью", Pair(58.772118, 49.473849))
        put("M4_Сады Урожай-1", Pair(58.770222, 49.482504))
        put("M4_Горцы", Pair(58.759871, 49.512871))
        put("M4_Поворот на Мурыгино", Pair(58.747287, 49.531892))
        put("M4_Поворот на Гирсово", Pair(58.737164, 49.552364))
        put("M4_Улица Дзержинского", Pair(58.633361, 49.617675))
        put("M4_Автовокзал Киров", Pair(58.583651, 49.650495))

        Log.d("DBHelper", "Кэш координат инициализирован: ${size} записей")
    }

    // ========== МЕТОД ДЛЯ ПОЛУЧЕНИЯ КООРДИНАТ ==========
    fun getStopCoordinates(stopName: String, tripId: Int = -1): Pair<Double, Double>? {
        val cleanName = stopName
            .replace("\\(.*?\\)".toRegex(), "")
            .trim()

        // Инициализируем кэш при первом использовании
        if (coordinatesCache.isEmpty()) {
            coordinatesCache.initializeAllCoordinates()
        }

        Log.d("DBHelper", "Поиск координат: '$stopName' (маршрут $tripId)")

        // 1. Сначала ищем точное совпадение с новым форматом (M1_, M2_, и т.д.)
        val prefixedName = when (tripId) {
            1 -> "M1_$cleanName"
            2 -> "M2_$cleanName"
            3 -> "M3_$cleanName"
            4 -> "M4_$cleanName"
            5 -> "M5_$cleanName"  // Теперь это Киров → Советск
            6 -> "M6_$cleanName"  // Теперь это Советск → Киров
            else -> cleanName
        }

        coordinatesCache[prefixedName]?.let {
            Log.d("DBHelper", "✓ Найдены координаты по префиксу маршрута: '$prefixedName'")
            return it
        }

        // 2. Ищем с учетом маршрута без префикса
        coordinatesCache[cleanName]?.let {
            Log.d("DBHelper", "✓ Найдены координаты без префикса: '$cleanName'")
            return it
        }

        // 3. Поиск по частичному совпадению
        for ((key, value) in coordinatesCache) {
            val cleanKey = key.replace("M[0-9]_", "").replace("\\(.*?\\)".toRegex(), "").trim()

            if (cleanName.contains(cleanKey) || cleanKey.contains(cleanName)) {
                Log.d("DBHelper", "✓ Частичное совпадение: '$cleanName' -> '$key'")
                return value
            }
        }

        // 4. Возвращаем координаты по умолчанию в зависимости от города
        val defaultCoords = when {
            cleanName.contains("киров", ignoreCase = true) -> Pair(58.583651, 49.650495)
            cleanName.contains("слободской", ignoreCase = true) -> Pair(58.721262, 50.181554)
            cleanName.contains("котельнич", ignoreCase = true) -> Pair(58.312207, 48.341900)
            cleanName.contains("советск", ignoreCase = true) -> Pair(57.592981, 48.969190)
            cleanName.contains("арбаж", ignoreCase = true) -> Pair(57.680673, 48.307524)
            cleanName.contains("орлов", ignoreCase = true) -> Pair(58.548402, 48.898684)
            cleanName.contains("верхозижемье", ignoreCase = true) -> Pair(58.007893, 49.106358)
            else -> null
        }

        if (defaultCoords != null) {
            Log.d("DBHelper", "⚠ Возвращаю координаты по городу: '$cleanName' -> ${defaultCoords.first}, ${defaultCoords.second}")
            return defaultCoords
        }

        Log.d("DBHelper", "❌ Не найдены координаты для: '$cleanName' (маршрут $tripId)")
        return Pair(58.600000, 49.600000) // Координаты Кирова по умолчанию
    }

    // ========== ДОБАВЛЕННЫЕ МЕТОДЫ ==========

    fun saveVerificationCode(userId: Int, email: String, code: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_CODE_USER_ID, userId)
            put(KEY_CODE_EMAIL, email)
            put(KEY_CODE, code)

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 15)
            val expiresAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
            put(KEY_EXPIRES_AT, expiresAt)
            put(KEY_IS_USED, 0)
        }
        return db.insert(TABLE_VERIFICATION_CODES, null, values)
    }

    fun verifyCode(userId: Int, email: String, code: String): Boolean {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_VERIFICATION_CODES 
            WHERE $KEY_CODE_USER_ID = ? AND $KEY_CODE_EMAIL = ? AND $KEY_CODE = ? 
            AND $KEY_IS_USED = 0 AND $KEY_EXPIRES_AT > datetime('now')
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), email, code))
        val isValid = cursor.moveToFirst()

        if (isValid) {
            val codeId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
            markCodeAsUsed(codeId)
        }

        cursor.close()
        return isValid
    }

    private fun markCodeAsUsed(codeId: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_IS_USED, 1)
        }
        db.update(TABLE_VERIFICATION_CODES, values, "$KEY_ID = ?", arrayOf(codeId.toString()))
    }

    fun activateUser(userId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_IS_ACTIVE, 1)
        }
        val rowsAffected = db.update(
            TABLE_USERS, values, "$KEY_ID = ?", arrayOf(userId.toString())
        )
        return rowsAffected > 0
    }

    fun getUserById(userId: Int): User? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $KEY_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        return if (cursor.moveToFirst()) {
            User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE)),
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_DATE)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun getTripById(tripId: Int): Trip? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_TRIPS WHERE $KEY_ID = ?",
            arrayOf(tripId.toString())
        )

        return if (cursor.moveToFirst()) {
            Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE))
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun getBookingsByUserIdFull(userId: Int): List<Booking> {
        val bookings = mutableListOf<Booking>()
        val db = readableDatabase

        val query = """
        SELECT b.*, t.$KEY_FROM_CITY, t.$KEY_TO_CITY, t.$KEY_DEPARTURE_TIME, t.$KEY_PRICE
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_ID
        WHERE b.$KEY_USER_ID_FK = ? 
        ORDER BY b.$KEY_TRIP_DATE DESC, b.$KEY_BOOKING_DATE DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        while (cursor.moveToNext()) {
            val booking = Booking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID_FK)),
                tripId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID_BOOKING)),
                passengerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME)),
                passengerEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL)),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                tripDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRIP_DATE)), // Добавляем tripDate
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_STATUS)),
                seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            )
            bookings.add(booking)
        }
        cursor.close()
        return bookings
    }

    fun getBookingsByUserId(userId: Int): List<Booking> {
        return getBookingsByUserIdFull(userId)
    }

    fun getBookingWithTripInfo(bookingId: Int): Pair<Booking, Trip>? {
        val db = readableDatabase
        val query = """
        SELECT b.*, t.* 
        FROM $TABLE_BOOKINGS b
        JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_ID
        WHERE b.$KEY_ID = ?
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(bookingId.toString()))

        return if (cursor.moveToFirst()) {
            val booking = Booking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID_FK)),
                tripId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TRIP_ID_BOOKING)),
                passengerName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_NAME)),
                passengerEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSENGER_EMAIL)),
                bookingDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                tripDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TRIP_DATE)), // Добавляем tripDate
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOKING_STATUS)),
                seatNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEAT_NUMBER))
            )
            val trip = Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("${TABLE_TRIPS}.$KEY_ID")),
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

    fun deleteBooking(bookingId: Int): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_BOOKINGS,
            "$KEY_ID = ?",
            arrayOf(bookingId.toString())
        )
        return rowsAffected > 0
    }

    fun addBookingWithSeat(userId: Int, tripId: Int, passengerName: String,
                           passengerEmail: String, seatNumber: Int): Long {
        return addBooking(userId, tripId, passengerName, passengerEmail, seatNumber)
    }

    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS ORDER BY $KEY_ROLE, $KEY_USERNAME",
            null
        )

        while (cursor.moveToNext()) {
            val user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE)),
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_DATE)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
            )
            users.add(user)
        }
        cursor.close()
        return users
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
            put(KEY_IS_ACTIVE, 1)
        }
        return db.insert(TABLE_USERS, null, values)
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
            put(KEY_IS_ACTIVE, if (user.isActive) 1 else 0)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$KEY_ID = ?",
            arrayOf(user.id.toString())
        )
        return rowsAffected > 0
    }

    fun deleteUser(userId: Int): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_USERS,
            "$KEY_ID = ?",
            arrayOf(userId.toString())
        )
        return rowsAffected > 0
    }

    // ========== ОСНОВНЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С БАЗОЙ ==========
    fun getUser(username: String, password: String): User? {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_USERS 
            WHERE $KEY_USERNAME = ? AND $KEY_PASSWORD = ? AND $KEY_IS_ACTIVE = 1
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(username, password))

        return if (cursor.moveToFirst()) {
            User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PHONE)),
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_DATE)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun getAllTrips(): List<Trip> {
        val trips = mutableListOf<Trip>()
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRIPS ORDER BY $KEY_DEPARTURE_TIME ASC", null)

        while (cursor.moveToNext()) {
            val trip = Trip(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                fromCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROM_CITY)),
                toCity = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TO_CITY)),
                departureTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEPARTURE_TIME)),
                arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARRIVAL_TIME)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE))
            )
            trips.add(trip)
        }
        cursor.close()
        return trips
    }

    fun getStopsByTripId(tripId: Int): List<Stop> {
        val stops = mutableListOf<Stop>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_STOPS WHERE $KEY_TRIP_ID_FK = ? ORDER BY $KEY_ARRIVAL_TIME_STOP ASC",
            arrayOf(tripId.toString())
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

    fun addBooking(userId: Int, tripId: Int, passengerName: String,
                   passengerEmail: String, seatNumber: Int, tripDate: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID_FK, userId)
            put(KEY_TRIP_ID_BOOKING, tripId)
            put(KEY_PASSENGER_NAME, passengerName)
            put(KEY_PASSENGER_EMAIL, passengerEmail)
            put(KEY_SEAT_NUMBER, seatNumber)
            put(KEY_TRIP_DATE, tripDate) // Добавляем дату поездки
            put(KEY_BOOKING_STATUS, "Активен")
        }
        return db.insert(TABLE_BOOKINGS, null, values)
    }

    fun addBookingWithSeat(userId: Int, tripId: Int, passengerName: String,
                           passengerEmail: String, seatNumber: Int, tripDate: String): Long {
        return addBooking(userId, tripId, passengerName, passengerEmail, seatNumber, tripDate)
    }

    fun addBookingForUser(userId: Int, tripId: Int, passengerName: String,
                          passengerEmail: String, seatNumber: Int, tripDate: String): Long {
        return addBooking(userId, tripId, passengerName, passengerEmail, seatNumber, tripDate)
    }

    // Обновите существующие методы с новой сигнатурой
    fun addBooking(userId: Int, tripId: Int, passengerName: String,
                   passengerEmail: String, seatNumber: Int): Long {
        // Для обратной совместимости - используем текущую дату
        return addBooking(userId, tripId, passengerName, passengerEmail, seatNumber,
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    fun addBookingForUser(userId: Int, tripId: Int, passengerName: String,
                          passengerEmail: String, seatNumber: Int): Long {
        return addBooking(userId, tripId, passengerName, passengerEmail, seatNumber)
    }

    fun getBookedSeats(tripId: Int, tripDate: String? = null): List<Int> {
        val bookedSeats = mutableListOf<Int>()
        val db = readableDatabase

        val query: String
        val selectionArgs: Array<String>

        if (tripDate != null) {
            // Если дата указана, фильтруем по дате
            query = "SELECT $KEY_SEAT_NUMBER FROM $TABLE_BOOKINGS WHERE $KEY_TRIP_ID_BOOKING = ? AND $KEY_TRIP_DATE = ?"
            selectionArgs = arrayOf(tripId.toString(), tripDate)
        } else {
            // Если дата не указана, возвращаем все места для этого рейса (для обратной совместимости)
            query = "SELECT $KEY_SEAT_NUMBER FROM $TABLE_BOOKINGS WHERE $KEY_TRIP_ID_BOOKING = ?"
            selectionArgs = arrayOf(tripId.toString())
        }

        val cursor = db.rawQuery(query, selectionArgs)

        while (cursor.moveToNext()) {
            val seat = cursor.getInt(0)
            if (seat > 0) {
                bookedSeats.add(seat)
            }
        }
        cursor.close()
        return bookedSeats
    }


    // ========== МЕТОДЫ ДЛЯ РЕГИСТРАЦИИ И ВЕРИФИКАЦИИ ==========
    fun isUsernameExists(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USERS WHERE $KEY_USERNAME = ?",
            arrayOf(username)
        )
        val exists = if (cursor.moveToFirst()) cursor.getInt(0) > 0 else false
        cursor.close()
        return exists
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USERS WHERE $KEY_EMAIL = ?",
            arrayOf(email)
        )
        val exists = if (cursor.moveToFirst()) cursor.getInt(0) > 0 else false
        cursor.close()
        return exists
    }

    fun registerUser(username: String, password: String, role: String,
                     fullName: String, email: String, phone: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
            put(KEY_ROLE, role)
            put(KEY_FULL_NAME, fullName)
            put(KEY_EMAIL, email)
            put(KEY_PHONE, phone)
            put(KEY_IS_ACTIVE, 1)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    // ========== МЕТОДЫ ДЛЯ ОТЧЕТОВ ==========
    fun getTodaySales(): String {
        val db = readableDatabase
        val query = """
            SELECT COUNT(*) as count, SUM(t.$KEY_PRICE) as total 
            FROM $TABLE_BOOKINGS b 
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_ID 
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
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_ID 
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
            JOIN $TABLE_TRIPS t ON b.$KEY_TRIP_ID_BOOKING = t.$KEY_ID 
            WHERE b.$KEY_BOOKING_DATE >= date('now', '-30 days')
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        return if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            val total = cursor.getDouble(1)
            "Продано билетов: $count\nОбщая выручка: ${total.toInt()} руб."
        } else {
            "Нет продаж за месяц"
        }.also { cursor.close() }
    }

    // ========== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ==========
    fun debugCheckDatabase(): String {
        val db = readableDatabase
        val info = StringBuilder()

        val tables = arrayOf(TABLE_USERS, TABLE_TRIPS, TABLE_STOPS, TABLE_BOOKINGS)

        for (table in tables) {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $table", null)
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            info.append("$table: $count записей\n")
        }

        return info.toString()
    }

    fun forceRecreateDatabase() {
        val db = writableDatabase
        onUpgrade(db, 0, DATABASE_VERSION)
    }

    fun getRouteCoordinates(tripId: Int): List<Pair<Double, Double>> {
        val coordinates = mutableListOf<Pair<Double, Double>>()
        val stops = getStopsByTripId(tripId)

        // Для каждого маршрута определяем правильный порядок точек
        when (tripId) {
            1 -> { // Слободской → Киров
                val routeOrder = listOf(
                    "Автостанция Слободского",
                    "Рождественская улица",
                    "Улица Грина",
                    "ДЭП-4",
                    "Стулово",
                    "ПМК-14",
                    "Ситники",
                    "Первомайский поворот",
                    "Подсобное хозяйство",
                    "Вахруши",
                    "Школа (Вахруши)",
                    "Рубежница",
                    "Логуновы",
                    "Осинцы",
                    "Луза",
                    "Сады Биохимик-2",
                    "Зониха",
                    "Пантелеевы",
                    "Столбово",
                    "Шихово",
                    "Поворот на Бобино",
                    "Трушковы",
                    "Новомакарьевское кладбище",
                    "Порошинский поворот",
                    "Слобода Макарье",
                    "Троицкая церковь",
                    "Проезжая улица",
                    "Заповедная улица",
                    "Улица Красный Химик",
                    "Слобода Дымково",
                    "Профсоюзная улица",
                    "Улица МОПРа",
                    "Храм Иоанна Предтечи",
                    "Трифонов монастырь",
                    "Филармония",
                    "Областная больница",
                    "ЦУМ",
                    "Автовокзал Киров"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            2 -> { // Киров → Слободской
                val routeOrder = listOf(
                    "Автовокзал Киров",
                    "ЦУМ",
                    "Областная больница",
                    "Филармония",
                    "Трифонов монастырь",
                    "Храм Иоанна Предтечи",
                    "Улица МОПРа",
                    "Профсоюзная улица",
                    "Слобода Дымково",
                    "Улица Красный Химик",
                    "Заповедная улица",
                    "Проезжая улица",
                    "Троицкая церковь",
                    "Слобода Макарье",
                    "Порошинский поворот",
                    "Новомакарьевское кладбище",
                    "Трушковы",
                    "Поворот на Бобино",
                    "Шихово",
                    "Столбово",
                    "Пантелеевы",
                    "Зониха",
                    "Сады Биохимик-2",
                    "Луза",
                    "Осинцы",
                    "Логуновы",
                    "Рубежница",
                    "Школа (Вахруши)",
                    "Вахруши",
                    "Подсобное хозяйство",
                    "Первомайский поворот",
                    "Ситники",
                    "ПМК-14",
                    "Стулово",
                    "ДЭП-4",
                    "Улица Грина",
                    "Рождественская улица",
                    "Автостанция Слободского"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            3 -> { // Киров → Котельнич
                val routeOrder = listOf(
                    "Автовокзал Киров",
                    "Улица Дзержинского",
                    "Поворот на Гирсово",
                    "Поворот на Мурыгино",
                    "Горцы",
                    "Сады Урожай-1",
                    "Поворот на Юрью",
                    "Поворот на Медяны",
                    "Поворот на Малое Чураково",
                    "Лаптевы",
                    "Река Великая",
                    "Поворот на Цепели",
                    "Красногоры",
                    "Верхняя Боярщина",
                    "Зоновщина",
                    "Юркичи",
                    "Раменье",
                    "Боярщина",
                    "Колеватовы",
                    "Кузнецы-Орлов",
                    "Нижние Опарины",
                    "Щенники",
                    "Казаковцевы",
                    "Весниничи",
                    "Назаровы",
                    "Поворот на Криничи",
                    "Автостанция Орлов",
                    "Магазин Золотая марка",
                    "Детские ясли",
                    "Магазин Петушок",
                    "ТЦ Муравейник",
                    "Больница",
                    "Магазин Наш дом",
                    "Мебельная фабрика",
                    "Юбилейная улица",
                    "Высоково",
                    "Осинки",
                    "Балванская",
                    "Поворот на Юрьево",
                    "Скурихинская",
                    "Овчинниковы",
                    "Минины",
                    "Кардаковы",
                    "Фадеевцы / Липичи / Жохи",
                    "Хаустовы",
                    "Гулины",
                    "Поворот на Ленинскую Искру",
                    "Климичи",
                    "Пост ГИБДД",
                    "Автостанция Котельнич",
                    "Широченки",
                    "Шестаковы",
                    "Копылы",
                    "Ванюшенки",
                    "Вишкиль",
                    "Мамаи",
                    "Смирновы",
                    "Криуша",
                    "Горбуновщина",
                    "Сорвижи",
                    "Поворот на Кормино",
                    "Поворот на Шабры",
                    "Поворот на Шембеть",
                    "Поворот на Арбаж",
                    "Мосуны",
                    "Чернушка",
                    "Мостолыги",
                    "Лобасты",
                    "Автостанция Арбаж"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            4 -> { // Котельнич → Киров
                val routeOrder = listOf(
                    "Автостанция Арбаж",
                    "Лобасты",
                    "Мостолыги",
                    "Чернушка",
                    "Мосуны",
                    "Поворот на Арбаж",
                    "Поворот на Шембеть",
                    "Поворот на Шабры",
                    "Поворот на Кормино",
                    "Сорвижи",
                    "Горбуновщина",
                    "Криуша",
                    "Смирновы",
                    "Мамаи",
                    "Вишкиль",
                    "Ванюшенки",
                    "Копылы",
                    "Шестаковы",
                    "Широченки",
                    "Автостанция Котельнич",
                    "Пост ГИБДД",
                    "Климичи",
                    "Поворот на Ленинскую Искру",
                    "Гулины",
                    "Хаустовы",
                    "Фадеевцы / Липичи / Жохи",
                    "Кардаковы",
                    "Минины",
                    "Овчинниковы",
                    "Скурихинская",
                    "Поворот на Юрьево",
                    "Балванская",
                    "Осинки",
                    "Высоково",
                    "Юбилейная улица",
                    "Мебельная фабрика",
                    "Магазин Наш дом",
                    "Больница",
                    "ТЦ Муравейник",
                    "Магазин Петушок",
                    "Детские ясли",
                    "Магазин Золотая марка",
                    "Автостанция Орлов",
                    "Поворот на Криничи",
                    "Назаровы",
                    "Весниничи",
                    "Казаковцевы",
                    "Щенники",
                    "Нижние Опарины",
                    "Кузнецы-Орлов",
                    "Колеватовы",
                    "Боярщина",
                    "Раменье",
                    "Юркичи",
                    "Зоновщина",
                    "Верхняя Боярщина",
                    "Красногоры",
                    "Поворот на Цепели",
                    "Река Великая",
                    "Лаптевы",
                    "Поворот на Малое Чураково",
                    "Поворот на Медяны",
                    "Поворот на Юрью",
                    "Сады Урожай-1",
                    "Горцы",
                    "Поворот на Мурыгино",
                    "Поворот на Гирсово",
                    "Улица Дзержинского",
                    "Автовокзал Киров"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            5 -> { // Киров → Советск
                val routeOrder = listOf(
                    "Автовокзал Киров",
                    "Слобода Поскрёбышевы",
                    "Советский тракт / Сады Дружба",
                    "По требованию",
                    "Сады Рассвет",
                    "Дуркино / Пеньково",
                    "Чирки",
                    "Поворот на Пасегово",
                    "Сады Искож",
                    "Дряхловщина",
                    "Сады Импульс",
                    "Вахрёнки",
                    "Камешник",
                    "Козулинцы",
                    "Исуповская",
                    "Сады Исуповское",
                    "Быстрицкий тубсанаторий",
                    "Река Быстрица",
                    "Полом",
                    "Шабардёнки",
                    "Мочалище",
                    "Бонево",
                    "Большое Рогово",
                    "50-й километр",
                    "Поворот на Коршик",
                    "Кадесниково",
                    "Пунгино",
                    "Поворот на Скородум",
                    "Поворот на Вьюги",
                    "Поворот на Верхошижемье – 1",
                    "Кафе Корона",
                    "Автостанция Верхошижемье",
                    "Поворот на Верхошижемье",
                    "Москва",
                    "Поворот на Зониху",
                    "Рамеши",
                    "Поворот на Беляки",
                    "Поворот на Мякиши",
                    "Дуброва",
                    "Поворот на Кожу",
                    "Поворот на Зашижемье",
                    "Калугино",
                    "Поворот на Лесозавод",
                    "Поворот на Котельнич",
                    "Поворот на Жерновогорье",
                    "Мясокомбинат",
                    "Автоколонна",
                    "Улица Кондакова",
                    "Рынок",
                    "Почта",
                    "Перчаточная фабрика",
                    "Советск – Баня",
                    "Автостанция Советск"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            6 -> { // Советск → Киров
                val routeOrder = listOf(
                    "Советск – Баня",
                    "Перчаточная фабрика",
                    "Почта",
                    "Рынок",
                    "Улица Кондакова",
                    "Автоколонна",
                    "Мясокомбинат",
                    "Поворот на Жерновогорье",
                    "Поворот на Котельнич",
                    "Поворот на Лесозавод",
                    "Калугино",
                    "Поворот на Зашижемье",
                    "Поворот на Кожу",
                    "Дуброва",
                    "Поворот на Мякиши",
                    "Поворот на Беляки",
                    "Рамеши",
                    "Поворот на Зониху",
                    "Москва",
                    "Поворот на Верхошижемье",
                    "Автостанция Верхошижемье",
                    "Кафе Корона",
                    "Поворот на Верхошижемье – 1",
                    "Поворот на Вьюги",
                    "Поворот на Скородум",
                    "Пунгино",
                    "Кадесниково",
                    "Поворот на Коршик",
                    "50-й километр",
                    "Большое Рогово",
                    "Бонево",
                    "Мочалище",
                    "Шабардёнки",
                    "Полом",
                    "Река Быстрица",
                    "Быстрицкий тубсанаторий",
                    "Сады Исуповское",
                    "Исуповская",
                    "Козулинцы",
                    "Камешник",
                    "Вахрёнки",
                    "Сады Импульс",
                    "Дряхловщина",
                    "Сады Искож",
                    "Поворот на Пасегово",
                    "Чирки",
                    "Дуркино / Пеньково",
                    "Сады Рассвет",
                    "По требованию",
                    "Советский тракт / Сады Дружба",
                    "Слобода Поскрёбышевы",
                    "Автовокзал Киров"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            else -> {
                // По умолчанию берем координаты начала и конца
                val trip = getTripById(tripId)
                if (trip != null) {
                    getStopCoordinates(trip.fromCity)?.let { coordinates.add(it) }
                    getStopCoordinates(trip.toCity)?.let { coordinates.add(it) }
                }
            }
        }

        // Удаляем нулевые значения
        return coordinates.filterNotNull()
    }

    fun getStopCoordinatesSimplified(stopName: String, tripId: Int = -1): Pair<Double, Double>? {
        val simplifiedName = stopName
            .replace("\\(.*?\\)".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .lowercase()

        // Ищем в кэше по упрощенному имени
        val prefixedName = when (tripId) {
            1 -> "M1_$simplifiedName"
            2 -> "M2_$simplifiedName"
            3 -> "M3_$simplifiedName"
            4 -> "M4_$simplifiedName"
            5 -> "M5_$simplifiedName"
            6 -> "M6_$simplifiedName"
            else -> simplifiedName
        }

        return coordinatesCache[prefixedName] ?: getStopCoordinates(stopName, tripId)
    }

    // Метод для доступа к кэшу координат (для MapActivity)
    fun getCoordinatesCacheMap(): Map<String, Pair<Double, Double>> {
        // Инициализируем кэш если он пустой
        if (coordinatesCache.isEmpty()) {
            coordinatesCache.initializeAllCoordinates()
        }
        return coordinatesCache
    }

    // Вспомогательный метод для поиска по частичному совпадению
    fun findCoordinatesByPartialName(partialName: String, tripId: Int = -1): Pair<Double, Double>? {
        val searchName = partialName.lowercase().trim()

        // Формируем префикс для поиска
        val prefix = when (tripId) {
            1 -> "M1_"
            2 -> "M2_"
            3 -> "M3_"
            4 -> "M4_"
            5 -> "M5_"
            6 -> "M6_"
            else -> ""
        }

        // Сначала ищем с префиксом маршрута
        val fullKey = "$prefix$searchName"
        coordinatesCache[fullKey]?.let { return it }

        // Ищем по частичному совпадению
        for ((key, value) in coordinatesCache) {
            val cleanKey = key.replace("M[0-9]_", "").lowercase()
            if (cleanKey.contains(searchName) || searchName.contains(cleanKey)) {
                return value
            }
        }

        return null
    }
    fun getBookedSeatsForDate(tripId: Int, tripDate: String): List<Int> {
        val bookedSeats = mutableListOf<Int>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT $KEY_SEAT_NUMBER FROM $TABLE_BOOKINGS WHERE $KEY_TRIP_ID_BOOKING = ? AND $KEY_TRIP_DATE = ?",
            arrayOf(tripId.toString(), tripDate)
        )

        while (cursor.moveToNext()) {
            val seat = cursor.getInt(0)
            if (seat > 0) {
                bookedSeats.add(seat)
            }
        }
        cursor.close()
        return bookedSeats
    }


}
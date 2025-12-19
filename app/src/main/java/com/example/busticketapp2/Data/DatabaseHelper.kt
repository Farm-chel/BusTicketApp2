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
        private const val DATABASE_VERSION = 13

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

            // Рейсы
            val trip1Id = insertTrip(db, "Слободской", "Киров", "08:00", "09:00", 240.0)
            val trip2Id = insertTrip(db, "Киров", "Слободской", "14:00", "15:00", 240.0)
            val trip3Id = insertTrip(db, "Киров", "Котельнич", "09:30", "11:55", 600.0)
            val trip4Id = insertTrip(db, "Котельнич", "Киров", "16:00", "18:25", 600.0)
            val trip5Id = insertTrip(db, "Киров", "Вятские Поляны", "07:30", "14:30", 940.0)
            val trip6Id = insertTrip(db, "Вятские Поляны", "Киров", "06:00", "13:00", 940.0)
            val trip7Id = insertTrip(db, "Киров", "Советск", "08:30", "10:40", 450.0)
            val trip8Id = insertTrip(db, "Советск", "Киров", "11:40", "13:50", 450.0)

            Log.d("DB_DEBUG", "Trips inserted: trip1=$trip1Id, trip2=$trip2Id, trip3=$trip3Id, trip4=$trip4Id, trip5=$trip5Id, trip6=$trip6Id, trip7=$trip7Id, trip8=$trip8Id")

            // Вставляем ВСЕ остановки для каждого маршрута
            insertAllStopsForTrip1(db, trip1Id)
            insertAllStopsForTrip2(db, trip2Id)
            insertAllStopsForTrip3(db, trip3Id)
            insertAllStopsForTrip4(db, trip4Id)
            insertAllStopsForTrip5(db, trip5Id)
            insertAllStopsForTrip6(db, trip6Id)
            insertAllStopsForTrip7(db, trip7Id)
            insertAllStopsForTrip8(db, trip8Id)

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
            StopData("Вахруши", "08:18", "08:19", 51.3), // Убрали "По требованию"
            StopData("Школа (Вахруши)", "08:20", "08:21", 57.0), // Изменили название
            StopData("Рубежница", "08:22", "08:23", 62.7),
            StopData("Логуновы", "08:24", "08:25", 68.4),
            StopData("Осинцы", "08:28", "08:29", 79.8),
            StopData("Луза", "08:30", "08:31", 85.5), // Убрали "По требованию"
            StopData("Сады Биохимик-2", "08:34", "08:35", 96.9),
            StopData("Зониха", "08:36", "08:37", 102.6),
            StopData("Пантелеевы", "08:38", "08:39", 108.3),
            StopData("Столбово", "08:40", "08:41", 114.0),
            StopData("Шихово", "08:42", "08:43", 119.7),
            StopData("Поворот на Бобино", "08:44", "08:45", 125.4), // Изменили название
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
            StopData("Поворот на Бобино", "14:36", "14:37", 205.2), // Изменили название
            StopData("Шихово", "14:38", "14:39", 216.6),
            StopData("Столбово", "14:40", "14:41", 228.0),
            StopData("Пантелеевы", "14:42", "14:43", 239.4),
            StopData("Зониха", "14:44", "14:45", 250.8),
            StopData("Сады Биохимик-2", "14:46", "14:47", 262.2),
            StopData("Луза", "14:48", "14:49", 273.6),
            StopData("Осинцы", "14:52", "14:53", 296.4),
            StopData("Логуновы", "14:54", "14:55", 307.8),
            StopData("Рубежница", "14:56", "14:57", 319.2),
            StopData("Школа (Вахруши)", "15:00", "15:01", 342.0), // Изменили название
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
            StopData("Ванюшенки", "14:00", "14:01", 462.8), // Убрали "Борки"
            StopData("Вишкиль", "14:05", "14:06", 471.4),
            StopData("Мамаи", "14:10", "14:11", 480.0),
            StopData("Смирновы", "14:15", "14:16", 488.5),
            StopData("Криуша", "14:25", "14:26", 505.7),
            StopData("Горбуновщина", "14:30", "14:31", 514.2),
            StopData("Сорвижи", "14:35", "14:36", 522.8),
            StopData("Горбуновщина (обратный)", "14:40", "14:41", 531.4),
            StopData("Криуша (обратный)", "14:45", "14:46", 540.0),
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
            StopData("Криуша (обратный)", "16:45", "16:46", 77.1),
            StopData("Горбуновщина (обратный)", "16:50", "16:51", 85.7),
            StopData("Сорвижи", "16:55", "16:56", 94.2),
            StopData("Горбуновщина", "17:00", "17:01", 102.8),
            StopData("Криуша", "17:05", "17:06", 111.4),
            StopData("Смирновы", "17:15", "17:16", 128.5),
            StopData("Мамаи", "17:20", "17:21", 137.1),
            StopData("Вишкиль", "17:25", "17:26", 145.7),
            StopData("Копылы", "17:40", "17:41", 171.4), // Убрали "Борки" и "Ванюшенки"
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
            StopData("Автовокзал Киров", "07:30", "07:30", 0.0),
            StopData("Дом техники", "07:33", "07:34", 14.2),
            StopData("Поворот на Казань (выезд)", "07:42", "07:43", 35.5),
            StopData("Поворот на Швецово", "07:56", "07:57", 70.8),
            StopData("Поворот на Речной", "07:59", "08:00", 78.3),
            StopData("Олимпийский", "08:01", "08:02", 82.7),
            StopData("Поворот на Вожгалы", "08:12", "08:13", 112.4),
            StopData("Поворот на Кумёны", "08:19", "08:20", 128.9),
            StopData("Большевик", "08:38", "08:39", 187.5),
            StopData("Автостанция Суна", "08:40", "08:41", 192.0),
            StopData("Кырчаны – 2", "08:56", "08:57", 235.8),
            StopData("Кырчаны", "08:57", "08:58", 240.0),
            StopData("Поворот на Нему", "08:58", "08:59", 242.7),
            StopData("Перевоз", "09:04", "09:05", 265.4),
            StopData("Нолинск", "09:06", "09:07", 275.0),
            StopData("Поворот на Нолинск", "09:07", "09:08", 278.2),
            StopData("Поворот на Юртик", "09:12", "09:13", 295.8),
            StopData("Сомовщина", "09:15", "09:16", 307.1),
            StopData("Боровляна", "09:18", "09:19", 319.5),
            StopData("Поворот на Аркуль", "09:24", "09:25", 342.8),
            StopData("Поворот на Орешник", "09:31", "09:32", 366.2),
            StopData("Круглые Полянки", "09:32", "09:33", 369.5),
            StopData("Петровское – 2", "09:34", "09:35", 377.3),
            StopData("Петровское – 1", "09:35", "09:36", 380.0),
            StopData("Скрябино", "09:37", "09:38", 390.2),
            StopData("Щино", "09:38", "09:39", 395.0),
            StopData("Вершинята", "09:40", "09:41", 405.7),
            StopData("Марчата", "09:43", "09:44", 418.3),
            StopData("Поворот на Андреевский", "09:54", "09:55", 455.0),
            StopData("Теребиловка", "09:57", "09:58", 470.8),
            StopData("АТП", "09:58", "09:59", 474.5),
            StopData("Автостанция Уржум", "09:59", "10:00", 480.0),
            StopData("Магазин Продукты", "10:00", "10:01", 485.2),
            StopData("Школа № 3", "10:01", "10:02", 488.7),
            StopData("Гостиница", "10:02", "10:03", 492.5),
            StopData("Поворот на Шевнино", "10:12", "10:13", 525.8),
            StopData("Русский Турек", "10:15", "10:16", 540.0),
            StopData("Поворот на Русский Турек", "10:16", "10:17", 543.3),
            StopData("Кизерь", "10:18", "10:19", 553.7),
            StopData("Поворот на Шурму", "10:24", "10:25", 580.2),
            StopData("Верхняя Шурма", "10:25", "10:26", 585.0),
            StopData("Поворот на Лазарево", "10:27", "10:28", 595.8),
            StopData("Поворот на Тюм-Тюм", "10:29", "10:30", 610.5),
            StopData("Сосновка", "10:31", "10:32", 622.7),
            StopData("Большой Рой", "10:33", "10:34", 640.0),
            StopData("Танабаево", "10:34", "10:35", 648.3),
            StopData("Манкинерь", "10:41", "10:42", 680.2),
            StopData("Поворот на Аджим", "10:44", "10:45", 695.8),
            StopData("Новая Тушка", "10:46", "10:47", 715.0),
            StopData("Поворот на Рожки", "10:52", "10:53", 750.5),
            StopData("Поворот на Тат-Верх-Гоньбу", "10:55", "10:56", 768.3),
            StopData("Поворот на Новый Ирюк", "11:01", "11:02", 805.0),
            StopData("Поворот на Старый Ирюк", "11:02", "11:03", 815.2),
            StopData("Поворот на Савали", "11:05", "11:06", 838.7),
            StopData("Поворот на Малмыж", "11:07", "11:08", 855.0),
            StopData("Калинино", "11:08", "11:09", 858.3),
            StopData("Школа", "11:09", "11:10", 862.5),
            StopData("АЗС", "11:10", "11:11", 868.0),
            StopData("АТП", "11:10", "11:11", 872.2),
            StopData("Автостанция Малмыж", "11:11", "11:12", 877.5),
            StopData("Поворот на Казань (Малмыж)", "11:12", "11:13", 882.8),
            StopData("Новая Смаиль", "11:22", "11:23", 920.0),
            StopData("Поворот на Каменный Ключ", "11:23", "11:24", 925.8),
            StopData("Поречке Китяк", "11:24", "11:25", 930.5),
            StopData("Кошай", "11:26", "11:27", 945.2),
            StopData("Большой Китяк", "11:27", "11:28", 955.0),
            StopData("Малый Китяк", "11:28", "11:29", 965.8),
            StopData("Поворот на Янгулово", "11:29", "11:30", 975.5),
            StopData("Кошкино", "11:31", "11:32", 990.2),
            StopData("Поворот на Новый Бурец", "11:33", "11:34", 1010.0),
            StopData("Челны", "11:34", "11:35", 1020.8),
            StopData("Верхняя Тойма", "11:35", "11:36", 1032.5),
            StopData("Средняя Тойма – 2", "11:36", "11:37", 1040.2),
            StopData("Средняя Тойма – 1", "11:36", "11:37", 1045.0),
            StopData("Нижняя Тойма – 3", "11:37", "11:38", 1055.8),
            StopData("Нижняя Тойма – 2", "11:37", "11:38", 1062.5),
            StopData("Нижняя Тойма – 1", "11:38", "11:39", 1070.0),
            StopData("Кооперативная улица", "11:38", "11:39", 1075.8),
            StopData("Строительная улица", "11:39", "11:40", 1080.5),
            StopData("Переезд", "11:39", "11:40", 1085.2),
            StopData("Улица Тойменка", "11:40", "11:41", 1090.0),
            StopData("Налоговая инспекция", "11:40", "11:41", 1095.8),
            StopData("Машиностроительный завод", "11:41", "11:42", 1102.5),
            StopData("Школа искусств", "11:41", "11:42", 1108.3),
            StopData("ГИБДД", "11:42", "11:43", 1115.0),
            StopData("Автостанция Вятские Поляны", "14:30", "14:30", 940.0)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 5 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip6(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Автостанция Вятские Поляны", "06:00", "06:00", 0.0),
            StopData("ГИБДД", "06:01", "06:02", 14.2),
            StopData("Школа искусств", "06:02", "06:03", 28.4),
            StopData("Машиностроительный завод", "06:03", "06:04", 42.6),
            StopData("Налоговая инспекция", "06:04", "06:05", 56.8),
            StopData("Улица Тойменка", "06:05", "06:06", 71.0),
            StopData("Переезд", "06:06", "06:07", 85.2),
            StopData("Строительная улица", "06:07", "06:08", 99.4),
            StopData("Кооперативная улица", "06:08", "06:09", 113.6),
            StopData("Нижняя Тойма – 1", "06:09", "06:10", 127.8),
            StopData("Нижняя Тойма – 2", "06:10", "06:11", 142.0),
            StopData("Нижняя Тойма – 3", "06:11", "06:12", 156.2),
            StopData("Средняя Тойма – 1", "06:12", "06:13", 170.4),
            StopData("Средняя Тойма – 2", "06:13", "06:14", 184.6),
            StopData("Верхняя Тойма", "06:14", "06:15", 198.8),
            StopData("Челны", "06:15", "06:16", 213.0),
            StopData("Поворот на Новый Бурец", "06:16", "06:17", 227.2),
            StopData("Кошкино", "06:17", "06:18", 241.4),
            StopData("Поворот на Янгулово", "06:18", "06:19", 255.6),
            StopData("Малый Китяк", "06:19", "06:20", 269.8),
            StopData("Большой Китяк", "06:20", "06:21", 284.0),
            StopData("Кошай", "06:21", "06:22", 298.2),
            StopData("Поречке Китяк", "06:22", "06:23", 312.4),
            StopData("Поворот на Каменный Ключ", "06:23", "06:24", 326.6),
            StopData("Новая Смаиль", "06:24", "06:25", 340.8),
            StopData("Поворот на Казань (Малмыж)", "06:33", "06:34", 355.0),
            StopData("Автостанция Малмыж", "06:34", "06:35", 369.2),
            StopData("АТП", "06:35", "06:36", 383.4),
            StopData("АЗС", "06:36", "06:37", 397.6),
            StopData("Школа", "06:37", "06:38", 411.8),
            StopData("Калинино", "06:38", "06:39", 426.0),
            StopData("Поворот на Малмыж", "06:39", "06:40", 440.2),
            StopData("Поворот на Савали", "06:42", "06:43", 454.4),
            StopData("Поворот на Старый Ирюк", "06:44", "06:45", 468.6),
            StopData("Поворот на Новый Ирюк", "06:45", "06:46", 482.8),
            StopData("Поворот на Тат-Верх-Гоньбу", "06:51", "06:52", 497.0),
            StopData("Поворот на Рожки", "06:54", "06:55", 511.2),
            StopData("Новая Тушка", "07:00", "07:01", 525.4),
            StopData("Поворот на Аджим", "07:03", "07:04", 539.6),
            StopData("Манкинерь", "07:06", "07:07", 553.8),
            StopData("Танабаево", "07:13", "07:14", 568.0),
            StopData("Большой Рой", "07:15", "07:16", 582.2),
            StopData("Сосновка", "07:17", "07:18", 596.4),
            StopData("Поворот на Тюм-Тюм", "07:19", "07:20", 610.6),
            StopData("Поворот на Лазарево", "07:22", "07:23", 624.8),
            StopData("Верхняя Шурма", "07:24", "07:25", 639.0),
            StopData("Поворот на Шурму", "07:25", "07:26", 653.2),
            StopData("Кизерь", "07:29", "07:30", 667.4),
            StopData("Поворот на Русский Турек", "07:30", "07:31", 681.6),
            StopData("Русский Турек", "07:31", "07:32", 695.8),
            StopData("Поворот на Шевнино", "07:34", "07:35", 710.0),
            StopData("Гостиница", "07:43", "07:44", 724.2),
            StopData("Школа № 3", "07:44", "07:45", 738.4),
            StopData("Магазин Продукты", "07:45", "07:46", 752.6),
            StopData("Автостанция Уржум", "07:46", "07:47", 766.8),
            StopData("АТП", "07:47", "07:48", 781.0),
            StopData("Теребиловка", "07:48", "07:49", 795.2),
            StopData("Поворот на Андреевский", "07:51", "07:52", 809.4),
            StopData("Марчата", "08:01", "08:02", 823.6),
            StopData("Вершинята", "08:04", "08:05", 837.8),
            StopData("Щино", "08:05", "08:06", 852.0),
            StopData("Скрябино", "08:06", "08:07", 866.2),
            StopData("Петровское – 1", "08:07", "08:08", 880.4),
            StopData("Петровское – 2", "08:08", "08:09", 894.6),
            StopData("Круглые Полянки", "08:09", "08:10", 908.8),
            StopData("Поворот на Орешник", "08:10", "08:11", 923.0),
            StopData("Поворот на Аркуль", "08:17", "08:18", 937.2),
            StopData("Боровляна", "08:24", "08:25", 940.0),
            StopData("Сомовщина", "08:26", "08:27", 945.0),
            StopData("Поворот на Юртик", "08:28", "08:29", 950.0),
            StopData("Поворот на Нолинск", "08:30", "08:31", 955.0),
            StopData("Нолинск", "08:32", "08:33", 960.0),
            StopData("Перевоз", "08:34", "08:35", 965.0),
            StopData("Поворот на Нему", "08:36", "08:37", 970.0),
            StopData("Кырчаны", "08:38", "08:39", 975.0),
            StopData("Кырчаны – 2", "08:39", "08:40", 980.0),
            StopData("Автостанция Суна", "08:41", "08:42", 985.0),
            StopData("Большевик", "08:43", "08:44", 990.0),
            StopData("Поворот на Кумёны", "08:45", "08:46", 995.0),
            StopData("Поворот на Вожгалы", "08:47", "08:48", 1000.0),
            StopData("Олимпийский", "08:49", "08:50", 1005.0),
            StopData("Поворот на Речной", "08:51", "08:52", 1010.0),
            StopData("Поворот на Швецово", "08:53", "08:54", 1015.0),
            StopData("Поворот на Казань (выезд)", "08:55", "08:56", 1020.0),
            StopData("Дом техники", "08:57", "08:58", 1025.0),
            StopData("Автовокзал Киров", "13:00", "13:00", 1035.0)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 6 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip7(db: SQLiteDatabase, tripId: Long) {
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
            StopData("Советск – Баня", "10:57", "10:58", 898.5),
            StopData("Автостанция Советск", "10:40", "10:40", 450.0)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 7 inserted: ${stops.size}")
    }

    private fun insertAllStopsForTrip8(db: SQLiteDatabase, tripId: Long) {
        val stops = listOf(
            StopData("Советск – Баня", "11:40", "11:41", 0.0),
            StopData("Перчаточная фабрика", "11:42", "11:43", 10.7),
            StopData("Почта", "11:43", "11:44", 21.4),
            StopData("Рынок", "11:44", "11:45", 32.1),
            StopData("Улица Кондакова", "11:45", "11:46", 42.8),
            StopData("Автоколонна", "11:46", "11:47", 53.5),
            StopData("Мясокомбинат", "11:47", "11:48", 64.2),
            StopData("Поворот на Жерновогорье", "11:48", "11:49", 74.9),
            StopData("Поворот на Котельнич", "11:49", "11:50", 85.6),
            StopData("Поворот на Лесозавод", "11:50", "11:51", 96.3),
            StopData("Калугино", "12:02", "12:03", 117.7),
            StopData("Поворот на Зашижемье", "12:09", "12:10", 128.4),
            StopData("Поворот на Кожу", "12:15", "12:16", 139.1),
            StopData("Дуброва", "12:18", "12:19", 149.8),
            StopData("Поворот на Мякиши", "12:21", "12:22", 160.5),
            StopData("Поворот на Беляки", "12:22", "12:23", 171.2),
            StopData("Рамеши", "12:23", "12:24", 181.9),
            StopData("Поворот на Зониху", "12:24", "12:25", 192.6),
            StopData("Москва", "12:33", "12:34", 214.0),
            StopData("Поворот на Верхошижемье", "12:35", "12:36", 224.7),
            StopData("Автостанция Верхошижемье", "12:37", "12:38", 235.4),
            StopData("Кафе Корона", "12:38", "12:39", 246.1),
            StopData("Поворот на Верхошижемье – 1", "12:39", "12:40", 256.8),
            StopData("Поворот на Вьюги", "12:42", "12:43", 267.5),
            StopData("Поворот на Скородум", "12:48", "12:49", 278.2),
            StopData("Пунгино", "12:53", "12:54", 288.9),
            StopData("Кадесниково", "12:55", "12:56", 299.6),
            StopData("Поворот на Коршик", "13:07", "13:08", 310.3),
            StopData("50-й километр", "13:12", "13:13", 321.0),
            StopData("Большое Рогово", "13:14", "13:15", 331.7),
            StopData("Бонево", "13:16", "13:17", 342.4),
            StopData("Мочалище", "13:20", "13:21", 353.1),
            StopData("Шабардёнки", "13:25", "13:26", 363.8),
            StopData("Полом", "13:35", "13:36", 374.5),
            StopData("Река Быстрица", "13:41", "13:42", 385.2),
            StopData("Быстрицкий тубсанаторий", "13:47", "13:48", 395.9),
            StopData("Сады Исуповское", "13:53", "13:54", 406.6),
            StopData("Исуповская", "13:54", "13:55", 417.3),
            StopData("Козулинцы", "13:55", "13:56", 428.0),
            StopData("Камешник", "13:56", "13:57", 438.7),
            StopData("Вахрёнки", "13:57", "13:58", 449.4),
            StopData("Сады Импульс", "13:58", "13:59", 460.1)
        )

        stops.forEach { stop ->
            insertStop(db, tripId, stop.name, stop.arrivalTime, stop.departureTime, stop.price)
        }
        Log.d("DB_DEBUG", "All stops for trip 8 inserted: ${stops.size}")
    }

    // ========== КООРДИНАТЫ ВСЕХ ОСТАНОВОК ==========
    // ========== КООРДИНАТЫ ВСЕХ ОСТАНОВОК ==========
    private fun MutableMap<String, Pair<Double, Double>>.initializeAllCoordinates() {
        clear()

        // ========== МАРШРУТ СЛОБОДСКОЙ → КИРОВ ==========
        put("Автостанция Слободского", Pair(58.721262, 50.181554))
        put("Рождественская улица", Pair(58.732264, 50.180422))
        put("Улица Грина", Pair(58.724187, 50.176146))
        put("ДЭП-4", Pair(58.723925, 50.159868))
        put("Стулово", Pair(58.722281, 50.150535))
        put("ПМК-14", Pair(58.720846, 50.143582))
        put("Ситники", Pair(58.716099, 50.128014))
        put("Первомайский поворот", Pair(58.711846, 50.113291))
        put("Подсобное хозяйство", Pair(58.699579, 50.088848))
        put("Вахруши", Pair(58.682865, 50.032532))
        put("Школа (Вахруши)", Pair(58.690639, 50.043339))
        put("Рубежница", Pair(58.677420, 50.019075))
        put("Логуновы", Pair(58.677369, 50.002528))
        put("Осинцы", Pair(58.676770, 49.968033))
        put("Луза", Pair(58.662801, 49.926055))
        put("Сады Биохимик-2", Pair(58.657788, 49.912724))
        put("Зониха", Pair(58.651356, 49.881543))
        put("Пантелеевы", Pair(58.648256, 49.865724))
        put("Столбово", Pair(58.642150, 49.852492))
        put("Шихово", Pair(58.634881, 49.831301))
        put("Поворот на Бобино", Pair(58.630515, 49.813792))
        put("Трушковы", Pair(58.627756, 49.804827))
        put("Новомакарьевское кладбище", Pair(58.615108, 49.756363))
        put("Порошинский поворот", Pair(58.615108, 49.756363))
        put("Слобода Макарье", Pair(58.613060, 49.750308))
        put("Троицкая церковь", Pair(58.618951, 49.719748))
        put("Проезжая улица", Pair(58.618951, 49.719748))
        put("Заповедная улица", Pair(58.618951, 49.719748))
        put("Улица Красный Химик", Pair(58.609774, 49.686753))
        put("Слобода Дымково", Pair(58.590357, 49.682018))
        put("Профсоюзная улица", Pair(58.606530, 49.681039))
        put("Улица МОПРа", Pair(58.590357, 49.682018))
        put("Храм Иоанна Предтечи", Pair(58.594686, 49.682450))
        put("Трифонов монастырь", Pair(58.593481, 49.659156))
        put("Филармония", Pair(58.593326, 49.633375))
        put("Областная больница", Pair(58.589119, 49.655913))
        put("ЦУМ", Pair(58.589119, 49.655913))
        put("Автовокзал Киров", Pair(58.583651, 49.650495))

        // ========== МАРШРУТ КИРОВ → ВЯТСКИЕ ПОЛЯНЫ ==========
        put("Дом техники", Pair(58.575806, 49.609369))
        put("Поворот на Казань (выезд)", Pair(58.463926, 49.758094))
        put("Поворот на Швецово", Pair(58.297162, 49.838450))
        put("Поворот на Речной", Pair(58.281636, 49.832585))
        put("Олимпийский", Pair(58.269808, 49.834779))
        put("Поворот на Вожгалы", Pair(58.160592, 49.900843))
        put("Поворот на Кумёны", Pair(58.115595, 49.943645))
        put("Большевик", Pair(57.845628, 50.065469))
        put("Автостанция Суна", Pair(57.836372, 50.074738))
        put("Кырчаны – 2", Pair(57.655728, 50.141969))
        put("Кырчаны", Pair(57.641379, 50.155822))
        put("Поворот на Нему", Pair(57.634801, 50.154633))
        put("Перевоз", Pair(57.553290, 50.005488))
        put("Нолинск", Pair(57.562190, 49.950472))
        put("Поворот на Нолинск", Pair(57.553822, 49.996869))
        put("Поворот на Юртик", Pair(57.503736, 49.992507))
        put("Сомовщина", Pair(57.485709, 49.966450))
        put("Боровляна", Pair(57.463135, 49.973681))
        put("Поворот на Аркуль", Pair(57.407513, 49.989722))
        put("Поворот на Орешник", Pair(57.351743, 49.958572))
        put("Круглые Полянки", Pair(57.345733, 49.974349))
        put("Петровское – 2", Pair(57.327588, 49.975809))
        put("Петровское – 1", Pair(57.320576, 49.971100))
        put("Скрябино", Pair(57.299727, 49.944683))
        put("Щино", Pair(57.287936, 49.937975))
        put("Вершинята", Pair(57.262993, 49.909340))
        put("Марчата", Pair(57.238042, 49.909359))
        put("Поворот на Андреевский", Pair(57.161171, 49.945024))
        put("Теребиловка", Pair(57.129456, 49.986712))
        put("АТП", Pair(57.125400, 49.989344))
        put("Автостанция Уржум", Pair(57.120178, 49.994436))
        put("Магазин Продукты", Pair(57.111008, 49.996222)) // Заменили Рынок
        put("Школа № 3", Pair(57.109906, 50.000716))
        put("Гостиница", Pair(57.109107, 50.005966))
        put("Поворот на Шевнино", Pair(57.076486, 50.128996))
        put("Русский Турек", Pair(57.054289, 50.203817))
        put("Поворот на Русский Турек", Pair(57.045756, 50.209092))
        put("Кизерь", Pair(57.025860, 50.221428))
        put("Поворот на Шурму", Pair(56.957883, 50.287130))
        put("Верхняя Шурма", Pair(56.946377, 50.303990))
        put("Поворот на Лазарево", Pair(56.933834, 50.331365))
        put("Поворот на Тюм-Тюм", Pair(56.919675, 50.372504))
        put("Сосновка", Pair(56.897599, 50.370822))
        put("Большой Рой", Pair(56.863940, 50.378323))
        put("Танабаево", Pair(56.852509, 50.365744))
        put("Манкинерь", Pair(56.788733, 50.378877))
        put("Поворот на Аджим", Pair(56.761218, 50.376236))
        put("Новая Тушка", Pair(56.725099, 50.418170))
        put("Поворот на Рожки", Pair(56.667159, 50.479090))
        put("Поворот на Тат-Верх-Гоньбу", Pair(56.640344, 50.491240))
        put("Поворот на Новый Ирюк", Pair(56.591441, 50.563585))
        put("Поворот на Старый Ирюк", Pair(56.573971, 50.588957))
        put("Поворот на Савали", Pair(56.531762, 50.617888))
        put("Поворот на Малмыж", Pair(56.505410, 50.612346))
        put("Калинино", Pair(56.507354, 50.634399))
        put("Школа", Pair(56.509399, 50.644127))
        put("АЗС", Pair(56.512817, 50.655083))
        put("АТП", Pair(56.514813, 50.662254))
        put("Автостанция Малмыж", Pair(56.517984, 50.670337))
        put("Поворот на Казань (Малмыж)", Pair(56.499185, 50.610039))
        put("Новая Смаиль", Pair(56.404785, 50.650229))
        put("Поворот на Каменный Ключ", Pair(56.396841, 50.663561))
        put("Поречке Китяк", Pair(56.390442, 50.673918))
        put("Кошай", Pair(56.364952, 50.701183))
        put("Большой Китяк", Pair(56.358097, 50.730394))
        put("Малый Китяк", Pair(56.349727, 50.761442))
        put("Поворот на Янгулово", Pair(56.345355, 50.789585))
        put("Кошкино", Pair(56.334152, 50.821204))
        put("Поворот на Новый Бурец", Pair(56.300531, 50.874497))
        put("Челны", Pair(56.275489, 50.892594))
        put("Верхняя Тойма", Pair(56.256194, 50.917709))
        put("Средняя Тойма – 2", Pair(56.249596, 50.945654))
        put("Средняя Тойма – 1", Pair(56.247508, 50.956578))
        put("Нижняя Тойма – 3", Pair(56.239238, 50.986576))
        put("Нижняя Тойма – 2", Pair(56.240749, 51.002469))
        put("Нижняя Тойма – 1", Pair(56.241479, 51.022741))
        put("Кооперативная улица", Pair(56.239560, 51.035595))
        put("Строительная улица", Pair(56.239072, 51.040528))
        put("Переезд", Pair(56.238048, 51.044718))
        put("Улица Тойменка", Pair(56.236058, 51.049961))
        put("Налоговая инспекция", Pair(56.234762, 51.061003))
        put("Машиностроительный завод", Pair(56.231909, 51.067676))
        put("Школа искусств", Pair(56.232174, 51.076136))
        put("ГИБДД", Pair(56.227870, 51.079839))
        put("Автостанция Вятские Поляны", Pair(56.224749, 51.079241))

        // ========== МАРШРУТ КИРОВ → СОВЕТСК ==========
        put("Слобода Поскрёбышевы", Pair(58.582198, 49.651007))
        put("Советский тракт / Сады Дружба", Pair(58.556697, 49.617762))
        put("По требованию", Pair(58.552340, 49.609491))
        put("Сады Рассвет", Pair(58.550355, 49.605025))
        put("Дуркино / Пеньково", Pair(58.541718, 49.591076))
        put("Чирки", Pair(58.532577, 49.583014))
        put("Поворот на Пасегово", Pair(58.522409, 49.582045))
        put("Сады Искож", Pair(58.510226, 49.580838))
        put("Дряхловщина", Pair(58.502427, 49.577070))
        put("Сады Импульс", Pair(58.491975, 49.577957))
        put("Вахрёнки", Pair(58.481576, 49.582436))
        put("Камешник", Pair(58.468829, 49.569956))
        put("Козулинцы", Pair(58.463111, 49.564291))
        put("Исуповская", Pair(58.449941, 49.554490))
        put("Сады Исуповское", Pair(58.434498, 49.546991))
        put("Быстрицкий тубсанаторий", Pair(58.429690, 49.545715))
        put("Река Быстрица", Pair(58.387618, 49.506806))
        put("Полом", Pair(58.377149, 49.489401))
        put("Шабардёнки", Pair(58.350709, 49.461301))
        put("Мочалище", Pair(58.318014, 49.391780))
        put("Бонево", Pair(58.284031, 49.369237))
        put("Большое Рогово", Pair(58.257448, 49.362716))
        put("50-й километр", Pair(58.245484, 49.351817))
        put("Поворот на Коршик", Pair(58.225738, 49.326817))
        put("Кадесниково", Pair(58.200094, 49.295026))
        put("Пунгино", Pair(58.146925, 49.177334))
        put("Поворот на Скородум", Pair(58.131892, 49.137154))
        put("Поворот на Вьюги", Pair(58.092797, 49.103550))
        put("Поворот на Верхошижемье – 1", Pair(58.051555, 49.084740))
        put("Кафе Корона", Pair(58.030566, 49.108676))
        put("Автостанция Верхошижемье", Pair(58.018583, 49.125480))
        put("Поворот на Верхошижемье", Pair(58.007893, 49.106358))
        put("Москва", Pair(57.988572, 49.121569))
        put("Поворот на Зониху", Pair(57.967823, 49.104849))
        put("Рамеши", Pair(57.911039, 49.110130))
        put("Поворот на Беляки", Pair(57.893767, 49.087572))
        put("Поворот на Мякиши", Pair(57.869751, 49.067881))
        put("Дуброва", Pair(57.847554, 49.015464))
        put("Поворот на Кожу", Pair(57.826179, 48.967924))
        put("Поворот на Зашижемье", Pair(57.793238, 48.930288))
        put("Калугино", Pair(57.751027, 48.887750))
        put("Поворот на Лесозавод", Pair(57.709416, 48.887900))
        put("Поворот на Котельнич", Pair(57.628964, 48.924445))
        put("Поворот на Жерновогорье", Pair(57.604934, 48.920228))
        put("Мясокомбинат", Pair(57.596660, 48.927875))
        put("Автоколонна", Pair(57.582615, 48.941465))
        put("Улица Кондакова", Pair(57.580973, 48.950117))
        put("Рынок", Pair(57.583399, 48.953667))
        put("Почта", Pair(57.586212, 48.957449))
        put("Перчаточная фабрика", Pair(57.589558, 48.961872))
        put("Советск – Баня", Pair(57.592622, 48.966065))
        put("Автостанция Советск", Pair(57.592981, 48.969190))

        // ========== МАРШРУТЫ КИРОВ-КОТЕЛЬНИЧ ==========
        put("Улица Дзержинского", Pair(58.633361, 49.617675))
        put("Поворот на Гирсово", Pair(58.737164, 49.552364))
        put("Поворот на Мурыгино", Pair(58.747287, 49.531892))
        put("Горцы", Pair(58.759871, 49.512871))
        put("Сады Урожай-1", Pair(58.770222, 49.482504))
        put("Поворот на Юрью", Pair(58.772118, 49.473849))
        put("Поворот на Медяны", Pair(58.771935, 49.384934))
        put("Поворот на Малое Чураково", Pair(58.755541, 49.315456))
        put("Лаптевы", Pair(58.745547, 49.287798))
        put("Река Великая", Pair(58.737099, 49.250816))
        put("Поворот на Цепели", Pair(58.722459, 49.221294))
        put("Красногоры", Pair(58.715344, 49.210115))
        put("Верхняя Боярщина", Pair(58.707308, 49.192200))
        put("Зоновщина", Pair(58.702078, 49.169786))
        put("Юркичи", Pair(58.692492, 49.129097))
        put("Раменье", Pair(58.685231, 49.106490))
        put("Боярщина", Pair(58.675677, 49.069284))
        put("Колеватовы", Pair(58.670739, 49.056520))
        put("Кузнецы-Орлов", Pair(58.658588, 49.029802))
        put("Нижние Опарины", Pair(58.647280, 49.006306))
        put("Щенники", Pair(58.630891, 48.987304))
        put("Казаковцевы", Pair(58.612166, 48.975044))
        put("Весниничи", Pair(58.597590, 48.964840))
        put("Назаровы", Pair(58.574886, 48.939167))
        put("Поворот на Криничи", Pair(58.560212, 48.918384))
        put("Автостанция Орлов", Pair(58.548402, 48.898684))
        put("Магазин Золотая марка", Pair(58.542541, 48.903440))
        put("Детские ясли", Pair(58.540691, 48.901498))
        put("Магазин Петушок", Pair(58.536763, 48.895470))
        put("ТЦ Муравейник", Pair(58.534106, 48.891417))
        put("Больница", Pair(58.531279, 48.886428))
        put("Магазин Наш дом", Pair(58.533788, 48.880280))
        put("Мебельная фабрика", Pair(58.532313, 48.875484))
        put("Юбилейная улица", Pair(58.531889, 48.870343))
        put("Высоково", Pair(58.543928, 48.753756))
        put("Осинки", Pair(58.489731, 48.587237))
        put("Балванская", Pair(58.480585, 48.572046))
        put("Поворот на Юрьево", Pair(58.452485, 48.531782))
        put("Скурихинская", Pair(58.438628, 48.516946))
        put("Овчинниковы", Pair(58.419006, 48.483533))
        put("Минины", Pair(58.408897, 48.474876))
        put("Кардаковы", Pair(58.400170, 48.467495))
        put("Фадеевцы / Липичи / Жохи", Pair(58.376832, 48.453930))
        put("Хаустовы", Pair(58.359602, 48.437854))
        put("Гулины", Pair(58.348800, 48.428992))
        put("Поворот на Ленинскую Искру", Pair(58.334175, 48.417026))
        put("Климичи", Pair(58.324498, 48.403480))
        put("Пост ГИБДД", Pair(58.318261, 48.396782))
        put("Автостанция Котельнич", Pair(58.312207, 48.341900))

        // ========== ОСТАНОВКИ В ГОРОДАХ ДЛЯ ОБРАТНЫХ МАРШРУТОВ ==========
        put("Автостанция Арбаж", Pair(57.680673, 48.307524))
        put("Лобасты", Pair(57.690000, 48.290700))
        put("Мостолыги", Pair(57.712196, 48.265604))
        put("Чернушка", Pair(57.743750, 48.268683))
        put("Мосуны", Pair(57.763075, 48.274191))
        put("Поворот на Арбаж", Pair(57.791532, 48.269725))
        put("Поворот на Шембеть", Pair(57.810725, 48.283248))
        put("Поворот на Шабры", Pair(57.845882, 48.312336))
        put("Поворот на Кормино", Pair(57.887566, 48.355890))
        put("Криуша (обратный)", Pair(57.908502, 48.412161))
        put("Горбуновщина (обратный)", Pair(57.886925, 48.447575))
        put("Сорвижи", Pair(57.864274, 48.534764))
        put("Криуша", Pair(57.908502, 48.412161))
        put("Смирновы", Pair(57.985803, 48.296416))
        put("Мамаи", Pair(58.004120, 48.280065))
        put("Вишкиль", Pair(58.092038, 48.318224))
        put("Копылы", Pair(58.212727, 48.302973))
        put("Шестаковы", Pair(58.247822, 48.306982))
        put("Широченки", Pair(58.260866, 48.306513))

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

        // 1. Сначала ищем точное совпадение
        coordinatesCache[cleanName]?.let {
            Log.d("DBHelper", "✓ Прямое совпадение: '$cleanName'")
            return it
        }

        // 2. Ищем с учетом маршрута
        if (tripId > 0) {
            // Для каждого маршрута определяем свои ключи поиска
            when (tripId) {
                1, 2 -> { // Маршруты Слободской-Киров
                    val slobodskoiKeys = listOf(
                        "Автостанция Слободского", "Рождественская улица", "Улица Грина",
                        "ДЭП-4", "Стулово", "ПМК-14", "Ситники", "Первомайский поворот",
                        "Подсобное хозяйство", "Вахруши", "Рубежница", "Логуновы",
                        "Осинцы", "Луза", "Зониха", "Пантелеевы", "Столбово", "Шихово",
                        "Трушковы", "Бобинский поворот", "Слобода Макарье", "Троицкая церковь",
                        "Проезжая улица", "Заповедная улица", "Слобода Дымково", "Профсоюзная улица",
                        "Улица МОПРа", "Храм Иоанна Предтечи", "Трифонов монастырь", "Филармония",
                        "Областная больница", "ЦУМ", "Автовокзал Киров"
                    )

                    for (key in slobodskoiKeys) {
                        if (cleanName.contains(key) || key.contains(cleanName)) {
                            coordinatesCache[key]?.let {
                                Log.d("DBHelper", "✓ Найдено в маршруте $tripId: '$cleanName' -> '$key'")
                                return it
                            }
                        }
                    }
                }
                3, 4 -> { // Маршруты Киров-Котельнич
                    val kirovKotelnicKeys = listOf(
                        "Улица Дзержинского", "Поворот на Гирсово", "Поворот на Мурыгино",
                        "Горцы", "Поворот на Медяны", "Лаптевы", "Река Великая", "Красногоры",
                        "Зоновщина", "Раменье", "Колеватовы", "Нижние Опарины", "Весниничи",
                        "Автостанция Орлов", "ТЦ Муравейник", "Высоково", "Овчинниковы",
                        "Кардаковы", "Гулины", "Климичи", "Автостанция Котельнич", "Шестаковы",
                        "Борки", "Вишкиль", "Мамаи", "Боровка", "Криуша", "Сорвижи",
                        "Чернушка", "Автостанция Арбаж"
                    )

                    for (key in kirovKotelnicKeys) {
                        if (cleanName.contains(key) || key.contains(cleanName)) {
                            coordinatesCache[key]?.let {
                                Log.d("DBHelper", "✓ Найдено в маршруте $tripId: '$cleanName' -> '$key'")
                                return it
                            }
                        }
                    }
                }
                5, 6 -> { // Маршруты Киров-Вятские Поляны
                    val kirovVyatskiePolyanyKeys = listOf(
                        "Дом техники", "Поворот на Казань", "Поворот на Швецово",
                        "Поворот на Речной", "Олимпийский", "Поворот на Вожгалы",
                        "Поворот на Кумёны", "Большевик", "Автостанция Суна",
                        "Кырчаны", "Нолинск", "Поворот на Юртик", "Боровляна",
                        "Петровское", "Скрябино", "Вершинята", "Марчата",
                        "Автостанция Уржум", "Рынок", "Гостиница", "Русский Турек",
                        "Кизерь", "Верхняя Шурма", "Сосновка", "Большой Рой",
                        "Танабаево", "Манкинерь", "Новая Тушка", "Автостанция Малмыж",
                        "Кошай", "Большой Китяк", "Кошкино", "Челны", "Верхняя Тойма",
                        "Средняя Тойма", "Нижняя Тойма", "Налоговая инспекция",
                        "Машиностроительный завод", "Школа искусств", "ГИБДД",
                        "Автостанция Вятские Поляны"
                    )

                    for (key in kirovVyatskiePolyanyKeys) {
                        if (cleanName.contains(key) || key.contains(cleanName)) {
                            coordinatesCache[key]?.let {
                                Log.d("DBHelper", "✓ Найдено в маршруте $tripId: '$cleanName' -> '$key'")
                                return it
                            }
                        }
                    }
                }
                7, 8 -> { // Маршруты Киров-Советск
                    val kirovSovetskKeys = listOf(
                        "Слобода Поскрёбышевы", "Советский тракт", "Сады Рассвет",
                        "Дуркино", "Чирки", "Поворот на Пасегово", "Сады Искож",
                        "Сады Импульс", "Вахрёнки", "Камешник", "Козулинцы",
                        "Исуповская", "Сады Исуповское", "Быстрицкий тубсанаторий",
                        "Река Быстрица", "Полом", "Шабардёнки", "Мочалище",
                        "Бонево", "Большое Рогово", "50-й километр", "Поворот на Коршик",
                        "Кадесниково", "Пунгино", "Поворот на Скородум", "Поворот на Вьюги",
                        "Кафе Корона", "Автостанция Верхошижемье", "Москва",
                        "Поворот на Зониху", "Рамеши", "Поворот на Беляки",
                        "Высоково", "Дуброва", "Поворот на Кожу", "Поворот на Зашижемье",
                        "Калугино", "Поворот на Лесозавод", "Мясокомбинат",
                        "Автоколонна", "Улица Кондакова", "Рынок", "Почта",
                        "Перчаточная фабрика", "Советск – Баня", "Автостанция Советск"
                    )

                    for (key in kirovSovetskKeys) {
                        if (cleanName.contains(key) || key.contains(cleanName)) {
                            coordinatesCache[key]?.let {
                                Log.d("DBHelper", "✓ Найдено в маршруте $tripId: '$cleanName' -> '$key'")
                                return it
                            }
                        }
                    }
                }
            }
        }

        // 3. Если не нашли по маршруту, ищем в общем кэше (менее строгий поиск)
        for ((key, value) in coordinatesCache) {
            val cleanKey = key.replace("\\(.*?\\)".toRegex(), "").trim()

            // Проверяем, является ли одна строка частью другой
            if (cleanName.contains(cleanKey) || cleanKey.contains(cleanName)) {
                // Дополнительная проверка: если это остановка с "поворот",
                // уточняем маршрут
                if (cleanName.contains("поворот", ignoreCase = true) &&
                    !cleanKey.contains("поворот", ignoreCase = true)) {
                    continue
                }

                Log.d("DBHelper", "✓ Общее совпадение: '$cleanName' -> '$key'")
                return value
            }
        }

        // 4. Возвращаем координаты по умолчанию в зависимости от города
        val defaultCoords = when {
            cleanName.contains("киров", ignoreCase = true) -> Pair(58.583651, 49.650495)
            cleanName.contains("слободской", ignoreCase = true) -> Pair(58.721262, 50.181554)
            cleanName.contains("котельнич", ignoreCase = true) -> Pair(58.312207, 48.341900)
            cleanName.contains("вятские поляны", ignoreCase = true) -> Pair(56.224749, 51.079241)
            cleanName.contains("советск", ignoreCase = true) -> Pair(57.592981, 48.969190)
            cleanName.contains("арбаж", ignoreCase = true) -> Pair(57.680673, 48.307524)
            cleanName.contains("орлов", ignoreCase = true) -> Pair(58.548402, 48.898684)
            cleanName.contains("верхозижемье", ignoreCase = true) -> Pair(58.007893, 49.106358)
            cleanName.contains("уржум", ignoreCase = true) -> Pair(57.120178, 49.994436)
            cleanName.contains("малмыж", ignoreCase = true) -> Pair(56.517984, 50.670337)
            cleanName.contains("суна", ignoreCase = true) -> Pair(57.836372, 50.074738)
            cleanName.contains("нолинск", ignoreCase = true) -> Pair(57.562190, 49.950472)
            else -> null
        }

        if (defaultCoords != null) {
            Log.d("DBHelper", "⚠ Возвращаю координаты по городу: '$cleanName' -> ${defaultCoords.first}, ${defaultCoords.second}")
            return defaultCoords
        }

        Log.d("DBHelper", "❌ Не найдены координаты для: '$cleanName' (маршрут $tripId)")
        return Pair(58.600000, 49.600000) // Координаты Кирова по умолчанию
    }

    // ========== ДОБАВЛЕННЫЕ МЕТОДЫ (из сообщения об ошибках) ==========

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
            ORDER BY b.$KEY_BOOKING_DATE DESC
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
                   passengerEmail: String, seatNumber: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID_FK, userId)
            put(KEY_TRIP_ID_BOOKING, tripId)
            put(KEY_PASSENGER_NAME, passengerName)
            put(KEY_PASSENGER_EMAIL, passengerEmail)
            put(KEY_SEAT_NUMBER, seatNumber)
        }
        return db.insert(TABLE_BOOKINGS, null, values)
    }

    fun addBookingForUser(userId: Int, tripId: Int, passengerName: String,
                          passengerEmail: String, seatNumber: Int): Long {
        return addBooking(userId, tripId, passengerName, passengerEmail, seatNumber)
    }

    fun getBookedSeats(tripId: Int): List<Int> {
        val bookedSeats = mutableListOf<Int>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT $KEY_SEAT_NUMBER FROM $TABLE_BOOKINGS WHERE $KEY_TRIP_ID_BOOKING = ?",
            arrayOf(tripId.toString())
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
            1 -> { // Слободской → Киров (обновленные названия)
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
            2 -> { // Киров → Слободской (обратный, обновленные названия)
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
            4 -> { // Котельнич → Киров (обратный)
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
            5 -> { // Киров → Вятские Поляны (с исправленными названиями)
                val routeOrder = listOf(
                    "Автовокзал Киров",
                    "Дом техники",
                    "Поворот на Казань (выезд)",
                    "Поворот на Швецово",
                    "Поворот на Речной",
                    "Олимпийский",
                    "Поворот на Вожгалы",
                    "Поворот на Кумёны",
                    "Большевик",
                    "Автостанция Суна",
                    "Кырчаны – 2",
                    "Кырчаны",
                    "Поворот на Нему",
                    "Перевоз",
                    "Нолинск",
                    "Поворот на Нолинск",
                    "Поворот на Юртик",
                    "Сомовщина",
                    "Боровляна",
                    "Поворот на Аркуль",
                    "Поворот на Орешник",
                    "Круглые Полянки",
                    "Петровское – 2",
                    "Петровское – 1",
                    "Скрябино",
                    "Щино",
                    "Вершинята",
                    "Марчата",
                    "Поворот на Андреевский",
                    "Теребиловка",
                    "АТП",
                    "Автостанция Уржум",
                    "Магазин Продукты",
                    "Школа № 3",
                    "Гостиница",
                    "Поворот на Шевнино",
                    "Русский Турек",
                    "Поворот на Русский Турек",
                    "Кизерь",
                    "Поворот на Шурму",
                    "Верхняя Шурма",
                    "Поворот на Лазарево",
                    "Поворот на Тюм-Тюм",
                    "Сосновка",
                    "Большой Рой",
                    "Танабаево",
                    "Манкинерь",
                    "Поворот на Аджим",
                    "Новая Тушка",
                    "Поворот на Рожки",
                    "Поворот на Тат-Верх-Гоньбу",
                    "Поворот на Новый Ирюк",
                    "Поворот на Старый Ирюк",
                    "Поворот на Савали",
                    "Поворот на Малмыж",
                    "Калинино",
                    "Школа",
                    "АЗС",
                    "АТП",
                    "Автостанция Малмыж",
                    "Поворот на Казань (Малмыж)",
                    "Новая Смаиль",
                    "Поворот на Каменный Ключ",
                    "Поречке Китяк",
                    "Кошай",
                    "Большой Китяк",
                    "Малый Китяк",
                    "Поворот на Янгулово",
                    "Кошкино",
                    "Поворот на Новый Бурец",
                    "Челны",
                    "Верхняя Тойма",
                    "Средняя Тойма – 2",
                    "Средняя Тойма – 1",
                    "Нижняя Тойма – 3",
                    "Нижняя Тойма – 2",
                    "Нижняя Тойма – 1",
                    "Кооперативная улица",
                    "Строительная улица",
                    "Переезд",
                    "Улица Тойменка",
                    "Налоговая инспекция",
                    "Машиностроительный завод",
                    "Школа искусств",
                    "ГИБДД",
                    "Автостанция Вятские Поляны"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            6 -> { // Вятские Поляны → Киров (обратный)
                val routeOrder = listOf(
                    "Автостанция Вятские Поляны",
                    "ГИБДД",
                    "Школа искусств",
                    "Машиностроительный завод",
                    "Налоговая инспекция",
                    "Улица Тойменка",
                    "Переезд",
                    "Строительная улица",
                    "Кооперативная улица",
                    "Нижняя Тойма – 1",
                    "Нижняя Тойма – 2",
                    "Нижняя Тойма – 3",
                    "Средняя Тойма – 1",
                    "Средняя Тойма – 2",
                    "Верхняя Тойма",
                    "Челны",
                    "Поворот на Новый Бурец",
                    "Кошкино",
                    "Поворот на Янгулово",
                    "Малый Китяк",
                    "Большой Китяк",
                    "Кошай",
                    "Поречке Китяк",
                    "Поворот на Каменный Ключ",
                    "Новая Смаиль",
                    "Поворот на Казань (Малмыж)",
                    "Автостанция Малмыж",
                    "АТП",
                    "АЗС",
                    "Школа",
                    "Калинино",
                    "Поворот на Малмыж",
                    "Поворот на Савали",
                    "Поворот на Старый Ирюк",
                    "Поворот на Новый Ирюк",
                    "Поворот на Тат-Верх-Гоньбу",
                    "Поворот на Рожки",
                    "Новая Тушка",
                    "Поворот на Аджим",
                    "Манкинерь",
                    "Танабаево",
                    "Большой Рой",
                    "Сосновка",
                    "Поворот на Тюм-Тюм",
                    "Поворот на Лазарево",
                    "Верхняя Шурма",
                    "Поворот на Шурму",
                    "Кизерь",
                    "Поворот на Русский Турек",
                    "Русский Турек",
                    "Поворот на Шевнино",
                    "Гостиница",
                    "Школа № 3",
                    "Магазин Продукты",
                    "Автостанция Уржум",
                    "АТП",
                    "Теребиловка",
                    "Поворот на Андреевский",
                    "Марчата",
                    "Вершинята",
                    "Щино",
                    "Скрябино",
                    "Петровское – 1",
                    "Петровское – 2",
                    "Круглые Полянки",
                    "Поворот на Орешник",
                    "Поворот на Аркуль",
                    "Боровляна",
                    "Сомовщина",
                    "Поворот на Юртик",
                    "Поворот на Нолинск",
                    "Нолинск",
                    "Перевоз",
                    "Поворот на Нему",
                    "Кырчаны",
                    "Кырчаны – 2",
                    "Автостанция Суна",
                    "Большевик",
                    "Поворот на Кумёны",
                    "Поворот на Вожгалы",
                    "Олимпийский",
                    "Поворот на Речной",
                    "Поворот на Швецово",
                    "Поворот на Казань (выезд)",
                    "Дом техники",
                    "Автовокзал Киров"
                )

                routeOrder.forEach { stopName ->
                    getStopCoordinates(stopName)?.let { coordinates.add(it) }
                }
            }
            7 -> { // Киров → Советск (с исправленными названиями)
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
            8 -> { // Советск → Киров (с исправленными названиями)
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
}
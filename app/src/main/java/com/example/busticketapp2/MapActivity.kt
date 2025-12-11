package com.example.busticketapp2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.Stop
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var dbHelper: DatabaseHelper
    private var mMap: GoogleMap? = null
    private lateinit var txtRouteTitle: TextView
    private lateinit var btnBack: Button

    private var tripId: Int = -1
    private var tripName: String = ""
    private lateinit var stops: List<Stop>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        supportActionBar?.hide()

        try {
            dbHelper = DatabaseHelper(this)

            tripId = intent.getIntExtra("TRIP_ID", -1)
            tripName = intent.getStringExtra("TRIP_NAME") ?: "Маршрут"

            Log.d("MapActivity", "Trip ID: $tripId, Name: $tripName")

            // Получаем остановки из базы данных
            stops = dbHelper.getStopsByTripId(tripId)
            Log.d("MapActivity", "Loaded ${stops.size} stops")

            initViews()
            setupMap()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки карты: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        txtRouteTitle = findViewById(R.id.txtRouteTitle)
        btnBack = findViewById(R.id.btnBack)

        txtRouteTitle.text = "🗺️ Карта маршрута: $tripName\nОстановок: ${stops.size}"

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

            if (mapFragment != null) {
                mapFragment.getMapAsync(this)
            } else {
                Toast.makeText(this, "Карта временно недоступна", Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки карты", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            // Настройка карты
            mMap?.uiSettings?.isZoomControlsEnabled = true
            mMap?.uiSettings?.isCompassEnabled = true
            mMap?.uiSettings?.isMapToolbarEnabled = true
            mMap?.uiSettings?.isRotateGesturesEnabled = true

            // Очищаем карту от старых маркеров
            mMap?.clear()

            // Получаем координаты для всех остановок
            val stopCoordinates = getCoordinatesForStops()

            if (stopCoordinates.isNotEmpty()) {
                addStopMarkers(stopCoordinates)
                drawRoute(stopCoordinates)
                centerMapOnRoute(stopCoordinates)

                Toast.makeText(this, "Загружено ${stops.size} остановок", Toast.LENGTH_SHORT).show()
            } else {
                // Если нет координат, используем простую демо-карту
                createDemoMap()
                Toast.makeText(this, "Демо-карта маршрута", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отображения карты: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MapActivity", "Error in onMapReady: ${e.message}", e)
        }
    }

    private fun getCoordinatesForStops(): List<Pair<Stop, LatLng>> {
        val coordinates = mutableListOf<Pair<Stop, LatLng>>()

        stops.forEach { stop ->
            val coords = dbHelper.getStopCoordinates(stop.name)
            if (coords != null) {
                coordinates.add(Pair(stop, LatLng(coords.first, coords.second)))
                Log.d("MapActivity", "Found coordinates for ${stop.name}: $coords")
            } else {
                // Если нет координат в базе, создаем приблизительные
                val demoCoords = createDemoCoordinatesForStop(stop.name)
                coordinates.add(Pair(stop, demoCoords))
                Log.d("MapActivity", "Using demo coordinates for ${stop.name}: $demoCoords")
            }
        }

        return coordinates
    }

    private fun createDemoCoordinatesForStop(stopName: String): LatLng {
        // Основные координаты
        val autoStationSlobodskoy = LatLng(58.721271, 50.181422)
        val avtovokzalKirov = LatLng(58.583651, 49.650495)
        val vahrushi = LatLng(58.678954, 50.024328)
        val kotelnichStation = LatLng(58.312207, 48.341900)

        // Используем when как выражение с явным return
        return when (tripId) {
            1 -> { // Слободской → Киров (полный маршрут через все точки)
                when {
                    stopName.contains("Слободского", ignoreCase = true) || stopName.contains("Автостанция", ignoreCase = true) -> autoStationSlobodskoy
                    stopName.contains("Центр Слободского", ignoreCase = true) -> LatLng(58.724178, 50.180442)
                    stopName.contains("Выезд", ignoreCase = true) -> LatLng(58.723479, 50.155136)
                    stopName.contains("Дороничи", ignoreCase = true) -> LatLng(58.719358, 50.138668)
                    stopName.contains("ПМК", ignoreCase = true) -> LatLng(58.715826, 50.127703)
                    stopName.contains("Ситники", ignoreCase = true) -> LatLng(58.712931, 50.118128)
                    stopName.contains("Первомайский", ignoreCase = true) -> LatLng(58.700771, 50.090956)
                    stopName.contains("Подсобное", ignoreCase = true) -> LatLng(58.692292, 50.058324)
                    stopName.contains("Школа", ignoreCase = true) -> LatLng(58.682776, 50.032523)
                    stopName.contains("Вахруши", ignoreCase = true) && !stopName.contains("обратный") && !stopName.contains("Слободского") -> vahrushi
                    stopName.contains("Рубежница", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.676495, 50.005969)
                    stopName.contains("Логуновы", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.677337, 49.975695)
                    stopName.contains("Осинцы", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.670765, 49.941844)
                    stopName.contains("Луза", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.658791, 49.917796)
                    stopName.contains("Сады Биохимик", ignoreCase = true) && !stopName.contains("2") -> LatLng(58.657271, 49.910616)
                    stopName.contains("Зониха", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.650529, 49.877381)
                    stopName.contains("Пантелеевы", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.646046, 49.860089)
                    stopName.contains("Столбово", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.639777, 49.846169)
                    stopName.contains("Шихово", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.635727, 49.834186)
                    stopName.contains("Трушковы", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.629728, 49.811047)
                    stopName.contains("Бобинский", ignoreCase = true) && !stopName.contains("обратный") -> LatLng(58.623457, 49.784353)
                    stopName.contains("Новомакарьевское", ignoreCase = true) -> LatLng(58.620520, 49.775688)
                    stopName.contains("Порошинский", ignoreCase = true) -> LatLng(58.616520, 49.765303)
                    stopName.contains("Макарье", ignoreCase = true) -> LatLng(58.614844, 49.755896)
                    stopName.contains("Троицкая", ignoreCase = true) -> LatLng(58.613077, 49.750204)
                    stopName.contains("Проезжая", ignoreCase = true) -> LatLng(58.613343, 49.743892)
                    stopName.contains("Заповедная", ignoreCase = true) -> LatLng(58.618765, 49.718881)
                    stopName.contains("Красный Химик", ignoreCase = true) -> LatLng(58.618483, 49.710419)
                    stopName.contains("Дымково", ignoreCase = true) -> LatLng(58.617178, 49.698415)
                    stopName.contains("Профсоюзная", ignoreCase = true) -> LatLng(58.615356, 49.683786)
                    stopName.contains("МОПРа", ignoreCase = true) -> LatLng(58.609771, 49.680656)
                    stopName.contains("Иоанна Предтечи", ignoreCase = true) -> LatLng(58.606565, 49.680864)
                    stopName.contains("Трифонов", ignoreCase = true) -> LatLng(58.600451, 49.681552)
                    stopName.contains("Филармония", ignoreCase = true) -> LatLng(58.594588, 49.681941)
                    stopName.contains("Областная больница", ignoreCase = true) -> LatLng(58.593655, 49.660874)
                    stopName.contains("ЦУМ", ignoreCase = true) -> LatLng(58.593526, 49.655366)
                    stopName.contains("Киров", ignoreCase = true) || stopName.contains("Автовокзал", ignoreCase = true) -> avtovokzalKirov
                    else -> {
                        // Распределяем равномерно между конечными точками
                        val progress = getStopProgress(stopName, stops)
                        val lat = autoStationSlobodskoy.latitude + (avtovokzalKirov.latitude - autoStationSlobodskoy.latitude) * progress
                        val lng = autoStationSlobodskoy.longitude + (avtovokzalKirov.longitude - autoStationSlobodskoy.longitude) * progress
                        LatLng(lat, lng)
                    }
                }
            }

            2 -> { // Киров → Слободской (обратный)
                when {
                    stopName.contains("Киров", ignoreCase = true) || stopName.contains("Автовокзал", ignoreCase = true) -> avtovokzalKirov
                    stopName.contains("ЦУМ", ignoreCase = true) -> LatLng(58.593526, 49.655366)
                    stopName.contains("Областная больница", ignoreCase = true) -> LatLng(58.593655, 49.660874)
                    stopName.contains("Филармония", ignoreCase = true) -> LatLng(58.594588, 49.681941)
                    stopName.contains("Трифонов", ignoreCase = true) -> LatLng(58.600451, 49.681552)
                    stopName.contains("Иоанна Предтечи", ignoreCase = true) -> LatLng(58.606565, 49.680864)
                    stopName.contains("МОПРа", ignoreCase = true) -> LatLng(58.609771, 49.680656)
                    stopName.contains("Профсоюзная", ignoreCase = true) -> LatLng(58.615356, 49.683786)
                    stopName.contains("Дымково", ignoreCase = true) -> LatLng(58.617178, 49.698415)
                    stopName.contains("Красный Химик", ignoreCase = true) -> LatLng(58.618483, 49.710419)
                    stopName.contains("Заповедная", ignoreCase = true) -> LatLng(58.618765, 49.718881)
                    stopName.contains("Проезжая", ignoreCase = true) -> LatLng(58.613343, 49.743892)
                    stopName.contains("Троицкая", ignoreCase = true) -> LatLng(58.613077, 49.750204)
                    stopName.contains("Макарье", ignoreCase = true) -> LatLng(58.614844, 49.755896)
                    stopName.contains("Порошинский", ignoreCase = true) -> LatLng(58.616520, 49.765303)
                    stopName.contains("Новомакарьевское", ignoreCase = true) -> LatLng(58.620520, 49.775688)
                    stopName.contains("Бобинский", ignoreCase = true) -> LatLng(58.623457, 49.784353)
                    stopName.contains("Трушковы", ignoreCase = true) -> LatLng(58.629728, 49.811047)
                    stopName.contains("Шихово", ignoreCase = true) -> LatLng(58.635727, 49.834186)
                    stopName.contains("Столбово", ignoreCase = true) -> LatLng(58.639777, 49.846169)
                    stopName.contains("Пантелеевы", ignoreCase = true) -> LatLng(58.646046, 49.860089)
                    stopName.contains("Зониха", ignoreCase = true) -> LatLng(58.650529, 49.877381)
                    stopName.contains("Сады Биохимик", ignoreCase = true) -> LatLng(58.657271, 49.910616)
                    stopName.contains("Луза", ignoreCase = true) -> LatLng(58.658791, 49.917796)
                    stopName.contains("Осинцы", ignoreCase = true) -> LatLng(58.670765, 49.941844)
                    stopName.contains("Логуновы", ignoreCase = true) -> LatLng(58.677337, 49.975695)
                    stopName.contains("Рубежница", ignoreCase = true) -> LatLng(58.676495, 50.005969)
                    stopName.contains("Вахруши", ignoreCase = true) -> vahrushi
                    stopName.contains("Подсобное", ignoreCase = true) -> LatLng(58.692292, 50.058324)
                    stopName.contains("Первомайский", ignoreCase = true) -> LatLng(58.700771, 50.090956)
                    stopName.contains("Ситники", ignoreCase = true) -> LatLng(58.712931, 50.118128)
                    stopName.contains("ПМК", ignoreCase = true) -> LatLng(58.715826, 50.127703)
                    stopName.contains("Дороничи", ignoreCase = true) -> LatLng(58.719358, 50.138668)
                    stopName.contains("Выезд", ignoreCase = true) -> LatLng(58.723479, 50.155136)
                    stopName.contains("Центр Слободского", ignoreCase = true) -> LatLng(58.724178, 50.180442)
                    stopName.contains("Слободского", ignoreCase = true) || stopName.contains("Автостанция", ignoreCase = true) -> autoStationSlobodskoy
                    else -> {
                        val progress = getStopProgress(stopName, stops)
                        val lat = avtovokzalKirov.latitude + (autoStationSlobodskoy.latitude - avtovokzalKirov.latitude) * progress
                        val lng = avtovokzalKirov.longitude + (autoStationSlobodskoy.longitude - avtovokzalKirov.longitude) * progress
                        LatLng(lat, lng)
                    }
                }
            }

            3 -> { // Киров → Котельнич
                val coordinates = mapOf(
                    "Автовокзал Киров" to avtovokzalKirov,
                    "Улица Дзержинского" to LatLng(58.633361, 49.617675),
                    "Поворот на Гирсово" to LatLng(58.737164, 49.552364),
                    "Поворот на Мурыгино" to LatLng(58.747287, 49.531892),
                    "Горцы" to LatLng(58.759871, 49.512871),
                    "Сады Урожай-1" to LatLng(58.770222, 49.482504),
                    "Поворот на Юрью" to LatLng(58.772118, 49.473849),
                    "Поворот на Медяны" to LatLng(58.771935, 49.384934),
                    "Поворот на Малое Чураково" to LatLng(58.755541, 49.315456),
                    "Лаптевы" to LatLng(58.745547, 49.287798),
                    "Река Великая" to LatLng(58.737099, 49.250816),
                    "Поворот на Цепели" to LatLng(58.722459, 49.221294),
                    "Красногоры" to LatLng(58.715344, 49.210115),
                    "Верхняя Боярщина" to LatLng(58.707308, 49.192200),
                    "Зоновщина" to LatLng(58.702078, 49.169786),
                    "Юркичи" to LatLng(58.692492, 49.129097),
                    "Раменье" to LatLng(58.685231, 49.106490),
                    "Боярщина" to LatLng(58.675677, 49.069284),
                    "Колеватовы" to LatLng(58.670739, 49.056520),
                    "Кузнецы-Орлов" to LatLng(58.658588, 49.029802),
                    "Нижние Опарины" to LatLng(58.647280, 49.006306),
                    "Щенники" to LatLng(58.630891, 48.987304),
                    "Казаковцевы" to LatLng(58.612166, 48.975044),
                    "Весниничи" to LatLng(58.597590, 48.964840),
                    "Назаровы" to LatLng(58.574886, 48.939167),
                    "Поворот на Криничи" to LatLng(58.560212, 48.918384),
                    "Автостанция Орлов" to LatLng(58.548402, 48.898684),
                    "Магазин Золотая марка" to LatLng(58.542541, 48.903440),
                    "Детские ясли" to LatLng(58.540691, 48.901498),
                    "Магазин Петушок" to LatLng(58.536763, 48.895470),
                    "ТЦ Муравейник" to LatLng(58.534106, 48.891417),
                    "Больница" to LatLng(58.531279, 48.886428),
                    "Магазин Наш дом" to LatLng(58.533788, 48.880280),
                    "Мебельная фабрика" to LatLng(58.532313, 48.875484),
                    "Юбилейная улица" to LatLng(58.531889, 48.870343),
                    "Высоково" to LatLng(58.543928, 48.753756),
                    "Осинки" to LatLng(58.489731, 48.587237),
                    "Балванская" to LatLng(58.480585, 48.572046),
                    "Поворот на Юрьево" to LatLng(58.452485, 48.531782),
                    "Скурихинская" to LatLng(58.438628, 48.516946),
                    "Овчинниковы" to LatLng(58.419006, 48.483533),
                    "Минины" to LatLng(58.408897, 48.474876),
                    "Кардаковы" to LatLng(58.400170, 48.467495),
                    "Фадеевцы / Липичи / Жохи" to LatLng(58.376832, 48.453930),
                    "Хаустовы" to LatLng(58.359602, 48.437854),
                    "Гулины" to LatLng(58.348800, 48.428992),
                    "Поворот на Ленинскую Искру" to LatLng(58.334175, 48.417026),
                    "Климичи" to LatLng(58.324498, 48.403480),
                    "Пост ГИБДД" to LatLng(58.318261, 48.396782),
                    "Автостанция Котельнич" to kotelnichStation,
                    "Широченки" to LatLng(58.260866, 48.306513),
                    "Шестаковы" to LatLng(58.247822, 48.306982),
                    "Копылы" to LatLng(58.212727, 48.302973),
                    "Борки" to LatLng(58.18196, 48.316781),  // ИСПРАВЛЕНО: новые координаты между Копылами и Ванюшенками
                    "Ванюшенки" to LatLng(58.151193, 48.330589),
                    "Вишкиль" to LatLng(58.092038, 48.318224),
                    "Мамаи" to LatLng(58.004120, 48.280065),
                    "Смирновы" to LatLng(57.985803, 48.296416),
                    "Боровка" to LatLng(57.948613, 48.328677),  // Это другая остановка!
                    "Криуша" to LatLng(57.908502, 48.412161),
                    "Горбуновщина" to LatLng(57.886925, 48.447575),
                    "Сорвижи" to LatLng(57.864274, 48.534764),
                    "Горбуновщина (обратный)" to LatLng(57.887269, 48.447344),
                    "Криуша (обратный)" to LatLng(57.909146, 48.411091),
                    "Поворот на Кормино" to LatLng(57.887566, 48.355890),
                    "Поворот на Шабры" to LatLng(57.845882, 48.312336),
                    "Поворот на Шембеть" to LatLng(57.810725, 48.283248),
                    "Поворот на Арбаж" to LatLng(57.791532, 48.269725),
                    "Мосуны" to LatLng(57.763075, 48.274191),
                    "Чернушка" to LatLng(57.743750, 48.268683),
                    "Мостолыги" to LatLng(57.712196, 48.265604),
                    "Лобасты" to LatLng(57.690938, 48.290700),
                    "Автостанция Арбаж" to LatLng(57.680673, 48.307524)
                )

                // Ищем координату по точному имени
                return coordinates[stopName] ?: run {
                    val progress = getStopProgress(stopName, stops)
                    val lat = avtovokzalKirov.latitude + (kotelnichStation.latitude - avtovokzalKirov.latitude) * progress
                    val lng = avtovokzalKirov.longitude + (kotelnichStation.longitude - avtovokzalKirov.longitude) * progress
                    LatLng(lat, lng)
                }
            }

            4 -> { // Котельнич → Киров (обратный)
                return when {
                    stopName.contains("Автостанция Арбаж", ignoreCase = true) -> LatLng(57.680673, 48.307524)
                    stopName.contains("Лобасты", ignoreCase = true) -> LatLng(57.690938, 48.290700)
                    stopName.contains("Мостолыги", ignoreCase = true) -> LatLng(57.712196, 48.265604)
                    stopName.contains("Чернушка", ignoreCase = true) -> LatLng(57.743750, 48.268683)
                    stopName.contains("Мосуны", ignoreCase = true) -> LatLng(57.763075, 48.274191)
                    stopName.contains("Поворот на Арбаж", ignoreCase = true) -> LatLng(57.791532, 48.269725)
                    stopName.contains("Поворот на Шембеть", ignoreCase = true) -> LatLng(57.810725, 48.283248)
                    stopName.contains("Поворот на Шабры", ignoreCase = true) -> LatLng(57.845882, 48.312336)
                    stopName.contains("Поворот на Кормино", ignoreCase = true) -> LatLng(57.887566, 48.355890)
                    stopName.contains("Криуша (обратный)", ignoreCase = true) -> LatLng(57.909146, 48.411091)
                    stopName.contains("Горбуновщина (обратный)", ignoreCase = true) -> LatLng(57.887269, 48.447344)
                    stopName.contains("Сорвижи", ignoreCase = true) -> LatLng(57.864274, 48.534764)
                    stopName.contains("Горбуновщина", ignoreCase = true) -> LatLng(57.886925, 48.447575)
                    stopName.contains("Криуша", ignoreCase = true) -> LatLng(57.908502, 48.412161)
                    stopName.contains("Боровка", ignoreCase = true) -> LatLng(57.948613, 48.328677)  // Эта остановка в конце маршрута
                    stopName.contains("Смирновы", ignoreCase = true) -> LatLng(57.985803, 48.296416)
                    stopName.contains("Мамаи", ignoreCase = true) -> LatLng(58.004120, 48.280065)
                    stopName.contains("Вишкиль", ignoreCase = true) -> LatLng(58.092038, 48.318224)
                    stopName.contains("Ванюшенки", ignoreCase = true) -> LatLng(58.151193, 48.330589)
                    stopName.contains("Борки", ignoreCase = true) -> LatLng(58.18196, 48.316781)  // ИСПРАВЛЕНО: координаты между Ванюшенками и Копылами
                    stopName.contains("Копылы", ignoreCase = true) -> LatLng(58.212727, 48.302973)
                    stopName.contains("Шестаковы", ignoreCase = true) -> LatLng(58.247822, 48.306982)
                    stopName.contains("Широченки", ignoreCase = true) -> LatLng(58.260866, 48.306513)
                    stopName.contains("Автостанция Котельнич", ignoreCase = true) -> kotelnichStation
                    stopName.contains("Пост ГИБДД", ignoreCase = true) -> LatLng(58.318261, 48.396782)
                    stopName.contains("Климичи", ignoreCase = true) -> LatLng(58.324498, 48.403480)
                    stopName.contains("Поворот на Ленинскую Искру", ignoreCase = true) -> LatLng(58.334175, 48.417026)
                    stopName.contains("Гулины", ignoreCase = true) -> LatLng(58.348800, 48.428992)
                    stopName.contains("Хаустовы", ignoreCase = true) -> LatLng(58.359602, 48.437854)
                    stopName.contains("Фадеевцы", ignoreCase = true) -> LatLng(58.376832, 48.453930)
                    stopName.contains("Кардаковы", ignoreCase = true) -> LatLng(58.400170, 48.467495)
                    stopName.contains("Минины", ignoreCase = true) -> LatLng(58.408897, 48.474876)
                    stopName.contains("Овчинниковы", ignoreCase = true) -> LatLng(58.419006, 48.483533)
                    stopName.contains("Скурихинская", ignoreCase = true) -> LatLng(58.438628, 48.516946)
                    stopName.contains("Поворот на Юрьево", ignoreCase = true) -> LatLng(58.452485, 48.531782)
                    stopName.contains("Балванская", ignoreCase = true) -> LatLng(58.480585, 48.572046)
                    stopName.contains("Осинки", ignoreCase = true) -> LatLng(58.489731, 48.587237)
                    stopName.contains("Высоково", ignoreCase = true) -> LatLng(58.543928, 48.753756)
                    stopName.contains("Юбилейная улица", ignoreCase = true) -> LatLng(58.531889, 48.870343)
                    stopName.contains("Мебельная фабрика", ignoreCase = true) -> LatLng(58.532313, 48.875484)
                    stopName.contains("Магазин Наш дом", ignoreCase = true) -> LatLng(58.533788, 48.880280)
                    stopName.contains("Больница", ignoreCase = true) -> LatLng(58.531279, 48.886428)
                    stopName.contains("ТЦ Муравейник", ignoreCase = true) -> LatLng(58.534106, 48.891417)
                    stopName.contains("Магазин Петушок", ignoreCase = true) -> LatLng(58.536763, 48.895470)
                    stopName.contains("Детские ясли", ignoreCase = true) -> LatLng(58.540691, 48.901498)
                    stopName.contains("Магазин Золотая марка", ignoreCase = true) -> LatLng(58.542541, 48.903440)
                    stopName.contains("Автостанция Орлов", ignoreCase = true) -> LatLng(58.548402, 48.898684)
                    stopName.contains("Поворот на Криничи", ignoreCase = true) -> LatLng(58.560212, 48.918384)
                    stopName.contains("Назаровы", ignoreCase = true) -> LatLng(58.574886, 48.939167)
                    stopName.contains("Весниничи", ignoreCase = true) -> LatLng(58.597590, 48.964840)
                    stopName.contains("Казаковцевы", ignoreCase = true) -> LatLng(58.612166, 48.975044)
                    stopName.contains("Щенники", ignoreCase = true) -> LatLng(58.630891, 48.987304)
                    stopName.contains("Нижние Опарины", ignoreCase = true) -> LatLng(58.647280, 49.006306)
                    stopName.contains("Кузнецы-Орлов", ignoreCase = true) -> LatLng(58.658588, 49.029802)
                    stopName.contains("Колеватовы", ignoreCase = true) -> LatLng(58.670739, 49.056520)
                    stopName.contains("Боярщина", ignoreCase = true) -> LatLng(58.675677, 49.069284)
                    stopName.contains("Раменье", ignoreCase = true) -> LatLng(58.685231, 49.106490)
                    stopName.contains("Юркичи", ignoreCase = true) -> LatLng(58.692492, 49.129097)
                    stopName.contains("Зоновщина", ignoreCase = true) -> LatLng(58.702078, 49.169786)
                    stopName.contains("Верхняя Боярщина", ignoreCase = true) -> LatLng(58.707308, 49.192200)
                    stopName.contains("Красногоры", ignoreCase = true) -> LatLng(58.715344, 49.210115)
                    stopName.contains("Поворот на Цепели", ignoreCase = true) -> LatLng(58.722459, 49.221294)
                    stopName.contains("Река Великая", ignoreCase = true) -> LatLng(58.737099, 49.250816)
                    stopName.contains("Лаптевы", ignoreCase = true) -> LatLng(58.745547, 49.287798)
                    stopName.contains("Поворот на Малое Чураково", ignoreCase = true) -> LatLng(58.755541, 49.315456)
                    stopName.contains("Поворот на Медяны", ignoreCase = true) -> LatLng(58.771935, 49.384934)
                    stopName.contains("Поворот на Юрью", ignoreCase = true) -> LatLng(58.772118, 49.473849)
                    stopName.contains("Сады Урожай-1", ignoreCase = true) -> LatLng(58.770222, 49.482504)
                    stopName.contains("Горцы", ignoreCase = true) -> LatLng(58.759871, 49.512871)
                    stopName.contains("Поворот на Мурыгино", ignoreCase = true) -> LatLng(58.747287, 49.531892)
                    stopName.contains("Поворот на Гирсово", ignoreCase = true) -> LatLng(58.737164, 49.552364)
                    stopName.contains("Улица Дзержинского", ignoreCase = true) -> LatLng(58.633361, 49.617675)
                    stopName.contains("Автовокзал Киров", ignoreCase = true) -> avtovokzalKirov

                    else -> {
                        val progress = getStopProgress(stopName, stops)
                        val lat = kotelnichStation.latitude + (avtovokzalKirov.latitude - kotelnichStation.latitude) * progress
                        val lng = kotelnichStation.longitude + (avtovokzalKirov.longitude - kotelnichStation.longitude) * progress
                        LatLng(lat, lng)
                    }
                }
            }

            // УДАЛЕНЫ маршруты 5 и 6 (Киров-Вахруши и Слободской-Вахруши)

            else -> LatLng(58.6, 49.6)  // Добавляем else для when выражения
        }
    }


    private fun getStopProgress(stopName: String, stops: List<Stop>): Double {
        val index = stops.indexOfFirst { it.name.contains(stopName, ignoreCase = true) }
        return if (index >= 0 && stops.size > 1) {
            index.toDouble() / (stops.size - 1)
        } else {
            0.5
        }
    }

    private fun createSimpleDemoLocationForOtherRoutes(stopName: String): LatLng {
        // Существующая логика для других маршрутов
        val baseLat = when (tripId) {
            3, 4 -> 58.62  // Киров-Котельнич и обратно
            5 -> 58.64     // Киров-Вахруши
            6 -> 58.70     // Слободской-Вахруши
            else -> 58.60
        }

        val baseLng = when (tripId) {
            3, 4 -> 49.7   // Киров-Котельнич и обратно
            5 -> 49.9      // Киров-Вахруши
            6 -> 50.1      // Слободской-Вахруши
            else -> 49.6
        }

        val index = stops.indexOfFirst { it.name.contains(stopName, ignoreCase = true) }
        val progress = if (index >= 0 && stops.size > 1) index.toDouble() / (stops.size - 1) else 0.0

        val lat = baseLat - (progress * 0.1)
        val lng = baseLng - (progress * 0.2)

        return LatLng(lat, lng)
    }

    private fun addStopMarkers(stopCoordinates: List<Pair<Stop, LatLng>>) {
        if (stopCoordinates.isEmpty()) return

        val startColor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        val endColor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        val intermediateColor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

        stopCoordinates.forEachIndexed { index, (stop, location) ->
            val markerColor = when {
                index == 0 -> startColor
                index == stopCoordinates.size - 1 -> endColor
                else -> intermediateColor
            }

            mMap?.addMarker(MarkerOptions()
                .position(location)
                .title("${index + 1}. ${stop.name}")
                .snippet("⏰ ${stop.arrivalTime} - 💰 ${stop.priceFromStart.toInt()} руб.")
                .icon(markerColor)
                .alpha(0.9f))

            Log.d("MapActivity", "Added marker ${index + 1}/${stopCoordinates.size}: ${stop.name} at $location")
        }
    }

    private fun drawRoute(stopCoordinates: List<Pair<Stop, LatLng>>) {
        if (stopCoordinates.size < 2) return

        val points = stopCoordinates.map { it.second }

        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(8f)
            .color(Color.parseColor("#1976D2"))
            .geodesic(true)

        mMap?.addPolyline(polylineOptions)

        // Добавляем начальную и конечную точки
        val startLocation = stopCoordinates.first().second
        val endLocation = stopCoordinates.last().second

        mMap?.addMarker(MarkerOptions()
            .position(startLocation)
            .title("🟢 Начало маршрута")
            .snippet(stops.first().name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))

        mMap?.addMarker(MarkerOptions()
            .position(endLocation)
            .title("🔴 Конец маршрута")
            .snippet(stops.last().name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

        Log.d("MapActivity", "Route drawn from: $startLocation to: $endLocation")
        Log.d("MapActivity", "First stop: ${stops.first().name}")
        Log.d("MapActivity", "Last stop: ${stops.last().name}")
    }

    private fun centerMapOnRoute(stopCoordinates: List<Pair<Stop, LatLng>>) {
        try {
            if (stopCoordinates.isEmpty()) return

            val builder = LatLngBounds.builder()

            stopCoordinates.forEach { (_, location) ->
                builder.include(location)
            }

            val bounds = builder.build()

            // Анимируем камеру к границам с отступами
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

            mMap?.animateCamera(cameraUpdate)

            Log.d("MapActivity", "Map centered on ${stopCoordinates.size} points")

        } catch (e: Exception) {
            // Если не удается показать все маркеры, показываем первый и последний
            val firstLocation = stopCoordinates.first().second
            val lastLocation = stopCoordinates.last().second

            val midLat = (firstLocation.latitude + lastLocation.latitude) / 2
            val midLng = (firstLocation.longitude + lastLocation.longitude) / 2

            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 10f))

            Log.d("MapActivity", "Map centered on midpoint: ($midLat, $midLng)")
        }
    }

    private fun createDemoMap() {
        if (stops.isEmpty()) return

        // Создаем демо-координаты для всех остановок
        val demoCoordinates = stops.mapIndexed { index, stop ->
            val demoLocation = createSimpleDemoLocation(index)
            Pair(stop, demoLocation)
        }

        addStopMarkers(demoCoordinates)

        // Рисуем простую линию
        if (demoCoordinates.size >= 2) {
            val points = demoCoordinates.map { it.second }
            val polylineOptions = PolylineOptions()
                .addAll(points)
                .width(6f)
                .color(Color.parseColor("#2196F3"))
                .geodesic(true)

            mMap?.addPolyline(polylineOptions)
        }

        // Центрируем на первой точке
        if (demoCoordinates.isNotEmpty()) {
            val firstLocation = demoCoordinates.first().second
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10f))
        }
    }

    private fun createSimpleDemoLocation(index: Int): LatLng {
        // Простые координаты для демо
        val baseLat = when (tripId) {
            1, 2 -> 58.65  // Слободской-Киров и обратно
            3, 4 -> 58.62  // Киров-Котельнич и обратно
            5 -> 58.64     // Киров-Вахруши
            6 -> 58.70     // Слободской-Вахруши
            else -> 58.60
        }

        val baseLng = when (tripId) {
            1, 2 -> 49.9   // Слободской-Киров и обратно
            3, 4 -> 49.7   // Киров-Котельнич и обратно
            5 -> 49.9      // Киров-Вахруши
            6 -> 50.1      // Слободской-Вахруши
            else -> 49.6
        }

        val progress = if (stops.size > 1) index.toDouble() / (stops.size - 1) else 0.0

        val lat = baseLat - (progress * 0.1)
        val lng = baseLng - (progress * 0.2)

        return LatLng(lat, lng)
    }
}
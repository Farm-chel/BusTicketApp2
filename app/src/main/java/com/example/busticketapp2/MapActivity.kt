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

    companion object {
        // Цвета для разных маршрутов
        private val ROUTE_COLORS = mapOf(
            1 to Color.parseColor("#2196F3"),  // Слободской → Киров - синий
            2 to Color.parseColor("#4CAF50"),  // Киров → Слободской - зеленый
            3 to Color.parseColor("#FF9800"),  // Киров → Котельнич - оранжевый
            4 to Color.parseColor("#9C27B0"),  // Котельнич → Киров - фиолетовый
            5 to Color.parseColor("#F44336"),  // Киров → Вятские Поляны - красный
            6 to Color.parseColor("#00BCD4"),  // Вятские Поляны → Киров - голубой
            7 to Color.parseColor("#795548"),  // Киров → Советск - коричневый
            8 to Color.parseColor("#607D8B")   // Советск → Киров - серый
        )
    }

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

        // Эмодзи для маршрута
        val emoji = when (tripId) {
            1, 2 -> "🏙️"  // Слободской-Киров
            3, 4 -> "🚂"  // Киров-Котельнич
            5, 6 -> "🌲"  // Киров-Вятские Поляны
            7, 8 -> "🏛️"  // Киров-Советск
            else -> "🗺️"
        }

        txtRouteTitle.text = "$emoji Карта маршрута: $tripName\n🚏 Остановок: ${stops.size}"

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
            mMap?.uiSettings?.isScrollGesturesEnabled = true
            mMap?.uiSettings?.isZoomGesturesEnabled = true

            // Очищаем карту от старых маркеров
            mMap?.clear()

            if (stops.isEmpty()) {
                showToast("Нет данных об остановках для этого маршрута")
                return
            }

            // Получаем координаты для всех остановок
            val stopCoordinates = getCoordinatesForStops()

            if (stopCoordinates.isEmpty()) {
                showToast("Не удалось получить координаты остановок")
                return
            }

            // Добавляем маркеры и линию маршрута
            addStopMarkers(stopCoordinates)
            drawRoute(stopCoordinates)
            centerMapOnRoute(stopCoordinates)

            Toast.makeText(this, "Загружено ${stopCoordinates.size} остановок", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отображения карты: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MapActivity", "Error in onMapReady: ${e.message}", e)
        }
    }

    private fun getCoordinatesForStops(): List<Pair<Stop, LatLng>> {
        val coordinates = mutableListOf<Pair<Stop, LatLng>>()

        Log.d("MapActivity", "=== Получение координат для маршрута $tripId ===")

        for ((index, stop) in stops.withIndex()) {
            val originalStopName = stop.name
            Log.d("MapActivity", "Остановка #${index + 1}: '$originalStopName'")

            // Используем DatabaseHelper для получения координат
            val coords = dbHelper.getStopCoordinates(originalStopName, tripId)

            if (coords != null && coords != Pair(58.600000, 49.600000)) {
                val latLng = LatLng(coords.first, coords.second)
                coordinates.add(Pair(stop, latLng))
                Log.d("MapActivity", "✓ Найдены координаты: ${coords.first}, ${coords.second}")
            } else {
                // Если координаты не найдены, используем метод DatabaseHelper
                val approxCoords = getApproximateCoordinates(originalStopName, index)
                coordinates.add(Pair(stop, approxCoords))
                Log.d("MapActivity", "⚠ Приблизительные координаты: ${approxCoords.latitude}, ${approxCoords.longitude}")
            }
        }

        Log.d("MapActivity", "=== Всего найдено ${coordinates.size} из ${stops.size} остановок ===")
        return coordinates
    }

    private fun getApproximateCoordinates(stopName: String, index: Int): LatLng {
        // Используем базовые координаты городов
        val cityCoords = when {
            stopName.contains("Киров", ignoreCase = true) -> LatLng(58.583651, 49.650495)
            stopName.contains("Слободской", ignoreCase = true) -> LatLng(58.721262, 50.181554)
            stopName.contains("Котельнич", ignoreCase = true) -> LatLng(58.312207, 48.341900)
            stopName.contains("Вятские Поляны", ignoreCase = true) -> LatLng(56.224749, 51.079241)
            stopName.contains("Советск", ignoreCase = true) -> LatLng(57.592981, 48.969190)
            stopName.contains("Орлов", ignoreCase = true) -> LatLng(58.548402, 48.898684)
            stopName.contains("Верхошижемье", ignoreCase = true) -> LatLng(58.007893, 49.106358)
            stopName.contains("Уржум", ignoreCase = true) -> LatLng(57.120178, 49.994436)
            stopName.contains("Нолинск", ignoreCase = true) -> LatLng(57.562190, 49.950472)
            stopName.contains("Малмыж", ignoreCase = true) -> LatLng(56.517984, 50.670337)
            stopName.contains("Арбаж", ignoreCase = true) -> LatLng(57.680673, 48.307524)
            else -> null
        }

        if (cityCoords != null) {
            return cityCoords
        }

        // Если это не город, распределяем точки равномерно по маршруту
        val (start, end) = when (tripId) {
            1 -> Pair(LatLng(58.721262, 50.181554), LatLng(58.583651, 49.650495)) // Слободской → Киров
            2 -> Pair(LatLng(58.583651, 49.650495), LatLng(58.721262, 50.181554)) // Киров → Слободской
            3 -> Pair(LatLng(58.583651, 49.650495), LatLng(58.312207, 48.341900)) // Киров → Котельнич
            4 -> Pair(LatLng(58.312207, 48.341900), LatLng(58.583651, 49.650495)) // Котельнич → Киров
            5 -> Pair(LatLng(58.583651, 49.650495), LatLng(56.224749, 51.079241)) // Киров → Вятские Поляны
            6 -> Pair(LatLng(56.224749, 51.079241), LatLng(58.583651, 49.650495)) // Вятские Поляны → Киров
            7 -> Pair(LatLng(58.583651, 49.650495), LatLng(57.592981, 48.969190)) // Киров → Советск
            8 -> Pair(LatLng(57.592981, 48.969190), LatLng(58.583651, 49.650495)) // Советск → Киров
            else -> Pair(LatLng(58.583651, 49.650495), LatLng(58.721262, 50.181554))
        }

        val progress = if (stops.size > 1) index.toDouble() / (stops.size - 1) else 0.5

        val lat = start.latitude + (end.latitude - start.latitude) * progress
        val lng = start.longitude + (end.longitude - start.longitude) * progress

        Log.d("MapActivity", "Приблизительные координаты для '$stopName': прогресс=$progress, lat=$lat, lng=$lng")

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
                .snippet("Время: ${stop.arrivalTime}")
                .icon(markerColor)
                .alpha(0.9f))
        }
    }

    private fun drawRoute(stopCoordinates: List<Pair<Stop, LatLng>>) {
        if (stopCoordinates.size < 2) return

        val points = stopCoordinates.map { it.second }
        val routeColor = ROUTE_COLORS[tripId] ?: Color.BLUE

        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(8f)
            .color(routeColor)
            .geodesic(true)

        mMap?.addPolyline(polylineOptions)
    }

    private fun centerMapOnRoute(stopCoordinates: List<Pair<Stop, LatLng>>) {
        try {
            if (stopCoordinates.isEmpty()) return

            val builder = LatLngBounds.builder()

            stopCoordinates.forEach { (_, location) ->
                builder.include(location)
            }

            val bounds = builder.build()

            // Проверяем, чтобы границы были валидными
            if (bounds.northeast.latitude - bounds.southwest.latitude < 0.001 ||
                bounds.northeast.longitude - bounds.southwest.longitude < 0.001) {
                // Если маршрут очень короткий, показываем с зумом
                val center = LatLng(
                    (bounds.northeast.latitude + bounds.southwest.latitude) / 2,
                    (bounds.northeast.longitude + bounds.southwest.longitude) / 2
                )
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))
            } else {
                val padding = 100 // отступ в пикселях
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                mMap?.animateCamera(cameraUpdate)
            }

        } catch (e: Exception) {
            // Если не удается показать все маркеры, показываем первый и последний
            val firstLocation = stopCoordinates.first().second
            val lastLocation = stopCoordinates.last().second

            val midLat = (firstLocation.latitude + lastLocation.latitude) / 2
            val midLng = (firstLocation.longitude + lastLocation.longitude) / 2

            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 10f))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
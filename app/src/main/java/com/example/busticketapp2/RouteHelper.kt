package com.example.busticketapp2

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RouteHelper(private val context: Context) {

    companion object {
        private const val TAG = "RouteHelper"
        private const val API_KEY = "AIzaSyDBnpKh31IjY1cRxzTNZdzM76J-aNmyMMk" // Ваш ключ из манифеста
    }

    fun getRouteCoordinates(origin: LatLng, destination: LatLng, callback: (List<LatLng>?) -> Unit) {
        Thread {
            try {
                val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origin.latitude},${origin.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&key=$API_KEY"

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val routePoints = parseRouteFromJson(response)
                    callback(routePoints)
                } else {
                    Log.e(TAG, "HTTP error: $responseCode")
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching route: ${e.message}")
                callback(null)
            }
        }.start()
    }

    private fun parseRouteFromJson(jsonResponse: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(jsonResponse)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val legs = route.getJSONArray("legs")
                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    val steps = leg.getJSONArray("steps")

                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val polyline = step.getJSONObject("polyline")
                        val pointsString = polyline.getString("points")
                        points.addAll(decodePolyline(pointsString))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing route JSON: ${e.message}")
        }
        return points
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val point = LatLng(lat / 1E5, lng / 1E5)
            poly.add(point)
        }
        return poly
    }
}
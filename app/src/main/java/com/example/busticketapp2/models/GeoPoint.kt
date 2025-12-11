package com.example.busticketapp2.models

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val address: String = ""
)

package com.example.busticketapp2.models

data class Stop(
    val name: String,
    val arrivalTime: String,
    val departureTime: String,
    val priceFromStart: Double
)
package com.example.busticketapp2.models

data class User(
    val id: Int = 0,
    val username: String,
    val password: String,
    val role: String,
    val fullName: String,
    val email: String,
    val createdDate: String = "2024-01-01",
    val phone: String = "",
    val isActive: Boolean = true
)
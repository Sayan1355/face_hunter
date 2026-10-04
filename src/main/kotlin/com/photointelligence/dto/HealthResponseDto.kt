package com.photointelligence.dto

data class HealthResponseDto(
    val status: String = "UP",
    val service: String = "photo-intelligence-backend"
)

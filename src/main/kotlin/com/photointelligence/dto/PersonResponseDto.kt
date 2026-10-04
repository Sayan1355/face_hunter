package com.photointelligence.dto

import java.time.Instant
import java.util.UUID

data class PersonResponseDto(
    val id: UUID,
    val name: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

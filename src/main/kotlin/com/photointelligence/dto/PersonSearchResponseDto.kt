package com.photointelligence.dto

import java.time.Instant
import java.util.UUID

data class PersonSearchResponseDto(
    val id: UUID,
    val name: String?,
    val faceRecordCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)

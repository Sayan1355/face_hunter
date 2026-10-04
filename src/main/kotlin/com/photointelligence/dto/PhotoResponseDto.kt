package com.photointelligence.dto

import java.time.Instant
import java.util.UUID

data class PhotoResponseDto(
    val id: UUID,
    val filePath: String,
    val originalFileName: String,
    val createdAt: Instant
)

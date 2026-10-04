package com.photointelligence.dto

import java.time.Instant
import java.util.UUID

data class FaceRecordResponseDto(
    val id: UUID,
    val photoId: UUID,
    val faceImagePath: String,
    val confidence: Double?,
    val createdAt: Instant
)

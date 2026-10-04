package com.photointelligence.dto

import java.util.UUID

data class AiMatchItemDto(
    val face_id: UUID,
    val person_id: UUID,
    val status: String,
    val similarity: Double?,
    val face_image_path: String?,
    val embedding: List<Double>?
)

data class AiIdentifyResponseDto(
    val faces_detected: Int,
    val matches: List<AiMatchItemDto>
)

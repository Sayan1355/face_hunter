package com.photointelligence.dto

import java.util.UUID

data class PersonMatchSummaryDto(
    val personId: UUID,
    val name: String?,
    val status: String,
    val similarity: Double?
)

data class PhotoProcessResponseDto(
    val photoId: UUID,
    val facesDetected: Int,
    val people: List<PersonMatchSummaryDto>
)

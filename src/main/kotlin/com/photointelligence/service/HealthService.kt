package com.photointelligence.service

import com.photointelligence.dto.HealthResponseDto
import org.springframework.stereotype.Service

@Service
class HealthService {

    fun getHealth(): HealthResponseDto {
        return HealthResponseDto(
            status = "UP",
            service = "photo-intelligence-backend"
        )
    }
}

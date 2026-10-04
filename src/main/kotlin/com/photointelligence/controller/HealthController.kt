package com.photointelligence.controller

import com.photointelligence.dto.HealthResponseDto
import com.photointelligence.service.HealthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController(
    private val healthService: HealthService
) {

    @GetMapping("/health")
    fun getHealth(): ResponseEntity<HealthResponseDto> {
        return ResponseEntity.ok(healthService.getHealth())
    }
}

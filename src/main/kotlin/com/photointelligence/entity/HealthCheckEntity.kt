package com.photointelligence.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "health_check")
class HealthCheckEntity(
    @Id
    var id: String = "system",
    var lastCheckedAt: Instant = Instant.now()
)

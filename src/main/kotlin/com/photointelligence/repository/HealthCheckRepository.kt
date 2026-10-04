package com.photointelligence.repository

import com.photointelligence.entity.HealthCheckEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HealthCheckRepository : JpaRepository<HealthCheckEntity, String>

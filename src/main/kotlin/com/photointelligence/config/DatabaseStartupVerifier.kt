package com.photointelligence.config

import com.photointelligence.entity.HealthCheckEntity
import com.photointelligence.repository.HealthCheckRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DatabaseStartupVerifier(
    private val healthCheckRepository: HealthCheckRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DatabaseStartupVerifier::class.java)

    override fun run(vararg args: String) {
        try {
            val entity = HealthCheckEntity(id = "startup-check", lastCheckedAt = Instant.now())
            healthCheckRepository.save(entity)
            logger.info("Successfully connected to PostgreSQL database and updated health_check table!")
        } catch (ex: Exception) {
            logger.error("Failed to connect to PostgreSQL database: {}", ex.message, ex)
            throw ex
        }
    }
}

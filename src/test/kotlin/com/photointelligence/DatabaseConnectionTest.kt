package com.photointelligence

import com.photointelligence.entity.HealthCheckEntity
import com.photointelligence.repository.HealthCheckRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private lateinit var healthCheckRepository: HealthCheckRepository

    @Test
    fun `verify database connectivity and repository operations`() {
        val testId = "test-connection-${System.currentTimeMillis()}"
        val entity = HealthCheckEntity(id = testId, lastCheckedAt = Instant.now())
        
        healthCheckRepository.save(entity)

        val retrieved = healthCheckRepository.findById(testId)
        assertTrue(retrieved.isPresent, "Entity should be found in PostgreSQL database")
        assertEquals(testId, retrieved.get().id)

        healthCheckRepository.deleteById(testId)
    }
}

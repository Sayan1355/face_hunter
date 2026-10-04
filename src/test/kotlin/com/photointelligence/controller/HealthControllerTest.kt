package com.photointelligence.controller

import com.photointelligence.service.HealthService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class HealthControllerTest {

    private val healthService = HealthService()
    private val healthController = HealthController(healthService)
    private val mockMvc = MockMvcBuilders.standaloneSetup(healthController).build()

    @Test
    fun `GET api health should return status UP and service name`() {
        val response = healthController.getHealth()
        assertEquals(200, response.statusCode.value())
        assertEquals("UP", response.body?.status)
        assertEquals("photo-intelligence-backend", response.body?.service)

        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("photo-intelligence-backend"))
    }
}

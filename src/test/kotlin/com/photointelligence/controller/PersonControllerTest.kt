package com.photointelligence.controller

import com.photointelligence.dto.CreatePersonRequestDto
import com.photointelligence.dto.UpdatePersonRequestDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class PersonControllerTest {

    @Autowired
    private lateinit var personController: PersonController

    @Test
    fun `test full person API controller flow`() {
        val mockMvc = MockMvcBuilders.standaloneSetup(personController).build()

        // 1. Create person
        val createDto = CreatePersonRequestDto(name = "Sayan")
        val createResponse = personController.createPerson(createDto)
        assertEquals(201, createResponse.statusCode.value())
        val createdPerson = createResponse.body!!
        assertNotNull(createdPerson.id)
        assertEquals("Sayan", createdPerson.name)

        val personId = createdPerson.id

        // 2. Get person endpoint via MockMvc
        mockMvc.perform(get("/api/persons/$personId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(personId.toString()))
            .andExpect(jsonPath("$.name").value("Sayan"))

        // 3. Update person name
        val updateDto = UpdatePersonRequestDto(name = "Sayan Paul")
        val updateResponse = personController.updatePersonName(personId, updateDto)
        assertEquals(200, updateResponse.statusCode.value())
        assertEquals(personId, updateResponse.body?.id)
        assertEquals("Sayan Paul", updateResponse.body?.name)

        // 4. List persons endpoint via MockMvc
        mockMvc.perform(get("/api/persons"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)

        // 5. Get person faces endpoint via MockMvc
        mockMvc.perform(get("/api/persons/$personId/faces"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
}

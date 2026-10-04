package com.photointelligence.controller

import com.photointelligence.dto.AiIdentifyResponseDto
import com.photointelligence.dto.AiMatchItemDto
import com.photointelligence.service.AiServiceClient
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@Transactional
class PhotoControllerTest {

    @Autowired
    private lateinit var photoController: PhotoController

    @Autowired
    private lateinit var personController: PersonController

    @MockitoBean
    private lateinit var aiServiceClient: AiServiceClient

    private fun <T> anyObj(): T {
        ArgumentMatchers.any<T>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    @Test
    fun `test photo process endpoint and search endpoints`() {
        val mockMvc = MockMvcBuilders.standaloneSetup(photoController, personController).build()

        val personId = UUID.randomUUID()
        val mockAiResponse = AiIdentifyResponseDto(
            faces_detected = 1,
            matches = listOf(
                AiMatchItemDto(
                    face_id = UUID.randomUUID(),
                    person_id = personId,
                    status = "NEW_PERSON",
                    similarity = null,
                    face_image_path = "storage/faces/test.jpg",
                    embedding = listOf(0.1, 0.2)
                )
            )
        )
        `when`(aiServiceClient.identifyFaces(anyObj(), ArgumentMatchers.anyString())).thenReturn(mockAiResponse)

        val file = MockMultipartFile("file", "sample.jpg", "image/jpeg", "image_bytes".toByteArray())

        // 1. Process photo endpoint
        mockMvc.perform(
            multipart("/api/photos/process").file(file)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.facesDetected").value(1))
            .andExpect(jsonPath("$.people[0].personId").value(personId.toString()))

        // 2. Search person endpoint
        mockMvc.perform(get("/api/persons/search").param("name", ""))
            .andExpect(status().isOk)

        // 3. Person photos endpoint
        mockMvc.perform(get("/api/persons/$personId/photos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
}

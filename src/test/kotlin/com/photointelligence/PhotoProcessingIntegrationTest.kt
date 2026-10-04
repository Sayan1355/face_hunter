package com.photointelligence

import com.photointelligence.dto.AiIdentifyResponseDto
import com.photointelligence.dto.AiMatchItemDto
import com.photointelligence.repository.FaceRecordRepository
import com.photointelligence.repository.PersonRepository
import com.photointelligence.repository.PhotoRepository
import com.photointelligence.service.AiServiceClient
import com.photointelligence.service.PhotoService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.UUID

@SpringBootTest
@Transactional
class PhotoProcessingIntegrationTest {

    @Autowired
    private lateinit var photoService: PhotoService

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var faceRecordRepository: FaceRecordRepository

    @MockitoBean
    private lateinit var aiServiceClient: AiServiceClient

    private fun <T> anyObj(): T {
        ArgumentMatchers.any<T>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    @Test
    fun `test full photo processing flow single face new person then matching person`() {
        val person1Id = UUID.randomUUID()
        val face1Id = UUID.randomUUID()

        // 1. Mock AI response for 1st image -> NEW_PERSON
        val mockAiResponse1 = AiIdentifyResponseDto(
            faces_detected = 1,
            matches = listOf(
                AiMatchItemDto(
                    face_id = face1Id,
                    person_id = person1Id,
                    status = "NEW_PERSON",
                    similarity = null,
                    face_image_path = "storage/faces/$face1Id.jpg",
                    embedding = listOf(0.1, 0.2, 0.3)
                )
            )
        )
        `when`(aiServiceClient.identifyFaces(anyObj(), ArgumentMatchers.anyString())).thenReturn(mockAiResponse1)

        val file1 = MockMultipartFile("file", "test1.jpg", "image/jpeg", "dummy_image_data_1".toByteArray())

        // Process 1st photo
        val result1 = photoService.processPhoto(file1)

        // 2. Verify Photo record
        val photo1 = photoRepository.findById(result1.photoId).orElseThrow()
        assertEquals("test1.jpg", photo1.originalFileName)
        assertTrue(File(photo1.filePath).exists())

        // 3. Verify Person record
        val person1 = personRepository.findById(person1Id).orElseThrow()
        assertNull(person1.name)
        assertEquals(person1Id, person1.id)

        // 4. Verify FaceRecord
        val faces1 = faceRecordRepository.findByPersonId(person1Id)
        assertEquals(1, faces1.size)
        assertEquals(photo1.id, faces1[0].photo.id)
        assertEquals(person1.id, faces1[0].person.id)
        assertNotNull(faces1[0].embedding)

        // 5. Mock AI response for 2nd image -> MATCHED person1Id
        val face2Id = UUID.randomUUID()
        val mockAiResponse2 = AiIdentifyResponseDto(
            faces_detected = 1,
            matches = listOf(
                AiMatchItemDto(
                    face_id = face2Id,
                    person_id = person1Id,
                    status = "MATCHED",
                    similarity = 0.92,
                    face_image_path = "storage/faces/$face2Id.jpg",
                    embedding = listOf(0.1, 0.2, 0.3)
                )
            )
        )
        `when`(aiServiceClient.identifyFaces(anyObj(), ArgumentMatchers.anyString())).thenReturn(mockAiResponse2)

        val file2 = MockMultipartFile("file", "test2.jpg", "image/jpeg", "dummy_image_data_2".toByteArray())

        // Process 2nd photo
        val result2 = photoService.processPhoto(file2)
        assertEquals(1, result2.people.size)
        assertEquals("MATCHED", result2.people[0].status)
        assertEquals(person1Id, result2.people[0].personId)

        // Verify face records count for person1 is now 2
        val facesUpdated = faceRecordRepository.findByPersonId(person1Id)
        assertEquals(2, facesUpdated.size)

        // 6. Test updating person's name
        val uniqueName = "UniqueTestPerson_${System.currentTimeMillis()}"
        person1.name = uniqueName
        personRepository.saveAndFlush(person1)

        // 7. Test searching person by name
        val searchResults = photoService.searchPersons(uniqueName.lowercase())
        assertEquals(1, searchResults.size)
        assertEquals(uniqueName, searchResults[0].name)
        assertEquals(2, searchResults[0].faceRecordCount)

        // 8. Test querying person's photos
        val personPhotos = photoService.getPersonPhotos(person1Id)
        assertEquals(2, personPhotos.size)
        assertTrue(personPhotos.any { it.id == photo1.id })
        assertTrue(personPhotos.any { it.id == result2.photoId })
    }
}

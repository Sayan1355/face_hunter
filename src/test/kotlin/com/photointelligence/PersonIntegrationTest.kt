package com.photointelligence

import com.photointelligence.dto.CreatePersonRequestDto
import com.photointelligence.dto.UpdatePersonRequestDto
import com.photointelligence.entity.FaceRecordEntity
import com.photointelligence.entity.PersonEntity
import com.photointelligence.entity.PhotoEntity
import com.photointelligence.repository.FaceRecordRepository
import com.photointelligence.repository.PersonRepository
import com.photointelligence.repository.PhotoRepository
import com.photointelligence.service.PersonService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@Transactional
class PersonIntegrationTest {

    @Autowired
    private lateinit var personService: PersonService

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var photoRepository: PhotoRepository

    @Autowired
    private lateinit var faceRecordRepository: FaceRecordRepository

    @Test
    fun `test creating getting updating and listing persons`() {
        // 1. Create Person
        val createDto = CreatePersonRequestDto(name = "Sayan")
        val createdPerson = personService.createPerson(createDto)
        
        assertNotNull(createdPerson.id)
        assertEquals("Sayan", createdPerson.name)
        assertNotNull(createdPerson.createdAt)
        assertNotNull(createdPerson.updatedAt)

        val originalId = createdPerson.id

        // 2. Get Person
        val fetchedPerson = personService.getPerson(originalId)
        assertEquals(originalId, fetchedPerson.id)
        assertEquals("Sayan", fetchedPerson.name)

        // 3. Update Person Name only
        val updateDto = UpdatePersonRequestDto(name = "Sayan Paul")
        val updatedPerson = personService.updatePersonName(originalId, updateDto)

        // Ensure ID remains unchanged
        assertEquals(originalId, updatedPerson.id)
        assertEquals("Sayan Paul", updatedPerson.name)
        assertEquals(createdPerson.createdAt, updatedPerson.createdAt)

        // 4. List Persons
        val list = personService.listPersons()
        assertTrue(list.any { it.id == originalId && it.name == "Sayan Paul" })
    }

    @Test
    fun `test Person to FaceRecord relationship and fetching person faces`() {
        // 1. Create Person
        val person = personRepository.save(PersonEntity(name = "Sayan"))
        assertNotNull(person.id)

        // 2. Create Photo
        val photo = photoRepository.save(
            PhotoEntity(
                filePath = "/storage/photos/img_001.jpg",
                originalFileName = "img_001.jpg"
            )
        )
        assertNotNull(photo.id)

        // 3. Create FaceRecords linked to Person and Photo
        val face1 = faceRecordRepository.save(
            FaceRecordEntity(
                person = person,
                photo = photo,
                faceImagePath = "/storage/faces/face_001.crop.jpg",
                confidence = 0.98
            )
        )
        val face2 = faceRecordRepository.save(
            FaceRecordEntity(
                person = person,
                photo = photo,
                faceImagePath = "/storage/faces/face_002.crop.jpg",
                confidence = 0.95
            )
        )

        // 4. Fetch person faces via PersonService
        val faces = personService.getPersonFaces(person.id!!)
        assertEquals(2, faces.size)
        assertTrue(faces.any { it.id == face1.id && it.photoId == photo.id && it.faceImagePath == "/storage/faces/face_001.crop.jpg" && it.confidence == 0.98 })
        assertTrue(faces.any { it.id == face2.id && it.photoId == photo.id && it.faceImagePath == "/storage/faces/face_002.crop.jpg" && it.confidence == 0.95 })
    }
}

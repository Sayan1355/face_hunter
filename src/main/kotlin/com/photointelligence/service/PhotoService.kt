package com.photointelligence.service

import com.photointelligence.dto.PersonMatchSummaryDto
import com.photointelligence.dto.PersonSearchResponseDto
import com.photointelligence.dto.PhotoProcessResponseDto
import com.photointelligence.dto.PhotoResponseDto
import com.photointelligence.entity.FaceRecordEntity
import com.photointelligence.entity.PersonEntity
import com.photointelligence.entity.PhotoEntity
import com.photointelligence.exception.PersonNotFoundException
import com.photointelligence.repository.FaceRecordRepository
import com.photointelligence.repository.PersonRepository
import com.photointelligence.repository.PhotoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@Service
class PhotoService(
    private val photoRepository: PhotoRepository,
    private val personRepository: PersonRepository,
    private val faceRecordRepository: FaceRecordRepository,
    private val aiServiceClient: AiServiceClient
) {

    private val originalsDir = File("storage/originals").apply { mkdirs() }

    @Transactional
    fun processPhoto(file: MultipartFile): PhotoProcessResponseDto {
        if (file.isEmpty) {
            throw IllegalArgumentException("Uploaded file is empty")
        }

        val originalFilename = file.originalFilename ?: "photo.jpg"
        val photoUuid = UUID.randomUUID()
        val extension = originalFilename.substringAfterLast('.', "jpg")
        val savedFileName = "$photoUuid.$extension"
        val savedFile = File(originalsDir, savedFileName)

        val imageBytes = file.bytes
        savedFile.writeBytes(imageBytes)

        val photoEntity = photoRepository.save(
            PhotoEntity(
                filePath = "storage/originals/$savedFileName",
                originalFileName = originalFilename
            )
        )

        // Call Python AI microservice
        val aiResult = aiServiceClient.identifyFaces(imageBytes, originalFilename)

        val personSummaries = mutableListOf<PersonMatchSummaryDto>()

        for (match in aiResult.matches) {
            val personId = match.person_id

            val person = personRepository.findById(personId).orElseGet {
                personRepository.save(PersonEntity(id = personId, name = null))
            }

            val embeddingJson = match.embedding?.joinToString(prefix = "[", postfix = "]", separator = ",")

            val faceRecord = FaceRecordEntity(
                person = person,
                photo = photoEntity,
                faceImagePath = match.face_image_path ?: "storage/faces/${match.face_id}.jpg",
                confidence = 1.0,
                similarity = match.similarity,
                embedding = embeddingJson
            )
            faceRecordRepository.save(faceRecord)

            personSummaries.add(
                PersonMatchSummaryDto(
                    personId = person.id!!,
                    name = person.name,
                    status = match.status,
                    similarity = match.similarity
                )
            )
        }

        return PhotoProcessResponseDto(
            photoId = photoEntity.id!!,
            facesDetected = aiResult.faces_detected,
            people = personSummaries
        )
    }

    @Transactional(readOnly = true)
    fun searchPersons(name: String): List<PersonSearchResponseDto> {
        val persons = personRepository.findByNameContainingIgnoreCase(name)
        return persons.map { p ->
            val count = faceRecordRepository.countByPersonId(p.id!!)
            PersonSearchResponseDto(
                id = p.id!!,
                name = p.name,
                faceRecordCount = count,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPersonPhotos(personId: UUID): List<PhotoResponseDto> {
        if (!personRepository.existsById(personId)) {
            throw PersonNotFoundException(personId)
        }
        val photos = faceRecordRepository.findDistinctPhotosByPersonId(personId)
        return photos.map { photo ->
            PhotoResponseDto(
                id = photo.id!!,
                filePath = photo.filePath,
                originalFileName = photo.originalFileName,
                createdAt = photo.createdAt
            )
        }
    }
}

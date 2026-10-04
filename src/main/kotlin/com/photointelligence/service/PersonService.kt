package com.photointelligence.service

import com.photointelligence.dto.CreatePersonRequestDto
import com.photointelligence.dto.FaceRecordResponseDto
import com.photointelligence.dto.PersonResponseDto
import com.photointelligence.dto.UpdatePersonRequestDto
import com.photointelligence.entity.PersonEntity
import com.photointelligence.exception.PersonNotFoundException
import com.photointelligence.repository.FaceRecordRepository
import com.photointelligence.repository.PersonRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PersonService(
    private val personRepository: PersonRepository,
    private val faceRecordRepository: FaceRecordRepository
) {

    @Transactional
    fun createPerson(request: CreatePersonRequestDto): PersonResponseDto {
        val person = PersonEntity(name = request.name)
        val saved = personRepository.save(person)
        return saved.toResponseDto()
    }

    @Transactional(readOnly = true)
    fun getPerson(id: UUID): PersonResponseDto {
        val person = personRepository.findById(id).orElseThrow { PersonNotFoundException(id) }
        return person.toResponseDto()
    }

    @Transactional
    fun updatePersonName(id: UUID, request: UpdatePersonRequestDto): PersonResponseDto {
        val person = personRepository.findById(id).orElseThrow { PersonNotFoundException(id) }
        person.name = request.name
        val updated = personRepository.save(person)
        return updated.toResponseDto()
    }

    @Transactional(readOnly = true)
    fun listPersons(): List<PersonResponseDto> {
        return personRepository.findAll().map { it.toResponseDto() }
    }

    @Transactional(readOnly = true)
    fun getPersonFaces(id: UUID): List<FaceRecordResponseDto> {
        if (!personRepository.existsById(id)) {
            throw PersonNotFoundException(id)
        }
        val faceRecords = faceRecordRepository.findByPersonId(id)
        return faceRecords.map { face ->
            FaceRecordResponseDto(
                id = face.id!!,
                photoId = face.photo.id!!,
                faceImagePath = face.faceImagePath,
                confidence = face.confidence,
                createdAt = face.createdAt
            )
        }
    }

    private fun PersonEntity.toResponseDto(): PersonResponseDto {
        return PersonResponseDto(
            id = this.id!!,
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

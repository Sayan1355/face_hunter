package com.photointelligence.controller

import com.photointelligence.dto.CreatePersonRequestDto
import com.photointelligence.dto.FaceRecordResponseDto
import com.photointelligence.dto.PersonResponseDto
import com.photointelligence.dto.UpdatePersonRequestDto
import com.photointelligence.service.PersonService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/persons")
class PersonController(
    private val personService: PersonService
) {

    @PostMapping
    fun createPerson(@RequestBody request: CreatePersonRequestDto): ResponseEntity<PersonResponseDto> {
        val created = personService.createPerson(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{id}")
    fun getPerson(@PathVariable id: UUID): ResponseEntity<PersonResponseDto> {
        val person = personService.getPerson(id)
        return ResponseEntity.ok(person)
    }

    @PatchMapping("/{id}")
    fun updatePersonName(
        @PathVariable id: UUID,
        @RequestBody request: UpdatePersonRequestDto
    ): ResponseEntity<PersonResponseDto> {
        val updated = personService.updatePersonName(id, request)
        return ResponseEntity.ok(updated)
    }

    @GetMapping
    fun listPersons(): ResponseEntity<List<PersonResponseDto>> {
        val persons = personService.listPersons()
        return ResponseEntity.ok(persons)
    }

    @GetMapping("/{id}/faces")
    fun getPersonFaces(@PathVariable id: UUID): ResponseEntity<List<FaceRecordResponseDto>> {
        val faces = personService.getPersonFaces(id)
        return ResponseEntity.ok(faces)
    }
}

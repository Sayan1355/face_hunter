package com.photointelligence.config

import com.photointelligence.exception.PersonNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PersonNotFoundException::class)
    fun handlePersonNotFound(ex: PersonNotFoundException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "timestamp" to Instant.now().toString(),
            "status" to HttpStatus.NOT_FOUND.value(),
            "error" to "Not Found",
            "message" to (ex.message ?: "Person not found")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }
}

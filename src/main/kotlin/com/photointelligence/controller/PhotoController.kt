package com.photointelligence.controller

import com.photointelligence.dto.PhotoProcessResponseDto
import com.photointelligence.service.PhotoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/photos")
class PhotoController(
    private val photoService: PhotoService
) {

    @PostMapping("/process")
    fun processPhoto(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<PhotoProcessResponseDto> {
        val result = photoService.processPhoto(file)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }
}

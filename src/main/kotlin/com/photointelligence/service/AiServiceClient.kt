package com.photointelligence.service

import com.photointelligence.dto.AiIdentifyResponseDto
import com.photointelligence.exception.AiServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class AiServiceClient(
    @Value("\${ai.service.url:http://localhost:8001}")
    private val aiServiceBaseUrl: String
) {
    private val restTemplate = RestTemplate()

    fun identifyFaces(imageBytes: ByteArray, originalFilename: String): AiIdentifyResponseDto {
        try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.MULTIPART_FORM_DATA
            }

            val fileResource = object : ByteArrayResource(imageBytes) {
                override fun getFilename(): String = originalFilename.ifBlank { "photo.jpg" }
            }

            val body = LinkedMultiValueMap<String, Any>().apply {
                add("file", fileResource)
            }

            val requestEntity = HttpEntity(body, headers)
            val url = "$aiServiceBaseUrl/api/ai/identify"

            val response = restTemplate.postForEntity(url, requestEntity, AiIdentifyResponseDto::class.java)

            if (!response.statusCode.is2xxSuccessful || response.body == null) {
                throw AiServiceException("AI service returned error status: ${response.statusCode}")
            }

            return response.body!!
        } catch (ex: Exception) {
            if (ex is AiServiceException) throw ex
            throw AiServiceException("Failed to call AI face identification service: ${ex.message}", ex)
        }
    }

    fun registerEmbedding(personId: UUID, embedding: List<Double>) {
        try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val payload = mapOf(
                "person_id" to personId.toString(),
                "embedding" to embedding
            )

            val requestEntity = HttpEntity(payload, headers)
            val url = "$aiServiceBaseUrl/api/ai/register-embedding"

            restTemplate.postForEntity(url, requestEntity, String::class.java)
        } catch (ex: Exception) {
            // Log or ignore if AI service is not running during startup re-index
        }
    }
}

package com.photointelligence.config

import com.photointelligence.repository.FaceRecordRepository
import com.photointelligence.service.AiServiceClient
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class VectorIndexSyncRunner(
    private val faceRecordRepository: FaceRecordRepository,
    private val aiServiceClient: AiServiceClient
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(VectorIndexSyncRunner::class.java)

    override fun run(vararg args: String) {
        try {
            val recordsWithEmbeddings = faceRecordRepository.findAllByEmbeddingIsNotNull()
            var count = 0
            for (record in recordsWithEmbeddings) {
                val embJson = record.embedding ?: continue
                val personId = record.person.id ?: continue
                val vector = embJson.trim('[', ']').split(',').mapNotNull { it.trim().toDoubleOrNull() }
                if (vector.isNotEmpty()) {
                    aiServiceClient.registerEmbedding(personId, vector)
                    count++
                }
            }
            if (count > 0) {
                logger.info("Successfully re-indexed $count face embeddings into AI vector index from PostgreSQL database.")
            }
        } catch (ex: Exception) {
            logger.warn("Vector index sync skipped or failed during startup: ${ex.message}")
        }
    }
}

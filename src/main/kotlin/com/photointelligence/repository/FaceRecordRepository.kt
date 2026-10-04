package com.photointelligence.repository

import com.photointelligence.entity.FaceRecordEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FaceRecordRepository : JpaRepository<FaceRecordEntity, UUID> {
    fun findByPersonId(personId: UUID): List<FaceRecordEntity>
}

package com.photointelligence.repository

import com.photointelligence.entity.FaceRecordEntity
import com.photointelligence.entity.PhotoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FaceRecordRepository : JpaRepository<FaceRecordEntity, UUID> {
    fun findByPersonId(personId: UUID): List<FaceRecordEntity>
    
    fun countByPersonId(personId: UUID): Long
    
    @Query("SELECT DISTINCT fr.photo FROM FaceRecordEntity fr WHERE fr.person.id = :personId")
    fun findDistinctPhotosByPersonId(@Param("personId") personId: UUID): List<PhotoEntity>

    fun findAllByEmbeddingIsNotNull(): List<FaceRecordEntity>
}

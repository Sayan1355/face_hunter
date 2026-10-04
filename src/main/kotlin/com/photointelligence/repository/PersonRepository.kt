package com.photointelligence.repository

import com.photointelligence.entity.PersonEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PersonRepository : JpaRepository<PersonEntity, UUID> {
    fun findByNameContainingIgnoreCase(name: String): List<PersonEntity>
}

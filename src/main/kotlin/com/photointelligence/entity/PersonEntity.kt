package com.photointelligence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "persons")
class PersonEntity(
    @Id
    private var id: UUID? = null,

    @Column(name = "name")
    var name: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) : Persistable<UUID> {

    @Transient
    private var isNewRecord: Boolean = true

    override fun getId(): UUID? = id

    fun setId(newId: UUID?) {
        this.id = newId
    }

    override fun isNew(): Boolean = isNewRecord

    @PostLoad
    @PostPersist
    fun markNotNew() {
        isNewRecord = false
    }

    @PrePersist
    fun onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID()
        }
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}

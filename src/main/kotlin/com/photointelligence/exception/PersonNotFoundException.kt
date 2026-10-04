package com.photointelligence.exception

import java.util.UUID

class PersonNotFoundException(id: UUID) : RuntimeException("Person not found with ID: $id")

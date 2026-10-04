package com.photointelligence

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PhotoIntelligenceBackendApplication

fun main(args: Array<String>) {
	runApplication<PhotoIntelligenceBackendApplication>(*args)
}

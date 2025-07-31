package com.example.traderview.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class BrokerDto(
    val id: Long? = null,
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotBlank(message = "Display name is required")
    val displayName: String,
    @field:NotBlank(message = "API endpoint is required")
    val apiEndpoint: String,
    @field:NotNull(message = "Active status is required")
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class CreateBrokerRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotBlank(message = "Display name is required")
    val displayName: String,
    @field:NotBlank(message = "API endpoint is required")
    val apiEndpoint: String
)

data class UpdateBrokerRequest(
    val displayName: String? = null,
    val apiEndpoint: String? = null,
    val isActive: Boolean? = null
)

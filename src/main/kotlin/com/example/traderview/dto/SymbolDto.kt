package com.example.traderview.dto

import com.example.traderview.entity.AssetType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

data class SymbolDto(
    val id: Long? = null,
    @field:NotBlank(message = "Ticker is required")
    val ticker: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotBlank(message = "Exchange is required")
    val exchange: String,
    @field:NotNull(message = "Asset type is required")
    val assetType: AssetType,
    val currency: String = "USD",
    val contractMultiplier: BigDecimal = BigDecimal.ONE,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class CreateSymbolRequest(
    @field:NotBlank(message = "Ticker is required")
    val ticker: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotBlank(message = "Exchange is required")
    val exchange: String,
    @field:NotNull(message = "Asset type is required")
    val assetType: AssetType,
    val currency: String = "USD"
)

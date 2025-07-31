package com.example.traderview.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

data class TradingAccountDto(
    val id: Long? = null,
    @field:NotBlank(message = "Account number is required")
    val accountNumber: String,
    @field:NotBlank(message = "Account name is required")
    val accountName: String,
    @field:NotNull(message = "Broker ID is required")
    val brokerId: Long,
    val brokerName: String? = null,
    @field:NotNull(message = "Initial balance is required")
    @field:Positive(message = "Initial balance must be positive")
    val initialBalance: BigDecimal,
    @field:NotNull(message = "Current balance is required")
    val currentBalance: BigDecimal,
    val currency: String = "USD",
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class CreateTradingAccountRequest(
    @field:NotBlank(message = "Account number is required")
    val accountNumber: String,
    @field:NotBlank(message = "Account name is required")
    val accountName: String,
    @field:NotNull(message = "Broker ID is required")
    val brokerId: Long,
    @field:NotNull(message = "Initial balance is required")
    @field:Positive(message = "Initial balance must be positive")
    val initialBalance: BigDecimal,
    val currency: String = "USD"
)

data class UpdateTradingAccountRequest(
    val accountName: String? = null,
    val currentBalance: BigDecimal? = null,
    val isActive: Boolean? = null
)

package com.example.traderview.dto

import com.example.traderview.entity.OrderSide
import com.example.traderview.entity.OrderStatus
import com.example.traderview.entity.OrderType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderDto(
    val id: Long? = null,
    @field:NotBlank(message = "Broker order ID is required")
    val brokerOrderId: String,
    @field:NotNull(message = "Trading account ID is required")
    val tradingAccountId: Long,
    val tradingAccountName: String? = null,
    @field:NotNull(message = "Symbol ID is required")
    val symbolId: Long,
    val symbolTicker: String? = null,
    @field:NotNull(message = "Order side is required")
    val side: OrderSide,
    @field:NotNull(message = "Order type is required")
    val type: OrderType,
    @field:NotNull(message = "Order status is required")
    val status: OrderStatus,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal,
    val filledQuantity: BigDecimal? = null,
    val price: BigDecimal? = null,
    val avgFillPrice: BigDecimal? = null,
    val commission: BigDecimal? = null,
    val fees: BigDecimal? = null,
    @field:NotNull(message = "Placed at time is required")
    val placedAt: LocalDateTime,
    val filledAt: LocalDateTime? = null,
    val cancelledAt: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    // New fields for enhanced order list
    val realizedPnl: BigDecimal? = null,
    val avgTradesPerDay: BigDecimal? = null,
    val lastTradedDate: LocalDateTime? = null
)

data class CreateOrderRequest(
    @field:NotBlank(message = "Broker order ID is required")
    val brokerOrderId: String,
    @field:NotNull(message = "Trading account ID is required")
    val tradingAccountId: Long,
    @field:NotNull(message = "Symbol ID is required")
    val symbolId: Long,
    @field:NotNull(message = "Order side is required")
    val side: OrderSide,
    @field:NotNull(message = "Order type is required")
    val type: OrderType,
    @field:NotNull(message = "Order status is required")
    val status: OrderStatus,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal,
    val filledQuantity: BigDecimal? = null,
    val price: BigDecimal? = null,
    val avgFillPrice: BigDecimal? = null,
    val commission: BigDecimal? = null,
    val fees: BigDecimal? = null,
    @field:NotNull(message = "Placed at time is required")
    val placedAt: LocalDateTime,
    val filledAt: LocalDateTime? = null
)

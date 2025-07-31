package com.example.traderview.dto

import com.example.traderview.entity.AssetType
import java.math.BigDecimal
import java.time.LocalDateTime

data class SymbolWithStatsDto(
    val id: Long,
    val ticker: String,
    val name: String,
    val exchange: String,
    val assetType: AssetType,
    val currency: String,
    val contractMultiplier: BigDecimal,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val tradesThisWeek: Int
)

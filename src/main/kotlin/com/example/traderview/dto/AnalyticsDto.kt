package com.example.traderview.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class DashboardSummaryDto(
    val totalTrades: Long,
    val openTrades: Long,
    val closedTrades: Long,
    val totalRealizedPnl: BigDecimal,
    val totalUnrealizedPnl: BigDecimal,
    val winningTrades: Long,
    val losingTrades: Long,
    val winRate: BigDecimal,
    val totalCommission: BigDecimal,
    val totalFees: BigDecimal,
    val accountBalance: BigDecimal,
    val periodStart: LocalDate,
    val periodEnd: LocalDate
)

data class SymbolPerformanceDto(
    val symbolId: Long,
    val symbolTicker: String,
    val symbolName: String,
    val totalTrades: Long,
    val totalRealizedPnl: BigDecimal,
    val winningTrades: Long,
    val losingTrades: Long,
    val winRate: BigDecimal,
    val avgTradeSize: BigDecimal,
    val totalVolume: BigDecimal
)

data class DailyPnlDto(
    val date: LocalDate,
    val realizedPnl: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val totalPnl: BigDecimal,
    val tradesCount: Long,
    val volume: BigDecimal
)

data class TradingHoursAnalysisDto(
    val hour: Int,
    val tradesCount: Long,
    val avgRealizedPnl: BigDecimal,
    val totalVolume: BigDecimal,
    val winRate: BigDecimal
)

data class MonthlyPerformanceDto(
    val year: Int,
    val month: Int,
    val totalTrades: Long,
    val realizedPnl: BigDecimal,
    val winningTrades: Long,
    val losingTrades: Long,
    val winRate: BigDecimal,
    val totalVolume: BigDecimal
)

data class TradeAnalyticsRequest(
    val tradingAccountId: Long? = null,
    val symbolId: Long? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
)

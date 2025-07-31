package com.example.traderview.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TradeStatsDto(
    val totalGainLoss: BigDecimal,
    val largestGain: BigDecimal,
    val largestLoss: BigDecimal,
    val averageDailyGainLoss: BigDecimal,
    val averageDailyVolume: BigDecimal,
    val averagePerShareGainLoss: BigDecimal,
    val averageTradeGainLoss: BigDecimal,
    val averageWinningTrade: BigDecimal,
    val averageLosingTrade: BigDecimal,
    val totalNumberOfTrades: Long,
    val numberOfWinningTrades: Long,
    val numberOfLosingTrades: Long,
    val averageHoldTimeScratches: Long, // in minutes
    val averageHoldTimeWinning: Long,
    val averageHoldTimeLosingTrades: Long,
    val numberOfScratchTrades: Long,
    val maxConsecutiveWins: Long,
    val maxConsecutiveLosses: Long,
    val tradePnlStandardDeviation: BigDecimal,
    val systemQualityNumber: BigDecimal,
    val probabilityOfRandomChance: BigDecimal,
    val kellyPercentage: BigDecimal,
    val kRatio: BigDecimal,
    val profitFactor: BigDecimal,
    val totalCommissions: BigDecimal,
    val totalFees: BigDecimal,
    val averagePositionMae: BigDecimal, // Maximum Adverse Excursion
    val averagePositionMfe: BigDecimal  // Maximum Favorable Excursion
)

data class WinLossDaysDto(
    val winningDays: WinLossDayStatsDto,
    val losingDays: WinLossDayStatsDto
)

data class WinLossDayStatsDto(
    val totalGainLoss: BigDecimal,
    val averageDailyGainLoss: BigDecimal,
    val averageDailyVolume: BigDecimal,
    val averagePerShareGainLoss: BigDecimal,
    val averageTradeGainLoss: BigDecimal,
    val numberOfDays: Long,
    val totalTrades: Long
)

data class DrawdownAnalysisDto(
    val maxDrawdown: BigDecimal,
    val maxDrawdownPercent: BigDecimal,
    val maxDrawdownDuration: Long, // in days
    val currentDrawdown: BigDecimal,
    val currentDrawdownPercent: BigDecimal,
    val currentDrawdownDuration: Long,
    val drawdownPeriods: List<DrawdownPeriodDto>
)

data class DrawdownPeriodDto(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val peakValue: BigDecimal,
    val troughValue: BigDecimal,
    val drawdownAmount: BigDecimal,
    val drawdownPercent: BigDecimal,
    val durationDays: Long,
    val recoveryDate: LocalDate?
)

data class TagBreakdownDto(
    val tag: String,
    val totalTrades: Long,
    val winningTrades: Long,
    val losingTrades: Long,
    val totalPnl: BigDecimal,
    val winRate: BigDecimal,
    val averageWin: BigDecimal,
    val averageLoss: BigDecimal,
    val profitFactor: BigDecimal
)

data class DurationAnalysisDto(
    val durationCategory: String, // "Scalp", "Intraday", "Swing", "Position"
    val minDurationMinutes: Long,
    val maxDurationMinutes: Long,
    val totalTrades: Long,
    val winningTrades: Long,
    val losingTrades: Long,
    val totalPnl: BigDecimal,
    val winRate: BigDecimal,
    val averageHoldTime: Long,
    val profitFactor: BigDecimal
)

data class IntradayAnalysisDto(
    val date: LocalDate,
    val totalTrades: Long,
    val winningTrades: Long,
    val losingTrades: Long,
    val scratchTrades: Long,
    val totalPnl: BigDecimal,
    val grossPnl: BigDecimal,
    val commissions: BigDecimal,
    val fees: BigDecimal,
    val largestWin: BigDecimal,
    val largestLoss: BigDecimal,
    val firstTradeTime: LocalDateTime?,
    val lastTradeTime: LocalDateTime?,
    val tradingDuration: Long, // in minutes
    val averageTradeSize: BigDecimal,
    val totalVolume: BigDecimal
)

data class AdvancedFilterRequest(
    val tradingAccountId: Long,
    val symbols: List<String>? = null,
    val tags: List<String>? = null,
    val sides: List<String>? = null, // BUY, SELL
    val durations: List<String>? = null, // SCALP, INTRADAY, SWING, POSITION
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val minPnl: BigDecimal? = null,
    val maxPnl: BigDecimal? = null,
    val minQuantity: BigDecimal? = null,
    val maxQuantity: BigDecimal? = null
)

data class ComparisonAnalysisDto(
    val period1: ComparisonPeriodDto,
    val period2: ComparisonPeriodDto,
    val improvements: List<ComparisonMetricDto>
)

data class ComparisonPeriodDto(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalTrades: Long,
    val totalPnl: BigDecimal,
    val winRate: BigDecimal,
    val profitFactor: BigDecimal,
    val averageDailyPnl: BigDecimal,
    val maxDrawdown: BigDecimal,
    val sharpeRatio: BigDecimal
)

data class ComparisonMetricDto(
    val metric: String,
    val period1Value: BigDecimal,
    val period2Value: BigDecimal,
    val changePercent: BigDecimal,
    val improvement: Boolean
)

package com.example.traderview.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class TradeImportRequest(
    @field:NotNull(message = "Trading account ID is required")
    val tradingAccountId: Long,
    @field:NotBlank(message = "Contract symbol is required")
    val contractSymbol: String,
    val contractName: String? = null,
    val exchange: String = "CME",
    val trades: List<TradeImportRecord>
)

data class TradeImportRecord(
    @field:NotNull(message = "Date is required")
    @JsonFormat(pattern = "MM/dd/yyyy")
    val date: LocalDate,
    @field:NotNull(message = "Buy quantity is required")
    val buy: BigDecimal,
    @field:NotNull(message = "Sell quantity is required")
    val sell: BigDecimal,
    @field:NotNull(message = "Price is required")
    val price: BigDecimal,
    val currency: String = "USD",
    val exchangeFees: BigDecimal = BigDecimal.ZERO,
    val nfaFees: BigDecimal = BigDecimal.ZERO,
    val commission: BigDecimal = BigDecimal.ZERO,
    val conversionRate: BigDecimal = BigDecimal.ONE
)

data class TradeImportResponse(
    val success: Boolean,
    val message: String,
    val importedOrders: Int,
    val importedTrades: Int,
    val errors: List<String> = emptyList()
)

data class FuturesTradeAnalysis(
    val contractSymbol: String,
    val totalTrades: Int,
    val totalVolume: BigDecimal,
    val totalCommissions: BigDecimal,
    val totalFees: BigDecimal,
    val netPnl: BigDecimal,
    val avgTradeSize: BigDecimal,
    val tradingDays: Int,
    val firstTradeDate: LocalDate,
    val lastTradeDate: LocalDate,
    val dailyBreakdown: List<DailyTradeBreakdown>
)

data class DailyTradeBreakdown(
    val date: LocalDate,
    val totalBuy: BigDecimal,
    val totalSell: BigDecimal,
    val netPosition: BigDecimal,
    val avgBuyPrice: BigDecimal?,
    val avgSellPrice: BigDecimal?,
    val dailyPnl: BigDecimal,
    val totalCommissions: BigDecimal,
    val totalFees: BigDecimal,
    val tradeCount: Int
)

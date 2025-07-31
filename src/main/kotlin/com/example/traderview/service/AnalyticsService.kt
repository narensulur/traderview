package com.example.traderview.service

import com.example.traderview.dto.*
import com.example.traderview.entity.TradeStatus
import com.example.traderview.repository.OrderRepository
import com.example.traderview.repository.TradeRepository
import com.example.traderview.repository.TradingAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AnalyticsService(
    private val tradeRepository: TradeRepository,
    private val orderRepository: OrderRepository,
    private val tradingAccountRepository: TradingAccountRepository
) {
    
    fun getDashboardSummary(
        tradingAccountId: Long,
        startDate: LocalDateTime = LocalDateTime.now().minusMonths(1),
        endDate: LocalDateTime = LocalDateTime.now()
    ): DashboardSummaryDto {
        val trades = tradeRepository.findByTradingAccountIdAndEntryTimeBetween(
            tradingAccountId, startDate, endDate
        )
        
        val totalTrades = trades.size.toLong()
        val openTrades = trades.count { it.status == TradeStatus.OPEN }.toLong()
        val closedTrades = trades.count { it.status == TradeStatus.CLOSED }.toLong()
        
        val totalRealizedPnl = trades
            .filter { it.status == TradeStatus.CLOSED }
            .sumOf { it.realizedPnl ?: BigDecimal.ZERO }
        
        val totalUnrealizedPnl = BigDecimal.ZERO // Would need current market prices to calculate
        
        val winningTrades = trades.count { 
            it.status == TradeStatus.CLOSED && (it.realizedPnl ?: BigDecimal.ZERO) > BigDecimal.ZERO 
        }.toLong()
        
        val losingTrades = trades.count { 
            it.status == TradeStatus.CLOSED && (it.realizedPnl ?: BigDecimal.ZERO) < BigDecimal.ZERO 
        }.toLong()
        
        val winRate = if (closedTrades > 0) {
            BigDecimal(winningTrades).divide(BigDecimal(closedTrades), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        val totalCommission = trades.sumOf { it.totalCommission }
        val totalFees = trades.sumOf { it.totalFees }
        
        val account = tradingAccountRepository.findById(tradingAccountId).orElse(null)
        val accountBalance = account?.currentBalance ?: BigDecimal.ZERO
        
        return DashboardSummaryDto(
            totalTrades = totalTrades,
            openTrades = openTrades,
            closedTrades = closedTrades,
            totalRealizedPnl = totalRealizedPnl,
            totalUnrealizedPnl = totalUnrealizedPnl,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRate = winRate,
            totalCommission = totalCommission,
            totalFees = totalFees,
            accountBalance = accountBalance,
            periodStart = startDate.toLocalDate(),
            periodEnd = endDate.toLocalDate()
        )
    }
    
    fun getSymbolPerformance(
        tradingAccountId: Long,
        startDate: LocalDateTime = LocalDateTime.now().minusMonths(1),
        endDate: LocalDateTime = LocalDateTime.now()
    ): List<SymbolPerformanceDto> {
        val trades = tradeRepository.findByTradingAccountIdAndEntryTimeBetween(
            tradingAccountId, startDate, endDate
        )
        
        return trades.groupBy { it.symbol }
            .map { (symbol, symbolTrades) ->
                val totalTrades = symbolTrades.size.toLong()
                val closedTrades = symbolTrades.filter { it.status == TradeStatus.CLOSED }
                
                val totalRealizedPnl = closedTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
                
                val winningTrades = closedTrades.count { 
                    (it.realizedPnl ?: BigDecimal.ZERO) > BigDecimal.ZERO 
                }.toLong()
                
                val losingTrades = closedTrades.count { 
                    (it.realizedPnl ?: BigDecimal.ZERO) < BigDecimal.ZERO 
                }.toLong()
                
                val winRate = if (closedTrades.isNotEmpty()) {
                    BigDecimal(winningTrades).divide(BigDecimal(closedTrades.size), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(100))
                } else {
                    BigDecimal.ZERO
                }
                
                val totalVolume = symbolTrades.sumOf { it.entryValue }
                val avgTradeSize = if (totalTrades > 0) {
                    totalVolume.divide(BigDecimal(totalTrades), 2, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }
                
                SymbolPerformanceDto(
                    symbolId = symbol.id,
                    symbolTicker = symbol.ticker,
                    symbolName = symbol.name,
                    totalTrades = totalTrades,
                    totalRealizedPnl = totalRealizedPnl,
                    winningTrades = winningTrades,
                    losingTrades = losingTrades,
                    winRate = winRate,
                    avgTradeSize = avgTradeSize,
                    totalVolume = totalVolume
                )
            }
            .sortedByDescending { it.totalRealizedPnl }
    }
    
    fun getDailyPnl(
        tradingAccountId: Long,
        startDate: LocalDateTime = LocalDateTime.now().minusMonths(1),
        endDate: LocalDateTime = LocalDateTime.now()
    ): List<DailyPnlDto> {
        val trades = tradeRepository.findByTradingAccountIdAndEntryTimeBetween(
            tradingAccountId, startDate, endDate
        )
        
        return trades
            .filter { it.status == TradeStatus.CLOSED && it.exitTime != null }
            .groupBy { it.exitTime!!.toLocalDate() }
            .map { (date, dayTrades) ->
                val realizedPnl = dayTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
                val unrealizedPnl = BigDecimal.ZERO // Would need current market prices
                val totalPnl = realizedPnl.add(unrealizedPnl)
                val tradesCount = dayTrades.size.toLong()
                val volume = dayTrades.sumOf { it.entryValue }
                
                DailyPnlDto(
                    date = date,
                    realizedPnl = realizedPnl,
                    unrealizedPnl = unrealizedPnl,
                    totalPnl = totalPnl,
                    tradesCount = tradesCount,
                    volume = volume
                )
            }
            .sortedBy { it.date }
    }
}

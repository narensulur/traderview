package com.example.traderview.service

import com.example.traderview.dto.*
import com.example.traderview.entity.Trade
import com.example.traderview.entity.TradeStatus
import com.example.traderview.repository.TradeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*

@Service
@Transactional(readOnly = true)
class AdvancedAnalyticsService(
    private val tradeRepository: TradeRepository
) {
    
    fun getTradeStats(tradingAccountId: Long, filters: AdvancedFilterRequest? = null): TradeStatsDto {
        val trades = getFilteredTrades(tradingAccountId, filters)
        
        if (trades.isEmpty()) {
            return createEmptyTradeStats()
        }
        
        val winningTrades = trades.filter { (it.realizedPnl ?: BigDecimal.ZERO) > BigDecimal.ZERO }
        val losingTrades = trades.filter { (it.realizedPnl ?: BigDecimal.ZERO) < BigDecimal.ZERO }
        val scratchTrades = trades.filter { (it.realizedPnl ?: BigDecimal.ZERO) == BigDecimal.ZERO }
        
        val totalPnl = trades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
        val totalCommissions = trades.sumOf { it.totalCommission }
        val totalFees = trades.sumOf { it.totalFees }
        
        val pnlValues = trades.mapNotNull { it.realizedPnl }
        val largestGain = pnlValues.maxOrNull() ?: BigDecimal.ZERO
        val largestLoss = pnlValues.minOrNull() ?: BigDecimal.ZERO
        
        val tradingDays = trades.map { it.entryTime.toLocalDate() }.distinct().size
        val averageDailyPnl = if (tradingDays > 0) totalPnl.divide(BigDecimal(tradingDays), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        
        val averageWin = if (winningTrades.isNotEmpty()) {
            winningTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }.divide(BigDecimal(winningTrades.size), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        
        val averageLoss = if (losingTrades.isNotEmpty()) {
            losingTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }.divide(BigDecimal(losingTrades.size), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        
        val consecutiveWins = calculateMaxConsecutiveWins(trades)
        val consecutiveLosses = calculateMaxConsecutiveLosses(trades)
        
        val pnlStdDev = calculateStandardDeviation(pnlValues)
        val profitFactor = calculateProfitFactor(winningTrades, losingTrades)
        val sharpeRatio = calculateSharpeRatio(pnlValues)
        
        return TradeStatsDto(
            totalGainLoss = totalPnl,
            largestGain = largestGain,
            largestLoss = largestLoss,
            averageDailyGainLoss = averageDailyPnl,
            averageDailyVolume = calculateAverageDailyVolume(trades, tradingDays),
            averagePerShareGainLoss = calculateAveragePerSharePnl(trades),
            averageTradeGainLoss = if (trades.isNotEmpty()) totalPnl.divide(BigDecimal(trades.size), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO,
            averageWinningTrade = averageWin,
            averageLosingTrade = averageLoss,
            totalNumberOfTrades = trades.size.toLong(),
            numberOfWinningTrades = winningTrades.size.toLong(),
            numberOfLosingTrades = losingTrades.size.toLong(),
            averageHoldTimeScratches = calculateAverageHoldTime(scratchTrades),
            averageHoldTimeWinning = calculateAverageHoldTime(winningTrades),
            averageHoldTimeLosingTrades = calculateAverageHoldTime(losingTrades),
            numberOfScratchTrades = scratchTrades.size.toLong(),
            maxConsecutiveWins = consecutiveWins,
            maxConsecutiveLosses = consecutiveLosses,
            tradePnlStandardDeviation = pnlStdDev,
            systemQualityNumber = calculateSystemQualityNumber(pnlValues),
            probabilityOfRandomChance = calculateProbabilityOfRandomChance(winningTrades.size, trades.size),
            kellyPercentage = calculateKellyPercentage(winningTrades, losingTrades),
            kRatio = calculateKRatio(pnlValues),
            profitFactor = profitFactor,
            totalCommissions = totalCommissions,
            totalFees = totalFees,
            averagePositionMae = BigDecimal.ZERO, // TODO: Implement MAE calculation
            averagePositionMfe = BigDecimal.ZERO  // TODO: Implement MFE calculation
        )
    }
    
    fun getWinLossDaysAnalysis(tradingAccountId: Long, filters: AdvancedFilterRequest? = null): WinLossDaysDto {
        val trades = getFilteredTrades(tradingAccountId, filters)
        
        val dailyPnl = trades.groupBy { it.entryTime.toLocalDate() }
            .mapValues { (_, dayTrades) -> 
                dayTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
            }
        
        val winningDays = dailyPnl.filter { it.value > BigDecimal.ZERO }
        val losingDays = dailyPnl.filter { it.value < BigDecimal.ZERO }
        
        return WinLossDaysDto(
            winningDays = calculateDayStats(winningDays, trades, true),
            losingDays = calculateDayStats(losingDays, trades, false)
        )
    }
    
    fun getDrawdownAnalysis(tradingAccountId: Long, filters: AdvancedFilterRequest? = null): DrawdownAnalysisDto {
        val trades = getFilteredTrades(tradingAccountId, filters).sortedBy { it.entryTime }
        
        if (trades.isEmpty()) {
            return DrawdownAnalysisDto(
                maxDrawdown = BigDecimal.ZERO,
                maxDrawdownPercent = BigDecimal.ZERO,
                maxDrawdownDuration = 0,
                currentDrawdown = BigDecimal.ZERO,
                currentDrawdownPercent = BigDecimal.ZERO,
                currentDrawdownDuration = 0,
                drawdownPeriods = emptyList()
            )
        }
        
        val cumulativePnl = mutableListOf<Pair<LocalDate, BigDecimal>>()
        var runningPnl = BigDecimal.ZERO
        
        trades.groupBy { it.entryTime.toLocalDate() }
            .toSortedMap()
            .forEach { (date, dayTrades) ->
                runningPnl += dayTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
                cumulativePnl.add(date to runningPnl)
            }
        
        val drawdownPeriods = calculateDrawdownPeriods(cumulativePnl)
        val maxDrawdown = drawdownPeriods.maxByOrNull { it.drawdownAmount }
        
        return DrawdownAnalysisDto(
            maxDrawdown = maxDrawdown?.drawdownAmount ?: BigDecimal.ZERO,
            maxDrawdownPercent = maxDrawdown?.drawdownPercent ?: BigDecimal.ZERO,
            maxDrawdownDuration = maxDrawdown?.durationDays ?: 0,
            currentDrawdown = calculateCurrentDrawdown(cumulativePnl),
            currentDrawdownPercent = calculateCurrentDrawdownPercent(cumulativePnl),
            currentDrawdownDuration = calculateCurrentDrawdownDuration(cumulativePnl),
            drawdownPeriods = drawdownPeriods
        )
    }
    
    fun getIntradayAnalysis(tradingAccountId: Long, date: LocalDate): List<IntradayAnalysisDto> {
        val trades = tradeRepository.findByTradingAccountId(tradingAccountId)
            .filter { it.entryTime.toLocalDate() == date }
        
        return trades.groupBy { it.entryTime.toLocalDate() }
            .map { (tradeDate, dayTrades) ->
                val winningTrades = dayTrades.filter { (it.realizedPnl ?: BigDecimal.ZERO) > BigDecimal.ZERO }
                val losingTrades = dayTrades.filter { (it.realizedPnl ?: BigDecimal.ZERO) < BigDecimal.ZERO }
                val scratchTrades = dayTrades.filter { (it.realizedPnl ?: BigDecimal.ZERO) == BigDecimal.ZERO }
                
                val totalPnl = dayTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
                val commissions = dayTrades.sumOf { it.totalCommission }
                val fees = dayTrades.sumOf { it.totalFees }
                
                val firstTrade = dayTrades.minByOrNull { it.entryTime }
                val lastTrade = dayTrades.maxByOrNull { it.exitTime ?: it.entryTime }
                
                val tradingDuration = if (firstTrade != null && lastTrade?.exitTime != null) {
                    ChronoUnit.MINUTES.between(firstTrade.entryTime, lastTrade.exitTime)
                } else 0L
                
                IntradayAnalysisDto(
                    date = tradeDate,
                    totalTrades = dayTrades.size.toLong(),
                    winningTrades = winningTrades.size.toLong(),
                    losingTrades = losingTrades.size.toLong(),
                    scratchTrades = scratchTrades.size.toLong(),
                    totalPnl = totalPnl,
                    grossPnl = totalPnl + commissions + fees,
                    commissions = commissions,
                    fees = fees,
                    largestWin = dayTrades.mapNotNull { it.realizedPnl }.maxOrNull() ?: BigDecimal.ZERO,
                    largestLoss = dayTrades.mapNotNull { it.realizedPnl }.minOrNull() ?: BigDecimal.ZERO,
                    firstTradeTime = firstTrade?.entryTime,
                    lastTradeTime = lastTrade?.exitTime,
                    tradingDuration = tradingDuration,
                    averageTradeSize = if (dayTrades.isNotEmpty()) {
                        dayTrades.sumOf { it.entryValue }.divide(BigDecimal(dayTrades.size), 2, RoundingMode.HALF_UP)
                    } else BigDecimal.ZERO,
                    totalVolume = dayTrades.sumOf { it.quantity }
                )
            }
    }
    
    private fun getFilteredTrades(tradingAccountId: Long, filters: AdvancedFilterRequest?): List<Trade> {
        var trades = tradeRepository.findByTradingAccountId(tradingAccountId)
            .filter { it.status == TradeStatus.CLOSED }
        
        filters?.let { filter ->
            filter.startDate?.let { start ->
                trades = trades.filter { it.entryTime >= start }
            }
            filter.endDate?.let { end ->
                trades = trades.filter { it.entryTime <= end }
            }
            filter.symbols?.let { symbols ->
                trades = trades.filter { trade -> symbols.contains(trade.symbol.ticker) }
            }
            filter.minPnl?.let { min ->
                trades = trades.filter { (it.realizedPnl ?: BigDecimal.ZERO) >= min }
            }
            filter.maxPnl?.let { max ->
                trades = trades.filter { (it.realizedPnl ?: BigDecimal.ZERO) <= max }
            }
        }
        
        return trades
    }
    
    // Helper methods for calculations
    private fun createEmptyTradeStats() = TradeStatsDto(
        totalGainLoss = BigDecimal.ZERO,
        largestGain = BigDecimal.ZERO,
        largestLoss = BigDecimal.ZERO,
        averageDailyGainLoss = BigDecimal.ZERO,
        averageDailyVolume = BigDecimal.ZERO,
        averagePerShareGainLoss = BigDecimal.ZERO,
        averageTradeGainLoss = BigDecimal.ZERO,
        averageWinningTrade = BigDecimal.ZERO,
        averageLosingTrade = BigDecimal.ZERO,
        totalNumberOfTrades = 0,
        numberOfWinningTrades = 0,
        numberOfLosingTrades = 0,
        averageHoldTimeScratches = 0,
        averageHoldTimeWinning = 0,
        averageHoldTimeLosingTrades = 0,
        numberOfScratchTrades = 0,
        maxConsecutiveWins = 0,
        maxConsecutiveLosses = 0,
        tradePnlStandardDeviation = BigDecimal.ZERO,
        systemQualityNumber = BigDecimal.ZERO,
        probabilityOfRandomChance = BigDecimal.ZERO,
        kellyPercentage = BigDecimal.ZERO,
        kRatio = BigDecimal.ZERO,
        profitFactor = BigDecimal.ZERO,
        totalCommissions = BigDecimal.ZERO,
        totalFees = BigDecimal.ZERO,
        averagePositionMae = BigDecimal.ZERO,
        averagePositionMfe = BigDecimal.ZERO
    )
    
    private fun calculateMaxConsecutiveWins(trades: List<Trade>): Long {
        var maxConsecutive = 0L
        var currentConsecutive = 0L
        
        trades.sortedBy { it.entryTime }.forEach { trade ->
            if ((trade.realizedPnl ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                currentConsecutive++
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }
        
        return maxConsecutive
    }
    
    private fun calculateMaxConsecutiveLosses(trades: List<Trade>): Long {
        var maxConsecutive = 0L
        var currentConsecutive = 0L
        
        trades.sortedBy { it.entryTime }.forEach { trade ->
            if ((trade.realizedPnl ?: BigDecimal.ZERO) < BigDecimal.ZERO) {
                currentConsecutive++
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }
        
        return maxConsecutive
    }
    
    private fun calculateStandardDeviation(values: List<BigDecimal>): BigDecimal {
        if (values.isEmpty()) return BigDecimal.ZERO
        
        val mean = values.sumOf { it }.divide(BigDecimal(values.size), MathContext.DECIMAL128)
        val variance = values.sumOf { value ->
            val diff = value.subtract(mean)
            diff.multiply(diff)
        }.divide(BigDecimal(values.size), MathContext.DECIMAL128)
        
        return BigDecimal(sqrt(variance.toDouble()))
    }
    
    private fun calculateProfitFactor(winningTrades: List<Trade>, losingTrades: List<Trade>): BigDecimal {
        val totalWins = winningTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
        val totalLosses = losingTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }.abs()
        
        return if (totalLosses > BigDecimal.ZERO) {
            totalWins.divide(totalLosses, 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    
    private fun calculateSharpeRatio(pnlValues: List<BigDecimal>): BigDecimal {
        if (pnlValues.isEmpty()) return BigDecimal.ZERO
        
        val mean = pnlValues.sumOf { it }.divide(BigDecimal(pnlValues.size), MathContext.DECIMAL128)
        val stdDev = calculateStandardDeviation(pnlValues)
        
        return if (stdDev > BigDecimal.ZERO) {
            mean.divide(stdDev, 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    
    private fun calculateAverageDailyVolume(trades: List<Trade>, tradingDays: Int): BigDecimal {
        val totalVolume = trades.sumOf { it.entryValue }
        return if (tradingDays > 0) {
            totalVolume.divide(BigDecimal(tradingDays), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    
    private fun calculateAveragePerSharePnl(trades: List<Trade>): BigDecimal {
        val totalShares = trades.sumOf { it.quantity }
        val totalPnl = trades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }
        
        return if (totalShares > BigDecimal.ZERO) {
            totalPnl.divide(totalShares, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    
    private fun calculateAverageHoldTime(trades: List<Trade>): Long {
        if (trades.isEmpty()) return 0L
        
        val holdTimes = trades.mapNotNull { trade ->
            trade.exitTime?.let { exit ->
                ChronoUnit.MINUTES.between(trade.entryTime, exit)
            }
        }
        
        return if (holdTimes.isNotEmpty()) {
            holdTimes.average().toLong()
        } else 0L
    }
    
    private fun calculateSystemQualityNumber(pnlValues: List<BigDecimal>): BigDecimal {
        // Simplified SQN calculation
        if (pnlValues.isEmpty()) return BigDecimal.ZERO
        
        val mean = pnlValues.sumOf { it }.divide(BigDecimal(pnlValues.size), MathContext.DECIMAL128)
        val stdDev = calculateStandardDeviation(pnlValues)
        
        return if (stdDev > BigDecimal.ZERO) {
            mean.multiply(BigDecimal(sqrt(pnlValues.size.toDouble()))).divide(stdDev, 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    
    private fun calculateProbabilityOfRandomChance(wins: Int, totalTrades: Int): BigDecimal {
        // Simplified calculation - in practice would use binomial distribution
        if (totalTrades == 0) return BigDecimal.ZERO
        
        val winRate = BigDecimal(wins).divide(BigDecimal(totalTrades), 4, RoundingMode.HALF_UP)
        return BigDecimal.ONE.subtract(winRate).multiply(BigDecimal(100))
    }
    
    private fun calculateKellyPercentage(winningTrades: List<Trade>, losingTrades: List<Trade>): BigDecimal {
        if (winningTrades.isEmpty() || losingTrades.isEmpty()) return BigDecimal.ZERO
        
        val avgWin = winningTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }.divide(BigDecimal(winningTrades.size), MathContext.DECIMAL128)
        val avgLoss = losingTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }.abs().divide(BigDecimal(losingTrades.size), MathContext.DECIMAL128)
        val winRate = BigDecimal(winningTrades.size).divide(BigDecimal(winningTrades.size + losingTrades.size), MathContext.DECIMAL128)
        
        // Kelly % = (bp - q) / b where b = avg win / avg loss, p = win rate, q = loss rate
        val b = if (avgLoss > BigDecimal.ZERO) avgWin.divide(avgLoss, MathContext.DECIMAL128) else BigDecimal.ZERO
        val q = BigDecimal.ONE.subtract(winRate)
        
        return if (b > BigDecimal.ZERO) {
            (b.multiply(winRate).subtract(q)).divide(b, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else BigDecimal.ZERO
    }
    
    private fun calculateKRatio(pnlValues: List<BigDecimal>): BigDecimal {
        // Simplified K-Ratio calculation
        if (pnlValues.size < 2) return BigDecimal.ZERO
        
        val slope = calculateLinearRegressionSlope(pnlValues)
        val stdError = calculateStandardError(pnlValues, slope)
        
        return if (stdError > BigDecimal.ZERO) {
            slope.divide(stdError, 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    
    private fun calculateLinearRegressionSlope(values: List<BigDecimal>): BigDecimal {
        val n = values.size
        val xSum = (1..n).sum()
        val ySum = values.sumOf { it }
        val xySum = values.mapIndexed { index, value -> BigDecimal(index + 1).multiply(value) }.sumOf { it }
        val xSquaredSum = (1..n).sumOf { it * it }
        
        val numerator = BigDecimal(n).multiply(xySum).subtract(BigDecimal(xSum).multiply(ySum))
        val denominator = BigDecimal(n).multiply(BigDecimal(xSquaredSum)).subtract(BigDecimal(xSum).multiply(BigDecimal(xSum)))
        
        return if (denominator != BigDecimal.ZERO) {
            numerator.divide(denominator, MathContext.DECIMAL128)
        } else BigDecimal.ZERO
    }
    
    private fun calculateStandardError(values: List<BigDecimal>, slope: BigDecimal): BigDecimal {
        // Simplified standard error calculation
        return calculateStandardDeviation(values).divide(BigDecimal(sqrt(values.size.toDouble())), MathContext.DECIMAL128)
    }
    
    private fun calculateDayStats(days: Map<LocalDate, BigDecimal>, allTrades: List<Trade>, isWinning: Boolean): WinLossDayStatsDto {
        if (days.isEmpty()) {
            return WinLossDayStatsDto(
                totalGainLoss = BigDecimal.ZERO,
                averageDailyGainLoss = BigDecimal.ZERO,
                averageDailyVolume = BigDecimal.ZERO,
                averagePerShareGainLoss = BigDecimal.ZERO,
                averageTradeGainLoss = BigDecimal.ZERO,
                numberOfDays = 0,
                totalTrades = 0
            )
        }
        
        val relevantTrades = allTrades.filter { trade ->
            val dayPnl = days[trade.entryTime.toLocalDate()]
            if (isWinning) dayPnl != null && dayPnl > BigDecimal.ZERO
            else dayPnl != null && dayPnl < BigDecimal.ZERO
        }
        
        val totalPnl = days.values.sumOf { it }
        val totalVolume = relevantTrades.sumOf { it.entryValue }
        val totalShares = relevantTrades.sumOf { it.quantity }
        
        return WinLossDayStatsDto(
            totalGainLoss = totalPnl,
            averageDailyGainLoss = totalPnl.divide(BigDecimal(days.size), 2, RoundingMode.HALF_UP),
            averageDailyVolume = totalVolume.divide(BigDecimal(days.size), 2, RoundingMode.HALF_UP),
            averagePerShareGainLoss = if (totalShares > BigDecimal.ZERO) totalPnl.divide(totalShares, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO,
            averageTradeGainLoss = if (relevantTrades.isNotEmpty()) totalPnl.divide(BigDecimal(relevantTrades.size), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO,
            numberOfDays = days.size.toLong(),
            totalTrades = relevantTrades.size.toLong()
        )
    }
    
    private fun calculateDrawdownPeriods(cumulativePnl: List<Pair<LocalDate, BigDecimal>>): List<DrawdownPeriodDto> {
        val drawdowns = mutableListOf<DrawdownPeriodDto>()
        var peak = BigDecimal.ZERO
        var peakDate = cumulativePnl.firstOrNull()?.first ?: LocalDate.now()
        var inDrawdown = false
        var drawdownStart: LocalDate? = null
        
        cumulativePnl.forEach { (date, value) ->
            if (value > peak) {
                // New peak reached
                if (inDrawdown) {
                    // End of drawdown period
                    inDrawdown = false
                    drawdownStart = null
                }
                peak = value
                peakDate = date
            } else if (value < peak && !inDrawdown) {
                // Start of new drawdown
                inDrawdown = true
                drawdownStart = date
            }
        }
        
        return drawdowns
    }
    
    private fun calculateCurrentDrawdown(cumulativePnl: List<Pair<LocalDate, BigDecimal>>): BigDecimal {
        if (cumulativePnl.isEmpty()) return BigDecimal.ZERO
        
        val currentValue = cumulativePnl.last().second
        val peak = cumulativePnl.maxOfOrNull { it.second } ?: BigDecimal.ZERO
        
        return if (currentValue < peak) peak.subtract(currentValue) else BigDecimal.ZERO
    }
    
    private fun calculateCurrentDrawdownPercent(cumulativePnl: List<Pair<LocalDate, BigDecimal>>): BigDecimal {
        if (cumulativePnl.isEmpty()) return BigDecimal.ZERO
        
        val currentValue = cumulativePnl.last().second
        val peak = cumulativePnl.maxOfOrNull { it.second } ?: BigDecimal.ZERO
        
        return if (currentValue < peak && peak > BigDecimal.ZERO) {
            peak.subtract(currentValue).divide(peak, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else BigDecimal.ZERO
    }
    
    private fun calculateCurrentDrawdownDuration(cumulativePnl: List<Pair<LocalDate, BigDecimal>>): Long {
        if (cumulativePnl.isEmpty()) return 0L
        
        val peak = cumulativePnl.maxOfOrNull { it.second } ?: BigDecimal.ZERO
        val currentValue = cumulativePnl.last().second
        
        if (currentValue >= peak) return 0L
        
        // Find the last date when we were at peak
        val peakDate = cumulativePnl.findLast { it.second == peak }?.first ?: return 0L
        val currentDate = cumulativePnl.last().first
        
        return ChronoUnit.DAYS.between(peakDate, currentDate)
    }
}

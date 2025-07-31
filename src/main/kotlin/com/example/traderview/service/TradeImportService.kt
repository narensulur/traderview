package com.example.traderview.service

import com.example.traderview.dto.*
import com.example.traderview.entity.*
import com.example.traderview.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class TradeImportService(
    private val tradingAccountRepository: TradingAccountRepository,
    private val symbolRepository: SymbolRepository,
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository
) {
    
    fun importTrades(request: TradeImportRequest): TradeImportResponse {
        val errors = mutableListOf<String>()
        var importedOrders = 0
        var importedTrades = 0
        
        try {
            // Validate trading account
            val tradingAccount = tradingAccountRepository.findById(request.tradingAccountId).orElse(null)
                ?: return TradeImportResponse(
                    success = false,
                    message = "Trading account not found",
                    importedOrders = 0,
                    importedTrades = 0,
                    errors = listOf("Trading account with ID ${request.tradingAccountId} not found")
                )
            
            // Find or create symbol
            val symbol = findOrCreateSymbol(request.contractSymbol, request.contractName, request.exchange)
            
            // Process each trade record
            val processedTrades = mutableListOf<Order>()
            
            request.trades.forEachIndexed { index, tradeRecord ->
                try {
                    val orders = processTradeRecord(tradeRecord, tradingAccount, symbol, index)
                    processedTrades.addAll(orders)
                    importedOrders += orders.size
                } catch (e: Exception) {
                    errors.add("Error processing trade ${index + 1}: ${e.message}")
                }
            }
            
            // Create aggregated trades from orders
            importedTrades = createAggregatedTrades(processedTrades, tradingAccount, symbol)
            
            return TradeImportResponse(
                success = errors.isEmpty(),
                message = if (errors.isEmpty()) "Successfully imported trades" else "Import completed with errors",
                importedOrders = importedOrders,
                importedTrades = importedTrades,
                errors = errors
            )
            
        } catch (e: Exception) {
            return TradeImportResponse(
                success = false,
                message = "Import failed: ${e.message}",
                importedOrders = importedOrders,
                importedTrades = importedTrades,
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }
    
    private fun findOrCreateSymbol(ticker: String, name: String?, exchange: String): Symbol {
        return symbolRepository.findByTicker(ticker) ?: run {
            // Determine contract multiplier based on symbol
            val contractMultiplier = when {
                ticker.startsWith("MES") -> BigDecimal(5) // Micro S&P 500 futures: $5 per point
                ticker.startsWith("ES") -> BigDecimal(50) // S&P 500 futures: $50 per point
                ticker.startsWith("MNQ") -> BigDecimal(2) // Micro Nasdaq futures: $2 per point
                ticker.startsWith("NQ") -> BigDecimal(20) // Nasdaq futures: $20 per point
                ticker.startsWith("MYM") -> BigDecimal(0.5) // Micro Dow futures: $0.50 per point
                ticker.startsWith("YM") -> BigDecimal(5) // Dow futures: $5 per point
                else -> BigDecimal.ONE // Default multiplier for unknown symbols
            }

            val newSymbol = Symbol(
                ticker = ticker,
                name = name ?: ticker,
                exchange = exchange,
                assetType = AssetType.FUTURE,
                currency = "USD",
                contractMultiplier = contractMultiplier,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            symbolRepository.save(newSymbol)
        }
    }
    
    private fun processTradeRecord(
        record: TradeImportRecord,
        tradingAccount: TradingAccount,
        symbol: Symbol,
        index: Int
    ): List<Order> {
        val orders = mutableListOf<Order>()
        val baseDateTime = record.date.atTime(9, 30) // Market open time
        
        // Create buy order if buy quantity > 0
        if (record.buy > BigDecimal.ZERO) {
            val buyOrder = Order(
                brokerOrderId = "IMPORT_BUY_${symbol.ticker}_${record.date}_${index}_${System.currentTimeMillis()}",
                tradingAccount = tradingAccount,
                symbol = symbol,
                side = OrderSide.BUY,
                type = OrderType.MARKET,
                status = OrderStatus.FILLED,
                quantity = record.buy,
                filledQuantity = record.buy,
                price = record.price,
                avgFillPrice = record.price,
                commission = record.commission.divide(BigDecimal(2), 2, RoundingMode.HALF_UP), // Split fees between buy/sell
                fees = (record.exchangeFees.add(record.nfaFees)).divide(BigDecimal(2), 2, RoundingMode.HALF_UP),
                placedAt = baseDateTime,
                filledAt = baseDateTime,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            orders.add(orderRepository.save(buyOrder))
        }
        
        // Create sell order if sell quantity > 0
        if (record.sell > BigDecimal.ZERO) {
            val sellOrder = Order(
                brokerOrderId = "IMPORT_SELL_${symbol.ticker}_${record.date}_${index}_${System.currentTimeMillis()}",
                tradingAccount = tradingAccount,
                symbol = symbol,
                side = OrderSide.SELL,
                type = OrderType.MARKET,
                status = OrderStatus.FILLED,
                quantity = record.sell,
                filledQuantity = record.sell,
                price = record.price,
                avgFillPrice = record.price,
                commission = record.commission.divide(BigDecimal(2), 2, RoundingMode.HALF_UP),
                fees = (record.exchangeFees.add(record.nfaFees)).divide(BigDecimal(2), 2, RoundingMode.HALF_UP),
                placedAt = baseDateTime.plusMinutes(1), // Slightly later than buy
                filledAt = baseDateTime.plusMinutes(1),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            orders.add(orderRepository.save(sellOrder))
        }
        
        return orders
    }
    
    private fun createAggregatedTrades(
        orders: List<Order>,
        tradingAccount: TradingAccount,
        symbol: Symbol
    ): Int {
        // Group orders by date and create daily trades
        val ordersByDate = orders.groupBy { it.filledAt?.toLocalDate() }
        var tradesCreated = 0
        
        ordersByDate.forEach { (date, dayOrders) ->
            if (date != null) {
                val buyOrders = dayOrders.filter { it.side == OrderSide.BUY }
                val sellOrders = dayOrders.filter { it.side == OrderSide.SELL }
                
                if (buyOrders.isNotEmpty() && sellOrders.isNotEmpty()) {
                    // Create a trade for the day
                    val totalBuyQty = buyOrders.sumOf { it.filledQuantity ?: BigDecimal.ZERO }
                    val totalSellQty = sellOrders.sumOf { it.filledQuantity ?: BigDecimal.ZERO }
                    val avgBuyPrice = buyOrders.map { (it.avgFillPrice ?: BigDecimal.ZERO) * (it.filledQuantity ?: BigDecimal.ZERO) }
                        .sumOf { it } / totalBuyQty
                    val avgSellPrice = sellOrders.map { (it.avgFillPrice ?: BigDecimal.ZERO) * (it.filledQuantity ?: BigDecimal.ZERO) }
                        .sumOf { it } / totalSellQty
                    
                    val minQty = minOf(totalBuyQty, totalSellQty)
                    val entryValue = avgBuyPrice * minQty
                    val exitValue = avgSellPrice * minQty

                    // Apply contract multiplier for futures P&L calculation
                    val priceDifference = avgSellPrice - avgBuyPrice
                    val realizedPnl = priceDifference * minQty * symbol.contractMultiplier
                    
                    val totalCommission = dayOrders.sumOf { it.commission ?: BigDecimal.ZERO }
                    val totalFees = dayOrders.sumOf { it.fees ?: BigDecimal.ZERO }
                    
                    val trade = Trade(
                        tradingAccount = tradingAccount,
                        symbol = symbol,
                        entryOrder = buyOrders.first(),
                        exitOrder = sellOrders.first(),
                        quantity = minQty,
                        entryPrice = avgBuyPrice,
                        exitPrice = avgSellPrice,
                        entryValue = entryValue,
                        exitValue = exitValue,
                        realizedPnl = realizedPnl - totalCommission - totalFees,
                        totalCommission = totalCommission,
                        totalFees = totalFees,
                        entryTime = buyOrders.minOf { it.filledAt!! },
                        exitTime = sellOrders.maxOf { it.filledAt!! },
                        status = TradeStatus.CLOSED,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                    
                    tradeRepository.save(trade)
                    tradesCreated++
                }
            }
        }
        
        return tradesCreated
    }
    
    fun analyzeFuturesTrades(tradingAccountId: Long, contractSymbol: String): FuturesTradeAnalysis? {
        val symbol = symbolRepository.findByTicker(contractSymbol) ?: return null
        val trades = tradeRepository.findByTradingAccountIdAndSymbolId(tradingAccountId, symbol.id)

        if (trades.isEmpty()) return null

        val totalVolume = trades.sumOf { trade -> trade.quantity }
        val totalCommissions = trades.sumOf { trade -> trade.totalCommission }
        val totalFees = trades.sumOf { trade -> trade.totalFees }
        val netPnl = trades.sumOf { trade -> trade.realizedPnl ?: BigDecimal.ZERO }
        val avgTradeSize = totalVolume / BigDecimal(trades.size)

        val dailyBreakdown = trades.groupBy { trade -> trade.entryTime.toLocalDate() }
            .map { (date: LocalDate, dayTrades: List<Trade>) ->
                DailyTradeBreakdown(
                    date = date,
                    totalBuy = dayTrades.sumOf { trade -> trade.quantity },
                    totalSell = dayTrades.sumOf { trade -> trade.quantity },
                    netPosition = BigDecimal.ZERO, // For closed trades
                    avgBuyPrice = dayTrades.map { trade -> trade.entryPrice }.average().toBigDecimal(),
                    avgSellPrice = dayTrades.map { trade -> trade.exitPrice ?: BigDecimal.ZERO }.average().toBigDecimal(),
                    dailyPnl = dayTrades.sumOf { trade -> trade.realizedPnl ?: BigDecimal.ZERO },
                    totalCommissions = dayTrades.sumOf { trade -> trade.totalCommission },
                    totalFees = dayTrades.sumOf { trade -> trade.totalFees },
                    tradeCount = dayTrades.size
                )
            }
            .sortedBy { breakdown -> breakdown.date }

        return FuturesTradeAnalysis(
            contractSymbol = contractSymbol,
            totalTrades = trades.size,
            totalVolume = totalVolume,
            totalCommissions = totalCommissions,
            totalFees = totalFees,
            netPnl = netPnl,
            avgTradeSize = avgTradeSize,
            tradingDays = dailyBreakdown.size,
            firstTradeDate = dailyBreakdown.first().date,
            lastTradeDate = dailyBreakdown.last().date,
            dailyBreakdown = dailyBreakdown
        )
    }
}

private fun Collection<BigDecimal>.average(): Double {
    return if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size
}

private fun Double.toBigDecimal(): BigDecimal = BigDecimal.valueOf(this)

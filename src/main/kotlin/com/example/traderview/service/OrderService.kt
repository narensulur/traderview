package com.example.traderview.service

import com.example.traderview.dto.CreateOrderRequest
import com.example.traderview.dto.OrderDto
import com.example.traderview.entity.Order
import com.example.traderview.entity.OrderStatus
import com.example.traderview.repository.OrderRepository
import com.example.traderview.repository.SymbolRepository
import com.example.traderview.repository.TradingAccountRepository
import com.example.traderview.repository.TradeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val tradingAccountRepository: TradingAccountRepository,
    private val symbolRepository: SymbolRepository,
    private val tradeRepository: TradeRepository
) {
    
    fun getAllOrders(): List<OrderDto> {
        return orderRepository.findAll().map { it.toDto() }
    }
    
    fun getOrderById(id: Long): OrderDto? {
        return orderRepository.findById(id).orElse(null)?.toDto()
    }
    
    fun getOrderByBrokerOrderId(brokerOrderId: String): OrderDto? {
        return orderRepository.findByBrokerOrderId(brokerOrderId)?.toDto()
    }
    
    fun getOrdersByTradingAccount(tradingAccountId: Long): List<OrderDto> {
        return orderRepository.findByTradingAccountId(tradingAccountId).map { it.toDto() }
    }
    
    fun getOrdersBySymbol(symbolId: Long): List<OrderDto> {
        return orderRepository.findBySymbolId(symbolId).map { it.toDto() }
    }
    
    fun getOrdersByStatus(status: OrderStatus): List<OrderDto> {
        return orderRepository.findByStatus(status).map { it.toDto() }
    }
    
    fun getOrdersByDateRange(
        tradingAccountId: Long? = null,
        symbolId: Long? = null,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<OrderDto> {
        return when {
            tradingAccountId != null -> orderRepository.findByTradingAccountIdAndFilledAtBetween(
                tradingAccountId, startDate, endDate
            )
            symbolId != null -> orderRepository.findBySymbolIdAndFilledAtBetween(
                symbolId, startDate, endDate
            )
            else -> orderRepository.findByFilledAtBetween(startDate, endDate)
        }.map { it.toDto() }
    }
    
    fun createOrder(request: CreateOrderRequest): OrderDto {
        val tradingAccount = tradingAccountRepository.findById(request.tradingAccountId).orElse(null)
            ?: throw IllegalArgumentException("Trading account with ID ${request.tradingAccountId} not found")
        
        val symbol = symbolRepository.findById(request.symbolId).orElse(null)
            ?: throw IllegalArgumentException("Symbol with ID ${request.symbolId} not found")
        
        val existingOrder = orderRepository.findByBrokerOrderId(request.brokerOrderId)
        if (existingOrder != null) {
            throw IllegalArgumentException("Order with broker ID '${request.brokerOrderId}' already exists")
        }
        
        val order = Order(
            brokerOrderId = request.brokerOrderId,
            tradingAccount = tradingAccount,
            symbol = symbol,
            side = request.side,
            type = request.type,
            status = request.status,
            quantity = request.quantity,
            filledQuantity = request.filledQuantity,
            price = request.price,
            avgFillPrice = request.avgFillPrice,
            commission = request.commission,
            fees = request.fees,
            placedAt = request.placedAt,
            filledAt = request.filledAt,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return orderRepository.save(order).toDto()
    }
    
    fun deleteOrder(id: Long): Boolean {
        return if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    private fun Order.toDto(): OrderDto {
        // Calculate realized P&L for this order
        val realizedPnl = calculateOrderPnl(this)

        // Calculate average trades per day for this symbol
        val avgTradesPerDay = calculateAvgTradesPerDay(this.symbol.id, this.tradingAccount.id)

        // Get last traded date for this symbol
        val lastTradedDate = getLastTradedDate(this.symbol.id, this.tradingAccount.id)

        return OrderDto(
            id = id,
            brokerOrderId = brokerOrderId,
            tradingAccountId = tradingAccount.id,
            tradingAccountName = tradingAccount.accountName,
            symbolId = symbol.id,
            symbolTicker = symbol.ticker,
            side = side,
            type = type,
            status = status,
            quantity = quantity,
            filledQuantity = filledQuantity,
            price = price,
            avgFillPrice = avgFillPrice,
            commission = commission,
            fees = fees,
            placedAt = placedAt,
            filledAt = filledAt,
            cancelledAt = cancelledAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            realizedPnl = realizedPnl,
            avgTradesPerDay = avgTradesPerDay,
            lastTradedDate = lastTradedDate
        )
    }

    private fun calculateOrderPnl(order: Order): BigDecimal? {
        if (order.status != OrderStatus.FILLED || order.avgFillPrice == null) {
            return null
        }

        // Find trades that involve this order as either entry or exit
        val trades = tradeRepository.findBySymbolIdAndTradingAccountId(order.symbol.id, order.tradingAccount.id)
        val relatedTrades = trades.filter { trade ->
            trade.entryOrder.id == order.id || trade.exitOrder?.id == order.id
        }

        return if (relatedTrades.isNotEmpty()) {
            // If this order is part of completed trades, return the proportional P&L
            val totalPnl = relatedTrades.sumOf { it.realizedPnl ?: BigDecimal.ZERO }

            // For orders that are part of multiple trades, calculate proportional P&L
            val orderContribution = relatedTrades.sumOf { trade ->
                when {
                    trade.entryOrder.id == order.id && trade.exitOrder?.id == order.id -> {
                        // This order is both entry and exit (shouldn't happen, but handle it)
                        trade.realizedPnl ?: BigDecimal.ZERO
                    }
                    trade.entryOrder.id == order.id -> {
                        // This order is the entry - attribute P&L based on entry contribution
                        (trade.realizedPnl ?: BigDecimal.ZERO).multiply(
                            order.filledQuantity!!.divide(trade.quantity, 4, RoundingMode.HALF_UP)
                        )
                    }
                    trade.exitOrder?.id == order.id -> {
                        // This order is the exit - attribute P&L based on exit contribution
                        (trade.realizedPnl ?: BigDecimal.ZERO).multiply(
                            order.filledQuantity!!.divide(trade.quantity, 4, RoundingMode.HALF_UP)
                        )
                    }
                    else -> BigDecimal.ZERO
                }
            }

            orderContribution
        } else {
            // Individual orders don't have P&L until they're part of a complete trade
            // Return null to indicate no P&L available yet
            null
        }
    }

    private fun calculateAvgTradesPerDay(symbolId: Long, tradingAccountId: Long): BigDecimal {
        // Get all filled orders for this symbol and account
        val filledOrders = orderRepository.findBySymbolIdAndTradingAccountIdAndStatusOrderByFilledAtDesc(
            symbolId, tradingAccountId, OrderStatus.FILLED
        )

        if (filledOrders.isEmpty()) {
            return BigDecimal.ZERO
        }

        // Get the date range from first to last filled order
        val firstOrderDate = filledOrders.minByOrNull { it.filledAt ?: it.placedAt }?.filledAt?.toLocalDate()
            ?: filledOrders.minByOrNull { it.placedAt }?.placedAt?.toLocalDate()
        val lastOrderDate = filledOrders.maxByOrNull { it.filledAt ?: it.placedAt }?.filledAt?.toLocalDate()
            ?: filledOrders.maxByOrNull { it.placedAt }?.placedAt?.toLocalDate()

        if (firstOrderDate == null || lastOrderDate == null) {
            return BigDecimal.ZERO
        }

        // Calculate days between first and last order (minimum 1 day)
        val daysBetween = maxOf(ChronoUnit.DAYS.between(firstOrderDate, lastOrderDate) + 1, 1)

        // Return total orders divided by days
        return BigDecimal(filledOrders.size).divide(BigDecimal(daysBetween), 2, RoundingMode.HALF_UP)
    }

    private fun getLastTradedDate(symbolId: Long, tradingAccountId: Long): LocalDateTime? {
        val orders = orderRepository.findBySymbolIdAndTradingAccountIdAndStatusOrderByFilledAtDesc(
            symbolId, tradingAccountId, OrderStatus.FILLED
        )

        return orders.firstOrNull()?.filledAt
    }
}

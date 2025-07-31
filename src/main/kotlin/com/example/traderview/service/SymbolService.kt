package com.example.traderview.service

import com.example.traderview.dto.CreateSymbolRequest
import com.example.traderview.dto.SymbolDto
import com.example.traderview.dto.SymbolWithStatsDto
import com.example.traderview.entity.AssetType
import com.example.traderview.entity.OrderStatus
import com.example.traderview.entity.Symbol
import com.example.traderview.repository.OrderRepository
import com.example.traderview.repository.SymbolRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class SymbolService(
    private val symbolRepository: SymbolRepository,
    private val orderRepository: OrderRepository
) {
    
    fun getAllSymbols(): List<SymbolDto> {
        return symbolRepository.findAll().map { it.toDto() }
    }
    
    fun getActiveSymbols(): List<SymbolDto> {
        return symbolRepository.findByIsActiveTrue().map { it.toDto() }
    }
    
    fun getSymbolById(id: Long): SymbolDto? {
        return symbolRepository.findById(id).orElse(null)?.toDto()
    }
    
    fun getSymbolByTicker(ticker: String): SymbolDto? {
        return symbolRepository.findByTicker(ticker)?.toDto()
    }
    
    fun getSymbolsByAssetType(assetType: AssetType): List<SymbolDto> {
        return symbolRepository.findByAssetType(assetType).map { it.toDto() }
    }
    
    fun searchSymbolsByTicker(ticker: String): List<SymbolDto> {
        return symbolRepository.findByTickerContainingIgnoreCase(ticker).map { it.toDto() }
    }
    
    fun getSymbolsByExchange(exchange: String): List<SymbolDto> {
        return symbolRepository.findByExchange(exchange).map { it.toDto() }
    }
    
    fun createSymbol(request: CreateSymbolRequest): SymbolDto {
        val existingSymbol = symbolRepository.findByTicker(request.ticker)
        if (existingSymbol != null) {
            throw IllegalArgumentException("Symbol with ticker '${request.ticker}' already exists")
        }
        
        val symbol = Symbol(
            ticker = request.ticker,
            name = request.name,
            exchange = request.exchange,
            assetType = request.assetType,
            currency = request.currency,
            contractMultiplier = BigDecimal.ONE, // Default multiplier
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return symbolRepository.save(symbol).toDto()
    }
    
    fun deleteSymbol(id: Long): Boolean {
        return if (symbolRepository.existsById(id)) {
            symbolRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    fun getSymbolsWithWeeklyStats(tradingAccountId: Long): List<SymbolWithStatsDto> {
        val symbols = symbolRepository.findByIsActiveTrue()
        val oneWeekAgo = LocalDateTime.now().minusDays(7)

        return symbols.map { symbol ->
            // Count filled orders for this symbol in the last 7 days
            val weeklyOrders = orderRepository.findBySymbolIdAndTradingAccountIdAndStatusOrderByFilledAtDesc(
                symbol.id, tradingAccountId, OrderStatus.FILLED
            ).filter { order ->
                order.filledAt?.isAfter(oneWeekAgo) == true
            }

            SymbolWithStatsDto(
                id = symbol.id,
                ticker = symbol.ticker,
                name = symbol.name,
                exchange = symbol.exchange,
                assetType = symbol.assetType,
                currency = symbol.currency,
                contractMultiplier = symbol.contractMultiplier,
                isActive = symbol.isActive,
                createdAt = symbol.createdAt,
                updatedAt = symbol.updatedAt,
                tradesThisWeek = weeklyOrders.size
            )
        }.sortedByDescending { it.tradesThisWeek }
    }
    
    private fun Symbol.toDto() = SymbolDto(
        id = id,
        ticker = ticker,
        name = name,
        exchange = exchange,
        assetType = assetType,
        currency = currency,
        contractMultiplier = contractMultiplier,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

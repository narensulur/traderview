package com.example.traderview.repository

import com.example.traderview.entity.Trade
import com.example.traderview.entity.TradeStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface TradeRepository : JpaRepository<Trade, Long> {
    
    fun findByTradingAccountId(tradingAccountId: Long): List<Trade>

    fun findBySymbolId(symbolId: Long): List<Trade>

    @Query("SELECT t FROM Trade t WHERE t.tradingAccount.id = :accountId AND t.symbol.id = :symbolId")
    fun findByTradingAccountIdAndSymbolId(
        @Param("accountId") accountId: Long,
        @Param("symbolId") symbolId: Long
    ): List<Trade>

    @Query("SELECT t FROM Trade t WHERE t.symbol.id = :symbolId AND t.tradingAccount.id = :accountId")
    fun findBySymbolIdAndTradingAccountId(
        @Param("symbolId") symbolId: Long,
        @Param("accountId") accountId: Long
    ): List<Trade>
    
    fun findByStatus(status: TradeStatus): List<Trade>
    
    @Query("SELECT t FROM Trade t WHERE t.tradingAccount.id = :accountId AND t.status = :status")
    fun findByTradingAccountIdAndStatus(
        @Param("accountId") accountId: Long,
        @Param("status") status: TradeStatus
    ): List<Trade>
    
    @Query("SELECT t FROM Trade t WHERE t.symbol.id = :symbolId AND t.entryTime BETWEEN :startDate AND :endDate")
    fun findBySymbolIdAndEntryTimeBetween(
        @Param("symbolId") symbolId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Trade>
    
    @Query("SELECT t FROM Trade t WHERE t.tradingAccount.id = :accountId AND t.entryTime BETWEEN :startDate AND :endDate ORDER BY t.entryTime DESC")
    fun findByTradingAccountIdAndEntryTimeBetween(
        @Param("accountId") accountId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Trade>
    
    @Query("SELECT SUM(t.realizedPnl) FROM Trade t WHERE t.tradingAccount.id = :accountId AND t.status = 'CLOSED'")
    fun getTotalRealizedPnlByTradingAccount(@Param("accountId") accountId: Long): BigDecimal?
    
    @Query("SELECT SUM(t.realizedPnl) FROM Trade t WHERE t.symbol.id = :symbolId AND t.status = 'CLOSED'")
    fun getTotalRealizedPnlBySymbol(@Param("symbolId") symbolId: Long): BigDecimal?
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.tradingAccount.id = :accountId AND t.realizedPnl > 0 AND t.status = 'CLOSED'")
    fun getWinningTradesCount(@Param("accountId") accountId: Long): Long
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.tradingAccount.id = :accountId AND t.realizedPnl < 0 AND t.status = 'CLOSED'")
    fun getLosingTradesCount(@Param("accountId") accountId: Long): Long
}

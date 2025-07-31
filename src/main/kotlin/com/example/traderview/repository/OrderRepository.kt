package com.example.traderview.repository

import com.example.traderview.entity.Order
import com.example.traderview.entity.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    
    fun findByBrokerOrderId(brokerOrderId: String): Order?
    
    fun findByTradingAccountId(tradingAccountId: Long): List<Order>
    
    fun findBySymbolId(symbolId: Long): List<Order>
    
    fun findByStatus(status: OrderStatus): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.tradingAccount.id = :accountId AND o.status = :status")
    fun findByTradingAccountIdAndStatus(
        @Param("accountId") accountId: Long,
        @Param("status") status: OrderStatus
    ): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId AND o.filledAt BETWEEN :startDate AND :endDate")
    fun findBySymbolIdAndFilledAtBetween(
        @Param("symbolId") symbolId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.tradingAccount.id = :accountId AND o.filledAt BETWEEN :startDate AND :endDate ORDER BY o.filledAt DESC")
    fun findByTradingAccountIdAndFilledAtBetween(
        @Param("accountId") accountId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.filledAt BETWEEN :startDate AND :endDate ORDER BY o.filledAt DESC")
    fun findByFilledAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Order>

    @Query("SELECT o FROM Order o WHERE o.symbol.id = :symbolId AND o.tradingAccount.id = :accountId AND o.status = :status ORDER BY o.filledAt DESC")
    fun findBySymbolIdAndTradingAccountIdAndStatusOrderByFilledAtDesc(
        @Param("symbolId") symbolId: Long,
        @Param("accountId") accountId: Long,
        @Param("status") status: OrderStatus
    ): List<Order>
}

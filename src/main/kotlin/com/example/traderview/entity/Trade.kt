package com.example.traderview.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "trades")
data class Trade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trading_account_id", nullable = false)
    val tradingAccount: TradingAccount,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    val symbol: Symbol,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_order_id", nullable = false)
    val entryOrder: Order,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_order_id")
    val exitOrder: Order? = null,
    
    @Column(nullable = false, precision = 10, scale = 0)
    val quantity: BigDecimal,
    
    @Column(nullable = false, precision = 15, scale = 4)
    val entryPrice: BigDecimal,
    
    @Column(precision = 15, scale = 4)
    val exitPrice: BigDecimal? = null,
    
    @Column(nullable = false, precision = 15, scale = 2)
    val entryValue: BigDecimal,
    
    @Column(precision = 15, scale = 2)
    val exitValue: BigDecimal? = null,
    
    @Column(precision = 15, scale = 2)
    val realizedPnl: BigDecimal? = null,
    
    @Column(precision = 15, scale = 2)
    val totalCommission: BigDecimal = BigDecimal.ZERO,
    
    @Column(precision = 15, scale = 2)
    val totalFees: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false)
    val entryTime: LocalDateTime,
    
    @Column
    val exitTime: LocalDateTime? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TradeStatus,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class TradeStatus {
    OPEN,
    CLOSED,
    PARTIALLY_CLOSED
}

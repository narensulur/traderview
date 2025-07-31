package com.example.traderview.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val brokerOrderId: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trading_account_id", nullable = false)
    val tradingAccount: TradingAccount,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    val symbol: Symbol,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val side: OrderSide,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: OrderType,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrderStatus,
    
    @Column(nullable = false, precision = 10, scale = 0)
    val quantity: BigDecimal,
    
    @Column(precision = 10, scale = 0)
    val filledQuantity: BigDecimal? = null,
    
    @Column(precision = 15, scale = 4)
    val price: BigDecimal? = null,
    
    @Column(precision = 15, scale = 4)
    val avgFillPrice: BigDecimal? = null,
    
    @Column(precision = 15, scale = 2)
    val commission: BigDecimal? = null,
    
    @Column(precision = 15, scale = 2)
    val fees: BigDecimal? = null,
    
    @Column(nullable = false)
    val placedAt: LocalDateTime,
    
    @Column
    val filledAt: LocalDateTime? = null,
    
    @Column
    val cancelledAt: LocalDateTime? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class OrderSide {
    BUY,
    SELL
}

enum class OrderType {
    MARKET,
    LIMIT,
    STOP,
    STOP_LIMIT
}

enum class OrderStatus {
    PENDING,
    FILLED,
    PARTIALLY_FILLED,
    CANCELLED,
    REJECTED
}

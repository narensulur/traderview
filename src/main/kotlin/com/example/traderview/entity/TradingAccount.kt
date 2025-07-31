package com.example.traderview.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "trading_accounts")
data class TradingAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val accountNumber: String,
    
    @Column(nullable = false)
    val accountName: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    val broker: Broker,
    
    @Column(nullable = false, precision = 15, scale = 2)
    val initialBalance: BigDecimal,
    
    @Column(nullable = false, precision = 15, scale = 2)
    val currentBalance: BigDecimal,
    
    @Column(nullable = false)
    val currency: String = "USD",
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "tradingAccount", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val orders: List<Order> = emptyList()
)

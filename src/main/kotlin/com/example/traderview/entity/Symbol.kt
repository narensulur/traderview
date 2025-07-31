package com.example.traderview.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "symbols")
data class Symbol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val ticker: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val exchange: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val assetType: AssetType,
    
    @Column(nullable = false)
    val currency: String = "USD",

    @Column(nullable = false, precision = 10, scale = 2)
    val contractMultiplier: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "symbol", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val orders: List<Order> = emptyList()
)

enum class AssetType {
    STOCK,
    OPTION,
    FUTURE,
    FOREX,
    CRYPTO,
    ETF,
    BOND
}

package com.example.traderview.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "brokers")
data class Broker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val name: String,
    
    @Column(nullable = false)
    val displayName: String,
    
    @Column(nullable = false)
    val apiEndpoint: String,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "broker", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val tradingAccounts: List<TradingAccount> = emptyList()
)

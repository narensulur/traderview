package com.example.traderview.repository

import com.example.traderview.entity.TradingAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TradingAccountRepository : JpaRepository<TradingAccount, Long> {
    
    fun findByAccountNumber(accountNumber: String): TradingAccount?
    
    fun findByBrokerId(brokerId: Long): List<TradingAccount>
    
    fun findByIsActiveTrue(): List<TradingAccount>
    
    @Query("SELECT ta FROM TradingAccount ta WHERE ta.broker.id = :brokerId AND ta.isActive = true")
    fun findActiveTradingAccountsByBroker(@Param("brokerId") brokerId: Long): List<TradingAccount>
}

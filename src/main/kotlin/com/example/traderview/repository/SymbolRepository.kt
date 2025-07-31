package com.example.traderview.repository

import com.example.traderview.entity.AssetType
import com.example.traderview.entity.Symbol
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SymbolRepository : JpaRepository<Symbol, Long> {
    
    fun findByTicker(ticker: String): Symbol?
    
    fun findByAssetType(assetType: AssetType): List<Symbol>
    
    fun findByIsActiveTrue(): List<Symbol>
    
    @Query("SELECT s FROM Symbol s WHERE s.ticker LIKE %:ticker% AND s.isActive = true")
    fun findByTickerContainingIgnoreCase(@Param("ticker") ticker: String): List<Symbol>
    
    @Query("SELECT s FROM Symbol s WHERE s.exchange = :exchange AND s.isActive = true")
    fun findByExchange(@Param("exchange") exchange: String): List<Symbol>
}

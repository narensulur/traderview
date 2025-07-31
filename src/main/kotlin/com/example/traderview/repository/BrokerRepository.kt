package com.example.traderview.repository

import com.example.traderview.entity.Broker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BrokerRepository : JpaRepository<Broker, Long> {
    
    fun findByName(name: String): Broker?
    
    fun findByIsActiveTrue(): List<Broker>
    
    @Query("SELECT b FROM Broker b WHERE b.isActive = true ORDER BY b.displayName")
    fun findActiveBrokersOrderByDisplayName(): List<Broker>
}

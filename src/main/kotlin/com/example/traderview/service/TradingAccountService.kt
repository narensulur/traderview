package com.example.traderview.service

import com.example.traderview.dto.CreateTradingAccountRequest
import com.example.traderview.dto.TradingAccountDto
import com.example.traderview.dto.UpdateTradingAccountRequest
import com.example.traderview.entity.TradingAccount
import com.example.traderview.repository.BrokerRepository
import com.example.traderview.repository.TradingAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TradingAccountService(
    private val tradingAccountRepository: TradingAccountRepository,
    private val brokerRepository: BrokerRepository
) {
    
    fun getAllTradingAccounts(): List<TradingAccountDto> {
        return tradingAccountRepository.findAll().map { it.toDto() }
    }
    
    fun getActiveTradingAccounts(): List<TradingAccountDto> {
        return tradingAccountRepository.findByIsActiveTrue().map { it.toDto() }
    }
    
    fun getTradingAccountById(id: Long): TradingAccountDto? {
        return tradingAccountRepository.findById(id).orElse(null)?.toDto()
    }
    
    fun getTradingAccountsByBroker(brokerId: Long): List<TradingAccountDto> {
        return tradingAccountRepository.findByBrokerId(brokerId).map { it.toDto() }
    }
    
    fun getTradingAccountByAccountNumber(accountNumber: String): TradingAccountDto? {
        return tradingAccountRepository.findByAccountNumber(accountNumber)?.toDto()
    }
    
    fun createTradingAccount(request: CreateTradingAccountRequest): TradingAccountDto {
        val broker = brokerRepository.findById(request.brokerId).orElse(null)
            ?: throw IllegalArgumentException("Broker with ID ${request.brokerId} not found")
        
        val existingAccount = tradingAccountRepository.findByAccountNumber(request.accountNumber)
        if (existingAccount != null) {
            throw IllegalArgumentException("Trading account with number '${request.accountNumber}' already exists")
        }
        
        val tradingAccount = TradingAccount(
            accountNumber = request.accountNumber,
            accountName = request.accountName,
            broker = broker,
            initialBalance = request.initialBalance,
            currentBalance = request.initialBalance,
            currency = request.currency,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return tradingAccountRepository.save(tradingAccount).toDto()
    }
    
    fun updateTradingAccount(id: Long, request: UpdateTradingAccountRequest): TradingAccountDto? {
        val account = tradingAccountRepository.findById(id).orElse(null) ?: return null
        
        val updatedAccount = account.copy(
            accountName = request.accountName ?: account.accountName,
            currentBalance = request.currentBalance ?: account.currentBalance,
            isActive = request.isActive ?: account.isActive,
            updatedAt = LocalDateTime.now()
        )
        
        return tradingAccountRepository.save(updatedAccount).toDto()
    }
    
    fun deleteTradingAccount(id: Long): Boolean {
        return if (tradingAccountRepository.existsById(id)) {
            tradingAccountRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    private fun TradingAccount.toDto() = TradingAccountDto(
        id = id,
        accountNumber = accountNumber,
        accountName = accountName,
        brokerId = broker.id,
        brokerName = broker.displayName,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        currency = currency,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

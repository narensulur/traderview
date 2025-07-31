package com.example.traderview.controller

import com.example.traderview.dto.CreateTradingAccountRequest
import com.example.traderview.dto.TradingAccountDto
import com.example.traderview.dto.UpdateTradingAccountRequest
import com.example.traderview.service.TradingAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trading-accounts")
@Tag(name = "Trading Accounts", description = "Trading account management operations")
class TradingAccountController(
    private val tradingAccountService: TradingAccountService
) {
    
    @GetMapping
    @Operation(summary = "Get all trading accounts")
    fun getAllTradingAccounts(): ResponseEntity<List<TradingAccountDto>> {
        return ResponseEntity.ok(tradingAccountService.getAllTradingAccounts())
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active trading accounts")
    fun getActiveTradingAccounts(): ResponseEntity<List<TradingAccountDto>> {
        return ResponseEntity.ok(tradingAccountService.getActiveTradingAccounts())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get trading account by ID")
    fun getTradingAccountById(@PathVariable id: Long): ResponseEntity<TradingAccountDto> {
        val account = tradingAccountService.getTradingAccountById(id)
        return if (account != null) {
            ResponseEntity.ok(account)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/broker/{brokerId}")
    @Operation(summary = "Get trading accounts by broker")
    fun getTradingAccountsByBroker(@PathVariable brokerId: Long): ResponseEntity<List<TradingAccountDto>> {
        return ResponseEntity.ok(tradingAccountService.getTradingAccountsByBroker(brokerId))
    }
    
    @GetMapping("/account-number/{accountNumber}")
    @Operation(summary = "Get trading account by account number")
    fun getTradingAccountByAccountNumber(@PathVariable accountNumber: String): ResponseEntity<TradingAccountDto> {
        val account = tradingAccountService.getTradingAccountByAccountNumber(accountNumber)
        return if (account != null) {
            ResponseEntity.ok(account)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping
    @Operation(summary = "Create a new trading account")
    fun createTradingAccount(@Valid @RequestBody request: CreateTradingAccountRequest): ResponseEntity<TradingAccountDto> {
        return try {
            val account = tradingAccountService.createTradingAccount(request)
            ResponseEntity.status(HttpStatus.CREATED).body(account)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update trading account")
    fun updateTradingAccount(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTradingAccountRequest
    ): ResponseEntity<TradingAccountDto> {
        val account = tradingAccountService.updateTradingAccount(id, request)
        return if (account != null) {
            ResponseEntity.ok(account)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete trading account")
    fun deleteTradingAccount(@PathVariable id: Long): ResponseEntity<Void> {
        return if (tradingAccountService.deleteTradingAccount(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

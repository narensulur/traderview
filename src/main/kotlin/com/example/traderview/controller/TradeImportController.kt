package com.example.traderview.controller

import com.example.traderview.dto.FuturesTradeAnalysis
import com.example.traderview.dto.TradeImportRequest
import com.example.traderview.dto.TradeImportResponse
import com.example.traderview.service.TradeImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/import")
@Tag(name = "Trade Import", description = "Import trading data from brokers")
class TradeImportController(
    private val tradeImportService: TradeImportService
) {
    
    @PostMapping("/trades")
    @Operation(summary = "Import trades from broker data")
    fun importTrades(@Valid @RequestBody request: TradeImportRequest): ResponseEntity<TradeImportResponse> {
        val response = tradeImportService.importTrades(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
    
    @GetMapping("/analysis/futures/{tradingAccountId}/{contractSymbol}")
    @Operation(summary = "Get futures trading analysis for a specific contract")
    fun getFuturesAnalysis(
        @PathVariable tradingAccountId: Long,
        @PathVariable contractSymbol: String
    ): ResponseEntity<FuturesTradeAnalysis> {
        val analysis = tradeImportService.analyzeFuturesTrades(tradingAccountId, contractSymbol)
        return if (analysis != null) {
            ResponseEntity.ok(analysis)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

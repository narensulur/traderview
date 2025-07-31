package com.example.traderview.controller

import com.example.traderview.dto.CreateSymbolRequest
import com.example.traderview.dto.SymbolDto
import com.example.traderview.dto.SymbolWithStatsDto
import com.example.traderview.entity.AssetType
import com.example.traderview.service.SymbolService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/symbols")
@Tag(name = "Symbols", description = "Symbol management operations")
class SymbolController(
    private val symbolService: SymbolService
) {
    
    @GetMapping
    @Operation(summary = "Get all symbols")
    fun getAllSymbols(): ResponseEntity<List<SymbolDto>> {
        return ResponseEntity.ok(symbolService.getAllSymbols())
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active symbols")
    fun getActiveSymbols(): ResponseEntity<List<SymbolDto>> {
        return ResponseEntity.ok(symbolService.getActiveSymbols())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get symbol by ID")
    fun getSymbolById(@PathVariable id: Long): ResponseEntity<SymbolDto> {
        val symbol = symbolService.getSymbolById(id)
        return if (symbol != null) {
            ResponseEntity.ok(symbol)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/ticker/{ticker}")
    @Operation(summary = "Get symbol by ticker")
    fun getSymbolByTicker(@PathVariable ticker: String): ResponseEntity<SymbolDto> {
        val symbol = symbolService.getSymbolByTicker(ticker)
        return if (symbol != null) {
            ResponseEntity.ok(symbol)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/asset-type/{assetType}")
    @Operation(summary = "Get symbols by asset type")
    fun getSymbolsByAssetType(@PathVariable assetType: AssetType): ResponseEntity<List<SymbolDto>> {
        return ResponseEntity.ok(symbolService.getSymbolsByAssetType(assetType))
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search symbols by ticker")
    fun searchSymbolsByTicker(@RequestParam ticker: String): ResponseEntity<List<SymbolDto>> {
        return ResponseEntity.ok(symbolService.searchSymbolsByTicker(ticker))
    }

    @GetMapping("/with-weekly-stats/{accountId}")
    @Operation(summary = "Get symbols with weekly trading statistics")
    fun getSymbolsWithWeeklyStats(@PathVariable accountId: Long): ResponseEntity<List<SymbolWithStatsDto>> {
        return ResponseEntity.ok(symbolService.getSymbolsWithWeeklyStats(accountId))
    }
    
    @GetMapping("/exchange/{exchange}")
    @Operation(summary = "Get symbols by exchange")
    fun getSymbolsByExchange(@PathVariable exchange: String): ResponseEntity<List<SymbolDto>> {
        return ResponseEntity.ok(symbolService.getSymbolsByExchange(exchange))
    }
    
    @PostMapping
    @Operation(summary = "Create a new symbol")
    fun createSymbol(@Valid @RequestBody request: CreateSymbolRequest): ResponseEntity<SymbolDto> {
        return try {
            val symbol = symbolService.createSymbol(request)
            ResponseEntity.status(HttpStatus.CREATED).body(symbol)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete symbol")
    fun deleteSymbol(@PathVariable id: Long): ResponseEntity<Void> {
        return if (symbolService.deleteSymbol(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

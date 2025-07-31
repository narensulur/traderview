package com.example.traderview.controller

import com.example.traderview.dto.*
import com.example.traderview.service.AdvancedAnalyticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/advanced-analytics")
@Tag(name = "Advanced Analytics", description = "TraderVue-style advanced analytics and reporting")
class AdvancedAnalyticsController(
    private val advancedAnalyticsService: AdvancedAnalyticsService
) {
    
    @GetMapping("/stats/{accountId}")
    @Operation(summary = "Get comprehensive trade statistics (TraderVue Overview tab)")
    fun getTradeStats(
        @PathVariable accountId: Long,
        @RequestParam(required = false) symbols: List<String>?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) sides: List<String>?,
        @RequestParam(required = false) durations: List<String>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<TradeStatsDto> {
        val filters = AdvancedFilterRequest(
            tradingAccountId = accountId,
            symbols = symbols,
            tags = tags,
            sides = sides,
            durations = durations,
            startDate = startDate?.atStartOfDay(),
            endDate = endDate?.plusDays(1)?.atStartOfDay()
        )
        
        val stats = advancedAnalyticsService.getTradeStats(accountId, filters)
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/win-loss-days/{accountId}")
    @Operation(summary = "Get win vs loss days analysis (TraderVue Win vs Loss Days tab)")
    fun getWinLossDaysAnalysis(
        @PathVariable accountId: Long,
        @RequestParam(required = false) symbols: List<String>?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<WinLossDaysDto> {
        val filters = AdvancedFilterRequest(
            tradingAccountId = accountId,
            symbols = symbols,
            tags = tags,
            startDate = startDate?.atStartOfDay(),
            endDate = endDate?.plusDays(1)?.atStartOfDay()
        )
        
        val analysis = advancedAnalyticsService.getWinLossDaysAnalysis(accountId, filters)
        return ResponseEntity.ok(analysis)
    }
    
    @GetMapping("/drawdown/{accountId}")
    @Operation(summary = "Get drawdown analysis (TraderVue Drawdown tab)")
    fun getDrawdownAnalysis(
        @PathVariable accountId: Long,
        @RequestParam(required = false) symbols: List<String>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<DrawdownAnalysisDto> {
        val filters = AdvancedFilterRequest(
            tradingAccountId = accountId,
            symbols = symbols,
            startDate = startDate?.atStartOfDay(),
            endDate = endDate?.plusDays(1)?.atStartOfDay()
        )
        
        val analysis = advancedAnalyticsService.getDrawdownAnalysis(accountId, filters)
        return ResponseEntity.ok(analysis)
    }
    
    @GetMapping("/intraday/{accountId}")
    @Operation(summary = "Get intraday analysis for specific date")
    fun getIntradayAnalysis(
        @PathVariable accountId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<IntradayAnalysisDto>> {
        val analysis = advancedAnalyticsService.getIntradayAnalysis(accountId, date)
        return ResponseEntity.ok(analysis)
    }
    
    @PostMapping("/filtered-stats")
    @Operation(summary = "Get trade statistics with advanced filtering")
    fun getFilteredTradeStats(
        @RequestBody filterRequest: AdvancedFilterRequest
    ): ResponseEntity<TradeStatsDto> {
        val stats = advancedAnalyticsService.getTradeStats(filterRequest.tradingAccountId, filterRequest)
        return ResponseEntity.ok(stats)
    }
    
    @PostMapping("/filtered-win-loss-days")
    @Operation(summary = "Get win vs loss days with advanced filtering")
    fun getFilteredWinLossDays(
        @RequestBody filterRequest: AdvancedFilterRequest
    ): ResponseEntity<WinLossDaysDto> {
        val analysis = advancedAnalyticsService.getWinLossDaysAnalysis(filterRequest.tradingAccountId, filterRequest)
        return ResponseEntity.ok(analysis)
    }
}

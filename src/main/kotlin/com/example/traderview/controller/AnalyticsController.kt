package com.example.traderview.controller

import com.example.traderview.dto.DailyPnlDto
import com.example.traderview.dto.DashboardSummaryDto
import com.example.traderview.dto.SymbolPerformanceDto
import com.example.traderview.service.AnalyticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Trading analytics and dashboard operations")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {
    
    @GetMapping("/dashboard/{tradingAccountId}")
    @Operation(summary = "Get dashboard summary for trading account")
    fun getDashboardSummary(
        @PathVariable tradingAccountId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<DashboardSummaryDto> {
        val start = startDate ?: LocalDateTime.now().minusMonths(1)
        val end = endDate ?: LocalDateTime.now()
        
        return ResponseEntity.ok(
            analyticsService.getDashboardSummary(tradingAccountId, start, end)
        )
    }
    
    @GetMapping("/symbol-performance/{tradingAccountId}")
    @Operation(summary = "Get symbol performance analysis for trading account")
    fun getSymbolPerformance(
        @PathVariable tradingAccountId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<List<SymbolPerformanceDto>> {
        val start = startDate ?: LocalDateTime.now().minusMonths(1)
        val end = endDate ?: LocalDateTime.now()
        
        return ResponseEntity.ok(
            analyticsService.getSymbolPerformance(tradingAccountId, start, end)
        )
    }
    
    @GetMapping("/daily-pnl/{tradingAccountId}")
    @Operation(summary = "Get daily P&L analysis for trading account")
    fun getDailyPnl(
        @PathVariable tradingAccountId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<List<DailyPnlDto>> {
        val start = startDate ?: LocalDateTime.now().minusMonths(1)
        val end = endDate ?: LocalDateTime.now()
        
        return ResponseEntity.ok(
            analyticsService.getDailyPnl(tradingAccountId, start, end)
        )
    }
}

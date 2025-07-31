package com.example.traderview.controller

import com.example.traderview.integration.*
import com.example.traderview.service.BrokerIntegrationInfo
import com.example.traderview.service.BrokerIntegrationService
import com.example.traderview.service.BrokerSyncResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/broker-integration")
@Tag(name = "Broker Integration", description = "Integration with external brokers")
class BrokerIntegrationController(
    private val brokerIntegrationService: BrokerIntegrationService
) {
    
    @GetMapping("/available")
    @Operation(summary = "Get available broker integrations")
    fun getAvailableIntegrations(): ResponseEntity<Map<String, BrokerIntegrationInfo>> {
        return ResponseEntity.ok(brokerIntegrationService.getAvailableIntegrations())
    }
    
    @PostMapping("/test-connection")
    @Operation(summary = "Test connection to a broker")
    fun testBrokerConnection(@Valid @RequestBody request: TestConnectionRequest): ResponseEntity<BrokerConnectionResult> {
        val result = brokerIntegrationService.testBrokerConnection(request.brokerName, request.credentials)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
    
    @PostMapping("/sync-orders")
    @Operation(summary = "Sync orders from broker")
    fun syncOrdersFromBroker(@Valid @RequestBody request: SyncOrdersRequest): ResponseEntity<BrokerSyncResult> {
        val result = brokerIntegrationService.syncOrdersFromBroker(
            request.brokerName,
            request.credentials,
            request.tradingAccountId,
            request.startDate,
            request.endDate
        )
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
    
    @PostMapping("/account-info")
    @Operation(summary = "Fetch account information from broker")
    fun fetchAccountInfo(@Valid @RequestBody request: FetchAccountInfoRequest): ResponseEntity<BrokerAccountResult> {
        val result = brokerIntegrationService.fetchAccountInfoFromBroker(
            request.brokerName,
            request.credentials,
            request.accountId
        )
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}

data class TestConnectionRequest(
    val brokerName: String,
    val credentials: BrokerCredentials
)

data class SyncOrdersRequest(
    val brokerName: String,
    val credentials: BrokerCredentials,
    val tradingAccountId: Long,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val startDate: LocalDateTime,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val endDate: LocalDateTime
)

data class FetchAccountInfoRequest(
    val brokerName: String,
    val credentials: BrokerCredentials,
    val accountId: String
)

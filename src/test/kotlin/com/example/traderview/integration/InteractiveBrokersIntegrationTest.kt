package com.example.traderview.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class InteractiveBrokersIntegrationTest {
    
    @Mock
    private lateinit var restTemplate: RestTemplate
    
    private lateinit var integration: InteractiveBrokersIntegration
    private lateinit var credentials: BrokerCredentials
    
    @BeforeEach
    fun setUp() {
        integration = InteractiveBrokersIntegration(restTemplate, ObjectMapper())
        credentials = BrokerCredentials(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            sandbox = true
        )
    }
    
    @Test
    fun `should return broker name and enabled status`() {
        assertEquals("Interactive Brokers", integration.brokerName)
        assertTrue(integration.isEnabled)
    }
    
    @Test
    fun `should return supported order types`() {
        val orderTypes = integration.getSupportedOrderTypes()
        assertTrue(orderTypes.contains("MARKET"))
        assertTrue(orderTypes.contains("LIMIT"))
        assertTrue(orderTypes.contains("STOP"))
        assertTrue(orderTypes.contains("STOP_LIMIT"))
    }
    
    @Test
    fun `should return supported asset types`() {
        val assetTypes = integration.getSupportedAssetTypes()
        assertTrue(assetTypes.contains("STOCK"))
        assertTrue(assetTypes.contains("OPTION"))
        assertTrue(assetTypes.contains("FUTURE"))
        assertTrue(assetTypes.contains("FOREX"))
    }
    
    @Test
    fun `testConnection should return success when authenticated`() {
        // Given
        val authResponse = IBAuthResponse(authenticated = true, connected = true)
        val accountsResponse = IBAccountsResponse(
            accounts = listOf(
                IBAccount(accountId = "DU123456", accountTitle = "Test Account")
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(IBAuthResponse::class.java)
        )).thenReturn(ResponseEntity.ok(authResponse))
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(IBAccountsResponse::class.java)
        )).thenReturn(ResponseEntity.ok(accountsResponse))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertTrue(result.success)
        assertEquals("Successfully connected to Interactive Brokers", result.message)
        assertEquals(listOf("DU123456"), result.accountsFound)
    }
    
    @Test
    fun `testConnection should return failure when not authenticated`() {
        // Given
        val authResponse = IBAuthResponse(authenticated = false)
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(IBAuthResponse::class.java)
        )).thenReturn(ResponseEntity.ok(authResponse))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Authentication failed", result.message)
    }
    
    @Test
    fun `fetchOrders should return orders when successful`() {
        // Given
        val ordersResponse = IBOrdersResponse(
            orders = listOf(
                IBOrder(
                    orderId = 123456L,
                    ticker = "AAPL",
                    side = "BUY",
                    orderType = "MKT",
                    status = "Filled",
                    totalSize = "100",
                    filledQuantity = "100",
                    avgPrice = "150.25",
                    commission = "1.00",
                    lastExecutionTime = "1640995200000", // 2022-01-01 timestamp
                    exchange = "NASDAQ",
                    currency = "USD"
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(IBOrdersResponse::class.java)
        )).thenReturn(ResponseEntity.ok(ordersResponse))
        
        // When
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "DU123456",
                LocalDateTime.of(2022, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 31, 23, 59)
            )
        }
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.orders.size)
        
        val order = result.orders.first()
        assertEquals("123456", order.brokerOrderId)
        assertEquals("AAPL", order.symbol)
        assertEquals("BUY", order.side)
        assertEquals("MARKET", order.orderType)
        assertEquals("FILLED", order.status)
        assertEquals(100.0, order.quantity)
        assertEquals(100.0, order.filledQuantity)
        assertEquals(150.25, order.avgFillPrice)
        assertEquals(1.0, order.commission)
        assertEquals("STOCK", order.assetType)
    }
    
    @Test
    fun `fetchAccountInfo should return account information when successful`() {
        // Given
        val accountSummary = IBAccountSummaryResponse(
            accountCode = "DU123456",
            accountType = "INDIVIDUAL",
            totalCashValue = "50000.00",
            availableFunds = "45000.00",
            currency = "USD"
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(IBAccountSummaryResponse::class.java)
        )).thenReturn(ResponseEntity.ok(accountSummary))
        
        // When
        val result = runBlocking { integration.fetchAccountInfo(credentials, "DU123456") }
        
        // Then
        assertTrue(result.success)
        val accountInfo = result.accountInfo!!
        assertEquals("DU123456", accountInfo.accountNumber)
        assertEquals("INDIVIDUAL", accountInfo.accountType)
        assertEquals(50000.0, accountInfo.balance)
        assertEquals(45000.0, accountInfo.availableBalance)
        assertEquals("USD", accountInfo.currency)
        assertTrue(accountInfo.isActive)
    }
    
    @Test
    fun `should handle API errors gracefully`() {
        // Given
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(IBAuthResponse::class.java)
        )).thenThrow(RuntimeException("API Error"))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Connection failed", result.message)
        assertEquals("API Error", result.error)
    }
}

// Helper function for testing coroutines
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

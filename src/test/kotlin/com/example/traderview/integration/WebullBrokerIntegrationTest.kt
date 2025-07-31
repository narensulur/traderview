package com.example.traderview.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class WebullBrokerIntegrationTest {
    
    @Mock
    private lateinit var restTemplate: RestTemplate
    
    private lateinit var integration: WebullBrokerIntegration
    private lateinit var credentials: BrokerCredentials
    
    @BeforeEach
    fun setUp() {
        integration = WebullBrokerIntegration(restTemplate, ObjectMapper())
        credentials = BrokerCredentials(
            accessToken = "test-webull-token",
            sandbox = true
        )
    }
    
    @Test
    fun `should return broker name and enabled status`() {
        assertEquals("Webull", integration.brokerName)
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
        assertTrue(assetTypes.contains("ETF"))
        assertTrue(assetTypes.contains("OPTION"))
        assertTrue(assetTypes.contains("CRYPTO"))
    }
    
    @Test
    fun `testConnection should return success when authenticated`() {
        // Given
        val userResponse = WebullUserResponse(
            success = true,
            data = WebullUser(uuid = "test-uuid", username = "testuser")
        )
        
        val accountsResponse = WebullAccountsResponse(
            success = true,
            data = listOf(
                WebullAccount(secAccountId = 12345L, accountType = "INDIVIDUAL")
            )
        )
        
        `when`(restTemplate.exchange(
            contains("/user"),
            any(),
            any(),
            eq(WebullUserResponse::class.java)
        )).thenReturn(ResponseEntity.ok(userResponse))
        
        `when`(restTemplate.exchange(
            contains("/account/getSecAccountList"),
            any(),
            any(),
            eq(WebullAccountsResponse::class.java)
        )).thenReturn(ResponseEntity.ok(accountsResponse))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertTrue(result.success)
        assertEquals("Successfully connected to Webull", result.message)
        assertEquals(listOf("12345"), result.accountsFound)
    }
    
    @Test
    fun `fetchOrders should return orders when successful`() {
        // Given
        val ordersResponse = WebullOrdersResponse(
            success = true,
            data = listOf(
                WebullOrder(
                    orderId = 789012L,
                    tickerId = 913256135L,
                    ticker = "NVDA",
                    action = "BUY",
                    orderType = "LMT",
                    status = "Filled",
                    totalQuantity = "25",
                    filledQuantity = "25",
                    lmtPrice = "220.50",
                    avgFilledPrice = "220.45",
                    fees = "0.00",
                    createTime = 1640995200000L, // 2022-01-01 timestamp
                    updateTime = 1640995500000L,
                    exchange = "NASDAQ",
                    timeInForce = "DAY"
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(WebullOrdersResponse::class.java)
        )).thenReturn(ResponseEntity.ok(ordersResponse))
        
        // When
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "12345",
                LocalDateTime.of(2022, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 31, 23, 59)
            )
        }
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.orders.size)
        
        val order = result.orders.first()
        assertEquals("789012", order.brokerOrderId)
        assertEquals("NVDA", order.symbol)
        assertEquals("BUY", order.side)
        assertEquals("LIMIT", order.orderType)
        assertEquals("FILLED", order.status)
        assertEquals(25.0, order.quantity)
        assertEquals(25.0, order.filledQuantity)
        assertEquals(220.50, order.price)
        assertEquals(220.45, order.avgFillPrice)
        assertEquals(0.0, order.commission) // Webull is commission-free
        assertEquals("STOCK", order.assetType)
    }
    
    @Test
    fun `fetchAccountInfo should return account information when successful`() {
        // Given
        val accountDetailResponse = WebullAccountDetailResponse(
            success = true,
            data = WebullAccountDetail(
                accountType = "INDIVIDUAL",
                netLiquidation = "35000.50",
                dayBuyingPower = "15000.25"
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(WebullAccountDetailResponse::class.java)
        )).thenReturn(ResponseEntity.ok(accountDetailResponse))
        
        // When
        val result = runBlocking { integration.fetchAccountInfo(credentials, "12345") }
        
        // Then
        assertTrue(result.success)
        val accountInfo = result.accountInfo!!
        assertEquals("12345", accountInfo.accountNumber)
        assertEquals("Webull Account", accountInfo.accountName)
        assertEquals("INDIVIDUAL", accountInfo.accountType)
        assertEquals(35000.50, accountInfo.balance)
        assertEquals(15000.25, accountInfo.availableBalance)
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
            eq(WebullUserResponse::class.java)
        )).thenThrow(RuntimeException("Network Error"))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Connection failed", result.message)
        assertEquals("Network Error", result.error)
    }
    
    @Test
    fun `should map Webull order types correctly`() {
        // Test order type mapping through public interface
        val ordersResponse = WebullOrdersResponse(
            success = true,
            data = listOf(
                WebullOrder(
                    orderId = 1L,
                    ticker = "TEST",
                    action = "SELL",
                    orderType = "STP_LMT",
                    status = "PARTIAL",
                    totalQuantity = "10",
                    createTime = System.currentTimeMillis()
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(WebullOrdersResponse::class.java)
        )).thenReturn(ResponseEntity.ok(ordersResponse))
        
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "12345",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
            )
        }
        
        assertTrue(result.success)
        val order = result.orders.first()
        assertEquals("SELL", order.side)
        assertEquals("STOP_LIMIT", order.orderType)
        assertEquals("PARTIALLY_FILLED", order.status)
    }
    
    @Test
    fun `should handle authentication failure`() {
        // Given
        val userResponse = WebullUserResponse(success = false)
        
        `when`(restTemplate.exchange(
            contains("/user"),
            any(),
            any(),
            eq(WebullUserResponse::class.java)
        )).thenReturn(ResponseEntity.ok(userResponse))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Authentication failed", result.message)
    }
}

// Helper function for testing coroutines
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

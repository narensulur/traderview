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
class RobinhoodBrokerIntegrationTest {
    
    @Mock
    private lateinit var restTemplate: RestTemplate
    
    private lateinit var integration: RobinhoodBrokerIntegration
    private lateinit var credentials: BrokerCredentials
    
    @BeforeEach
    fun setUp() {
        integration = RobinhoodBrokerIntegration(restTemplate, ObjectMapper())
        credentials = BrokerCredentials(
            accessToken = "test-robinhood-token",
            sandbox = true
        )
    }
    
    @Test
    fun `should return broker name and enabled status`() {
        assertEquals("Robinhood", integration.brokerName)
        assertTrue(integration.isEnabled)
    }
    
    @Test
    fun `should return supported order types`() {
        val orderTypes = integration.getSupportedOrderTypes()
        assertTrue(orderTypes.contains("MARKET"))
        assertTrue(orderTypes.contains("LIMIT"))
        assertTrue(orderTypes.contains("STOP_LOSS"))
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
        val userResponse = RobinhoodUserResponse(
            username = "testuser@example.com",
            email = "testuser@example.com"
        )
        
        val accountsResponse = RobinhoodAccountsResponse(
            results = listOf(
                RobinhoodAccount(
                    accountNumber = "RH123456",
                    url = "https://robinhood.com/accounts/RH123456/",
                    type = "INDIVIDUAL",
                    deactivated = false
                )
            )
        )
        
        `when`(restTemplate.exchange(
            contains("/user/"),
            any(),
            any(),
            eq(RobinhoodUserResponse::class.java)
        )).thenReturn(ResponseEntity.ok(userResponse))
        
        `when`(restTemplate.exchange(
            contains("/accounts/"),
            any(),
            any(),
            eq(RobinhoodAccountsResponse::class.java)
        )).thenReturn(ResponseEntity.ok(accountsResponse))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertTrue(result.success)
        assertEquals("Successfully connected to Robinhood", result.message)
        assertEquals(listOf("RH123456"), result.accountsFound)
    }
    
    @Test
    fun `fetchOrders should return orders when successful`() {
        // Given
        val ordersResponse = RobinhoodOrdersResponse(
            results = listOf(
                RobinhoodOrder(
                    id = "rh-order-123",
                    instrumentUrl = "https://robinhood.com/instruments/450dfc6d-5510-4d40-abfb-f633b7d9be3e/",
                    side = "buy",
                    type = "limit",
                    state = "filled",
                    quantity = "10",
                    quantityFilled = "10",
                    price = "175.25",
                    averageFillPrice = "175.20",
                    fees = "0.00",
                    createdAt = "2022-01-01T10:00:00.000000Z",
                    updatedAt = "2022-01-01T10:05:00.000000Z",
                    timeInForce = "gfd",
                    trigger = "immediate"
                )
            ),
            next = null
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(RobinhoodOrdersResponse::class.java)
        )).thenReturn(ResponseEntity.ok(ordersResponse))
        
        // When
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "RH123456",
                LocalDateTime.of(2022, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 31, 23, 59)
            )
        }
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.orders.size)
        
        val order = result.orders.first()
        assertEquals("rh-order-123", order.brokerOrderId)
        assertEquals("BUY", order.side)
        assertEquals("LIMIT", order.orderType)
        assertEquals("FILLED", order.status)
        assertEquals(10.0, order.quantity)
        assertEquals(10.0, order.filledQuantity)
        assertEquals(175.25, order.price)
        assertEquals(175.20, order.avgFillPrice)
        assertEquals(0.0, order.commission) // Robinhood is commission-free
        assertEquals(0.0, order.fees)
        assertEquals("STOCK", order.assetType)
    }
    
    @Test
    fun `fetchAccountInfo should return account information when successful`() {
        // Given
        val account = RobinhoodAccount(
            accountNumber = "RH123456",
            portfolioUrl = "https://robinhood.com/accounts/RH123456/portfolio/",
            type = "INDIVIDUAL",
            deactivated = false
        )
        
        val portfolio = RobinhoodPortfolio(
            totalEquity = "25000.75",
            withdrawableAmount = "5000.25"
        )
        
        `when`(restTemplate.exchange(
            contains("/accounts/RH123456/"),
            any(),
            any(),
            eq(RobinhoodAccount::class.java)
        )).thenReturn(ResponseEntity.ok(account))
        
        `when`(restTemplate.exchange(
            contains("/portfolio/"),
            any(),
            any(),
            eq(RobinhoodPortfolio::class.java)
        )).thenReturn(ResponseEntity.ok(portfolio))
        
        // When
        val result = runBlocking { integration.fetchAccountInfo(credentials, "RH123456") }
        
        // Then
        assertTrue(result.success)
        val accountInfo = result.accountInfo!!
        assertEquals("RH123456", accountInfo.accountNumber)
        assertEquals("Robinhood Account", accountInfo.accountName)
        assertEquals("INDIVIDUAL", accountInfo.accountType)
        assertEquals(25000.75, accountInfo.balance)
        assertEquals(5000.25, accountInfo.availableBalance)
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
            eq(RobinhoodUserResponse::class.java)
        )).thenThrow(RuntimeException("API Rate Limit"))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Connection failed", result.message)
        assertEquals("API Rate Limit", result.error)
    }
    
    @Test
    fun `should map Robinhood order states correctly`() {
        // Test order state mapping through public interface
        val ordersResponse = RobinhoodOrdersResponse(
            results = listOf(
                RobinhoodOrder(
                    id = "test-1",
                    side = "sell",
                    type = "stop_loss",
                    state = "partially_filled",
                    quantity = "5",
                    createdAt = LocalDateTime.now().toString() + ".000000Z"
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(RobinhoodOrdersResponse::class.java)
        )).thenReturn(ResponseEntity.ok(ordersResponse))
        
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "RH123456",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
            )
        }
        
        assertTrue(result.success)
        assertTrue(result.orders.isNotEmpty())
        val order = result.orders.first()
        assertEquals("SELL", order.side)
        assertEquals("STOP", order.orderType)
        assertEquals("PARTIALLY_FILLED", order.status)
    }
    
    @Test
    fun `should handle authentication failure`() {
        // Given - return null username indicating auth failure
        val userResponse = RobinhoodUserResponse(username = null)
        
        `when`(restTemplate.exchange(
            contains("/user/"),
            any(),
            any(),
            eq(RobinhoodUserResponse::class.java)
        )).thenReturn(ResponseEntity.ok(userResponse))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Authentication failed", result.message)
    }
    
    @Test
    fun `should handle pagination in fetchOrders`() {
        // Given - first page
        val firstPageResponse = RobinhoodOrdersResponse(
            results = listOf(
                RobinhoodOrder(
                    id = "order-1",
                    side = "buy",
                    quantity = "1",
                    createdAt = "2022-01-01T10:00:00.000000Z"
                )
            ),
            next = "https://robinhood.com/api/orders/?cursor=next-page"
        )
        
        // Second page
        val secondPageResponse = RobinhoodOrdersResponse(
            results = listOf(
                RobinhoodOrder(
                    id = "order-2",
                    side = "sell",
                    quantity = "1",
                    createdAt = "2022-01-01T11:00:00.000000Z"
                )
            ),
            next = null
        )
        
        `when`(restTemplate.exchange(
            contains("cursor="),
            any(),
            any(),
            eq(RobinhoodOrdersResponse::class.java)
        )).thenReturn(ResponseEntity.ok(firstPageResponse))
            .thenReturn(ResponseEntity.ok(secondPageResponse))
        
        // When
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "RH123456",
                LocalDateTime.of(2022, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 31, 23, 59)
            )
        }
        
        // Then
        assertTrue(result.success)
        assertEquals(2, result.orders.size)
        assertEquals("order-1", result.orders[0].brokerOrderId)
        assertEquals("order-2", result.orders[1].brokerOrderId)
    }
}

// Helper function for testing coroutines
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

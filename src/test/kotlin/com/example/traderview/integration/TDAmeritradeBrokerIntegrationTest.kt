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
class TDAmeritradeBrokerIntegrationTest {
    
    @Mock
    private lateinit var restTemplate: RestTemplate
    
    private lateinit var integration: TDAmeritradeBrokerIntegration
    private lateinit var credentials: BrokerCredentials
    
    @BeforeEach
    fun setUp() {
        integration = TDAmeritradeBrokerIntegration(restTemplate, ObjectMapper())
        credentials = BrokerCredentials(
            accessToken = "test-access-token",
            clientId = "test-client-id",
            sandbox = true
        )
    }
    
    @Test
    fun `should return broker name and enabled status`() {
        assertEquals("TD Ameritrade", integration.brokerName)
        assertTrue(integration.isEnabled)
    }
    
    @Test
    fun `should return supported order types`() {
        val orderTypes = integration.getSupportedOrderTypes()
        assertTrue(orderTypes.contains("MARKET"))
        assertTrue(orderTypes.contains("LIMIT"))
        assertTrue(orderTypes.contains("STOP"))
        assertTrue(orderTypes.contains("TRAILING_STOP"))
    }
    
    @Test
    fun `should return supported asset types`() {
        val assetTypes = integration.getSupportedAssetTypes()
        assertTrue(assetTypes.contains("EQUITY"))
        assertTrue(assetTypes.contains("OPTION"))
        assertTrue(assetTypes.contains("ETF"))
        assertTrue(assetTypes.contains("MUTUAL_FUND"))
    }
    
    @Test
    fun `testConnection should return success when authenticated`() {
        // Given
        val accounts = arrayOf(
            TDAccount(
                securitiesAccount = TDSecuritiesAccount(
                    accountId = "123456789",
                    type = "MARGIN"
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(Array<TDAccount>::class.java)
        )).thenReturn(ResponseEntity.ok(accounts))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertTrue(result.success)
        assertEquals("Successfully connected to TD Ameritrade", result.message)
        assertEquals(listOf("123456789"), result.accountsFound)
    }
    
    @Test
    fun `fetchOrders should return orders when successful`() {
        // Given
        val orders = arrayOf(
            TDOrder(
                orderId = 987654321L,
                orderType = "LIMIT",
                status = "FILLED",
                enteredTime = "2022-01-01T10:00:00+0000",
                closeTime = "2022-01-01T10:05:00+0000",
                quantity = "50",
                filledQuantity = "50",
                price = "155.50",
                orderStrategyType = "SINGLE",
                duration = "DAY",
                orderLegCollection = listOf(
                    TDOrderLeg(
                        instruction = "BUY",
                        quantity = "50",
                        instrument = TDInstrument(
                            symbol = "TSLA",
                            assetType = "EQUITY"
                        )
                    )
                ),
                orderActivityCollection = listOf(
                    TDOrderActivity(
                        executionLegs = listOf(
                            TDExecutionLeg(
                                price = "155.50",
                                commission = "0.00"
                            )
                        )
                    )
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(Array<TDOrder>::class.java)
        )).thenReturn(ResponseEntity.ok(orders))
        
        // When
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "123456789",
                LocalDateTime.of(2022, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 31, 23, 59)
            )
        }
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.orders.size)
        
        val order = result.orders.first()
        assertEquals("987654321", order.brokerOrderId)
        assertEquals("TSLA", order.symbol)
        assertEquals("BUY", order.side)
        assertEquals("LIMIT", order.orderType)
        assertEquals("FILLED", order.status)
        assertEquals(50.0, order.quantity)
        assertEquals(50.0, order.filledQuantity)
        assertEquals(155.50, order.avgFillPrice)
        assertEquals(0.0, order.commission)
        assertEquals("STOCK", order.assetType)
    }
    
    @Test
    fun `fetchAccountInfo should return account information when successful`() {
        // Given
        val account = TDAccount(
            securitiesAccount = TDSecuritiesAccount(
                accountId = "123456789",
                type = "MARGIN",
                currentBalances = TDBalances(
                    liquidationValue = "75000.00",
                    availableFunds = "25000.00"
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(TDAccount::class.java)
        )).thenReturn(ResponseEntity.ok(account))
        
        // When
        val result = runBlocking { integration.fetchAccountInfo(credentials, "123456789") }
        
        // Then
        assertTrue(result.success)
        val accountInfo = result.accountInfo!!
        assertEquals("123456789", accountInfo.accountNumber)
        assertEquals("MARGIN", accountInfo.accountType)
        assertEquals(75000.0, accountInfo.balance)
        assertEquals(25000.0, accountInfo.availableBalance)
        assertEquals("USD", accountInfo.currency)
        assertTrue(accountInfo.isActive)
    }
    
    @Test
    fun `should handle authentication errors gracefully`() {
        // Given
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(Array<TDAccount>::class.java)
        )).thenThrow(RuntimeException("Unauthorized"))
        
        // When
        val result = runBlocking { integration.testConnection(credentials) }
        
        // Then
        assertFalse(result.success)
        assertEquals("Connection failed", result.message)
        assertEquals("Unauthorized", result.error)
    }
    
    @Test
    fun `should map TD order instructions correctly`() {
        // Test the private mapping functions through public interface
        val orders = arrayOf(
            TDOrder(
                orderId = 1L,
                orderLegCollection = listOf(
                    TDOrderLeg(instruction = "SELL_SHORT", instrument = TDInstrument(symbol = "SPY"))
                )
            )
        )
        
        `when`(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(Array<TDOrder>::class.java)
        )).thenReturn(ResponseEntity.ok(orders))
        
        val result = runBlocking {
            integration.fetchOrders(
                credentials,
                "123456789",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
            )
        }
        
        assertTrue(result.success)
        assertEquals("SELL", result.orders.first().side)
    }
}

// Helper function for testing coroutines
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

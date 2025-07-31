package com.example.traderview.service

import com.example.traderview.entity.*
import com.example.traderview.integration.*
import com.example.traderview.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class BrokerIntegrationServiceTest {
    
    @Mock
    private lateinit var interactiveBrokersIntegration: InteractiveBrokersIntegration
    
    @Mock
    private lateinit var tdAmeritradeIntegration: TDAmeritradeBrokerIntegration
    
    @Mock
    private lateinit var webullIntegration: WebullBrokerIntegration
    
    @Mock
    private lateinit var robinhoodIntegration: RobinhoodBrokerIntegration
    
    @Mock
    private lateinit var brokerRepository: BrokerRepository
    
    @Mock
    private lateinit var tradingAccountRepository: TradingAccountRepository
    
    @Mock
    private lateinit var symbolRepository: SymbolRepository
    
    @Mock
    private lateinit var orderService: OrderService
    
    private lateinit var brokerIntegrationService: BrokerIntegrationService
    private lateinit var credentials: BrokerCredentials
    private lateinit var tradingAccount: TradingAccount
    private lateinit var broker: Broker
    
    @BeforeEach
    fun setUp() {
        brokerIntegrationService = BrokerIntegrationService(
            interactiveBrokersIntegration,
            tdAmeritradeIntegration,
            webullIntegration,
            robinhoodIntegration,
            brokerRepository,
            tradingAccountRepository,
            symbolRepository,
            orderService
        )
        
        credentials = BrokerCredentials(
            accessToken = "test-token",
            sandbox = true
        )
        
        broker = Broker(
            id = 1L,
            name = "interactive_brokers",
            displayName = "Interactive Brokers",
            apiEndpoint = "https://api.interactivebrokers.com",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        tradingAccount = TradingAccount(
            id = 1L,
            accountNumber = "DU123456",
            accountName = "Test Account",
            broker = broker,
            initialBalance = BigDecimal("100000.00"),
            currentBalance = BigDecimal("105000.00"),
            currency = "USD",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    @Test
    fun `getAvailableIntegrations should return all broker integrations`() {
        // Given
        `when`(interactiveBrokersIntegration.brokerName).thenReturn("Interactive Brokers")
        `when`(interactiveBrokersIntegration.isEnabled).thenReturn(true)
        `when`(interactiveBrokersIntegration.getSupportedOrderTypes()).thenReturn(listOf("MARKET", "LIMIT"))
        `when`(interactiveBrokersIntegration.getSupportedAssetTypes()).thenReturn(listOf("STOCK", "OPTION"))
        
        `when`(tdAmeritradeIntegration.brokerName).thenReturn("TD Ameritrade")
        `when`(tdAmeritradeIntegration.isEnabled).thenReturn(true)
        `when`(tdAmeritradeIntegration.getSupportedOrderTypes()).thenReturn(listOf("MARKET", "LIMIT"))
        `when`(tdAmeritradeIntegration.getSupportedAssetTypes()).thenReturn(listOf("EQUITY", "OPTION"))
        
        `when`(webullIntegration.brokerName).thenReturn("Webull")
        `when`(webullIntegration.isEnabled).thenReturn(true)
        `when`(webullIntegration.getSupportedOrderTypes()).thenReturn(listOf("MARKET", "LIMIT"))
        `when`(webullIntegration.getSupportedAssetTypes()).thenReturn(listOf("STOCK", "ETF"))
        
        `when`(robinhoodIntegration.brokerName).thenReturn("Robinhood")
        `when`(robinhoodIntegration.isEnabled).thenReturn(true)
        `when`(robinhoodIntegration.getSupportedOrderTypes()).thenReturn(listOf("MARKET", "LIMIT"))
        `when`(robinhoodIntegration.getSupportedAssetTypes()).thenReturn(listOf("STOCK", "ETF"))
        
        // When
        val integrations = brokerIntegrationService.getAvailableIntegrations()
        
        // Then
        assertEquals(4, integrations.size)
        assertTrue(integrations.containsKey("interactive_brokers"))
        assertTrue(integrations.containsKey("td_ameritrade"))
        assertTrue(integrations.containsKey("webull"))
        assertTrue(integrations.containsKey("robinhood"))
        
        val ibInfo = integrations["interactive_brokers"]!!
        assertEquals("Interactive Brokers", ibInfo.name)
        assertTrue(ibInfo.isEnabled)
        assertTrue(ibInfo.supportedOrderTypes.contains("MARKET"))
        assertTrue(ibInfo.supportedAssetTypes.contains("STOCK"))
    }
    
    @Test
    fun `testBrokerConnection should return success for valid broker`() {
        // Given
        val expectedResult = BrokerConnectionResult(
            success = true,
            message = "Connected successfully",
            accountsFound = listOf("DU123456")
        )
        
        `when`(runBlocking { interactiveBrokersIntegration.testConnection(credentials) }).thenReturn(
            expectedResult
        )
        
        // When
        val result = brokerIntegrationService.testBrokerConnection("interactive_brokers", credentials)
        
        // Then
        assertTrue(result.success)
        assertEquals("Connected successfully", result.message)
        assertEquals(listOf("DU123456"), result.accountsFound)
    }
    
    @Test
    fun `testBrokerConnection should return error for unsupported broker`() {
        // When
        val result = brokerIntegrationService.testBrokerConnection("unsupported_broker", credentials)
        
        // Then
        assertFalse(result.success)
        assertEquals("Broker integration not found", result.message)
        assertEquals("Unsupported broker: unsupported_broker", result.error)
    }
    
    @Test
    fun `syncOrdersFromBroker should successfully import orders`() {
        // Given
        val startDate = LocalDateTime.of(2022, 1, 1, 0, 0)
        val endDate = LocalDateTime.of(2022, 1, 31, 23, 59)
        
        val brokerOrders = listOf(
            BrokerOrderData(
                brokerOrderId = "IB123456",
                symbol = "AAPL",
                side = "BUY",
                orderType = "MARKET",
                status = "FILLED",
                quantity = 100.0,
                filledQuantity = 100.0,
                avgFillPrice = 150.25,
                commission = 1.0,
                fees = 0.5,
                placedAt = LocalDateTime.of(2022, 1, 15, 10, 0),
                filledAt = LocalDateTime.of(2022, 1, 15, 10, 1),
                assetType = "STOCK",
                exchange = "NASDAQ",
                currency = "USD"
            )
        )
        
        val ordersResult = BrokerOrdersResult(
            success = true,
            orders = brokerOrders,
            totalCount = 1,
            message = "Orders fetched successfully"
        )
        
        val symbol = Symbol(
            id = 1L,
            ticker = "AAPL",
            name = "Apple Inc.",
            exchange = "NASDAQ",
            assetType = AssetType.STOCK,
            currency = "USD",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        `when`(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(tradingAccount))
        `when`(runBlocking { interactiveBrokersIntegration.fetchOrders(credentials, "DU123456", startDate, endDate) })
            .thenReturn(ordersResult)
        `when`(orderService.getOrderByBrokerOrderId("IB123456")).thenReturn(null)
        `when`(symbolRepository.findByTicker("AAPL")).thenReturn(symbol)
        `when`(orderService.createOrder(any())).thenReturn(mock())
        
        // When
        val result = brokerIntegrationService.syncOrdersFromBroker(
            "interactive_brokers",
            credentials,
            1L,
            startDate,
            endDate
        )
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.importedOrders)
        assertEquals(0, result.skippedOrders)
        assertEquals(1, result.totalFetched)
        assertTrue(result.errors.isEmpty())
        
        verify(orderService).createOrder(any())
    }
    
    @Test
    fun `syncOrdersFromBroker should skip existing orders`() {
        // Given
        val startDate = LocalDateTime.of(2022, 1, 1, 0, 0)
        val endDate = LocalDateTime.of(2022, 1, 31, 23, 59)
        
        val brokerOrders = listOf(
            BrokerOrderData(
                brokerOrderId = "EXISTING123",
                symbol = "TSLA",
                side = "SELL",
                orderType = "LIMIT",
                status = "FILLED",
                quantity = 50.0,
                placedAt = LocalDateTime.now(),
                assetType = "STOCK"
            )
        )
        
        val ordersResult = BrokerOrdersResult(
            success = true,
            orders = brokerOrders,
            totalCount = 1
        )
        
        `when`(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(tradingAccount))
        `when`(runBlocking { interactiveBrokersIntegration.fetchOrders(credentials, "DU123456", startDate, endDate) })
            .thenReturn(ordersResult)
        `when`(orderService.getOrderByBrokerOrderId("EXISTING123")).thenReturn(mock())
        
        // When
        val result = brokerIntegrationService.syncOrdersFromBroker(
            "interactive_brokers",
            credentials,
            1L,
            startDate,
            endDate
        )
        
        // Then
        assertTrue(result.success)
        assertEquals(0, result.importedOrders)
        assertEquals(1, result.skippedOrders)
        assertEquals(1, result.totalFetched)
        
        verify(orderService, never()).createOrder(any())
    }
    
    @Test
    fun `syncOrdersFromBroker should handle trading account not found`() {
        // Given
        `when`(tradingAccountRepository.findById(999L)).thenReturn(Optional.empty())
        
        // When
        val result = brokerIntegrationService.syncOrdersFromBroker(
            "interactive_brokers",
            credentials,
            999L,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        )
        
        // Then
        assertFalse(result.success)
        assertEquals("Trading account not found", result.message)
        assertEquals("Trading account with ID 999 not found", result.error)
    }
    
    @Test
    fun `fetchAccountInfoFromBroker should return account information`() {
        // Given
        val expectedAccountInfo = BrokerAccountInfo(
            accountNumber = "DU123456",
            accountName = "Test Account",
            accountType = "INDIVIDUAL",
            balance = 50000.0,
            availableBalance = 25000.0,
            currency = "USD",
            isActive = true
        )
        
        val accountResult = BrokerAccountResult(
            success = true,
            accountInfo = expectedAccountInfo
        )
        
        `when`(runBlocking { interactiveBrokersIntegration.fetchAccountInfo(credentials, "DU123456") })
            .thenReturn(accountResult)
        
        // When
        val result = brokerIntegrationService.fetchAccountInfoFromBroker(
            "interactive_brokers",
            credentials,
            "DU123456"
        )
        
        // Then
        assertTrue(result.success)
        assertEquals(expectedAccountInfo, result.accountInfo)
    }
    
    @Test
    fun `should create new symbol when not found`() {
        // Given
        val brokerOrder = BrokerOrderData(
            brokerOrderId = "NEW123",
            symbol = "NVDA",
            side = "BUY",
            orderType = "MARKET",
            status = "FILLED",
            quantity = 25.0,
            placedAt = LocalDateTime.now(),
            assetType = "STOCK",
            exchange = "NASDAQ",
            currency = "USD"
        )
        
        val ordersResult = BrokerOrdersResult(
            success = true,
            orders = listOf(brokerOrder),
            totalCount = 1
        )
        
        val newSymbol = Symbol(
            id = 2L,
            ticker = "NVDA",
            name = "NVDA",
            exchange = "NASDAQ",
            assetType = AssetType.STOCK,
            currency = "USD",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        `when`(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(tradingAccount))
        `when`(runBlocking { interactiveBrokersIntegration.fetchOrders(any(), any(), any(), any()) })
            .thenReturn(ordersResult)
        `when`(orderService.getOrderByBrokerOrderId("NEW123")).thenReturn(null)
        `when`(symbolRepository.findByTicker("NVDA")).thenReturn(null)
        `when`(symbolRepository.save(any<Symbol>())).thenReturn(newSymbol)
        `when`(orderService.createOrder(any())).thenReturn(mock())
        
        // When
        val result = brokerIntegrationService.syncOrdersFromBroker(
            "interactive_brokers",
            credentials,
            1L,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        )
        
        // Then
        assertTrue(result.success)
        assertEquals(1, result.importedOrders)
        verify(symbolRepository).save(any<Symbol>())
        verify(orderService).createOrder(any())
    }
}

// Helper function for mocking
private inline fun <reified T> any(): T = org.mockito.kotlin.any()

// Helper function for testing coroutines
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

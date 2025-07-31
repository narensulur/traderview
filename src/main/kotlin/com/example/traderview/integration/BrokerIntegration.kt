package com.example.traderview.integration

import com.example.traderview.dto.CreateOrderRequest
import com.example.traderview.entity.TradingAccount
import java.time.LocalDateTime

/**
 * Base interface for all broker integrations
 */
interface BrokerIntegration {
    val brokerName: String
    val isEnabled: Boolean
    
    /**
     * Test connection to broker API
     */
    suspend fun testConnection(credentials: BrokerCredentials): BrokerConnectionResult
    
    /**
     * Fetch orders for a specific account within date range
     */
    suspend fun fetchOrders(
        credentials: BrokerCredentials,
        accountId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BrokerOrdersResult
    
    /**
     * Fetch account information
     */
    suspend fun fetchAccountInfo(credentials: BrokerCredentials, accountId: String): BrokerAccountResult
    
    /**
     * Get supported order types for this broker
     */
    fun getSupportedOrderTypes(): List<String>
    
    /**
     * Get supported asset types for this broker
     */
    fun getSupportedAssetTypes(): List<String>
}

data class BrokerCredentials(
    val apiKey: String? = null,
    val apiSecret: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val accountNumber: String? = null,
    val clientId: String? = null,
    val redirectUri: String? = null,
    val sandbox: Boolean = true,
    val additionalParams: Map<String, String> = emptyMap()
)

data class BrokerConnectionResult(
    val success: Boolean,
    val message: String,
    val accountsFound: List<String> = emptyList(),
    val error: String? = null
)

data class BrokerOrdersResult(
    val success: Boolean,
    val orders: List<BrokerOrderData> = emptyList(),
    val totalCount: Int = 0,
    val message: String = "",
    val error: String? = null
)

data class BrokerAccountResult(
    val success: Boolean,
    val accountInfo: BrokerAccountInfo? = null,
    val error: String? = null
)

data class BrokerOrderData(
    val brokerOrderId: String,
    val symbol: String,
    val side: String, // BUY, SELL
    val orderType: String, // MARKET, LIMIT, STOP, etc.
    val status: String, // FILLED, CANCELLED, etc.
    val quantity: Double,
    val filledQuantity: Double? = null,
    val price: Double? = null,
    val avgFillPrice: Double? = null,
    val commission: Double? = null,
    val fees: Double? = null,
    val placedAt: LocalDateTime,
    val filledAt: LocalDateTime? = null,
    val cancelledAt: LocalDateTime? = null,
    val assetType: String = "STOCK", // STOCK, OPTION, FUTURE, etc.
    val exchange: String? = null,
    val currency: String = "USD",
    val metadata: Map<String, Any> = emptyMap()
)

data class BrokerAccountInfo(
    val accountNumber: String,
    val accountName: String,
    val accountType: String,
    val balance: Double,
    val availableBalance: Double,
    val currency: String = "USD",
    val isActive: Boolean = true
)

enum class BrokerType {
    INTERACTIVE_BROKERS,
    TD_AMERITRADE,
    WEBULL,
    ROBINHOOD,
    ALPACA,
    E_TRADE,
    SCHWAB
}

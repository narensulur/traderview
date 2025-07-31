package com.example.traderview.service

import com.example.traderview.dto.CreateOrderRequest
import com.example.traderview.entity.*
import com.example.traderview.integration.*
import com.example.traderview.repository.*
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class BrokerIntegrationService(
    private val interactiveBrokersIntegration: InteractiveBrokersIntegration,
    private val tdAmeritradeIntegration: TDAmeritradeBrokerIntegration,
    private val webullIntegration: WebullBrokerIntegration,
    private val robinhoodIntegration: RobinhoodBrokerIntegration,
    private val brokerRepository: BrokerRepository,
    private val tradingAccountRepository: TradingAccountRepository,
    private val symbolRepository: SymbolRepository,
    private val orderService: OrderService
) {
    
    private val integrations = mapOf(
        "interactive_brokers" to interactiveBrokersIntegration,
        "td_ameritrade" to tdAmeritradeIntegration,
        "webull" to webullIntegration,
        "robinhood" to robinhoodIntegration
    )
    
    fun getAvailableIntegrations(): Map<String, BrokerIntegrationInfo> {
        return integrations.mapValues { (_, integration) ->
            BrokerIntegrationInfo(
                name = integration.brokerName,
                isEnabled = integration.isEnabled,
                supportedOrderTypes = integration.getSupportedOrderTypes(),
                supportedAssetTypes = integration.getSupportedAssetTypes()
            )
        }
    }
    
    fun testBrokerConnection(brokerName: String, credentials: BrokerCredentials): BrokerConnectionResult {
        val integration = integrations[brokerName.lowercase()]
            ?: return BrokerConnectionResult(
                success = false,
                message = "Broker integration not found",
                error = "Unsupported broker: $brokerName"
            )
        
        return runBlocking {
            integration.testConnection(credentials)
        }
    }
    
    fun syncOrdersFromBroker(
        brokerName: String,
        credentials: BrokerCredentials,
        tradingAccountId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BrokerSyncResult {
        val integration = integrations[brokerName.lowercase()]
            ?: return BrokerSyncResult(
                success = false,
                message = "Broker integration not found",
                error = "Unsupported broker: $brokerName"
            )
        
        return runBlocking {
            try {
                val tradingAccount = tradingAccountRepository.findById(tradingAccountId).orElse(null)
                    ?: return@runBlocking BrokerSyncResult(
                        success = false,
                        message = "Trading account not found",
                        error = "Trading account with ID $tradingAccountId not found"
                    )
                
                // Fetch orders from broker
                val ordersResult = integration.fetchOrders(
                    credentials,
                    tradingAccount.accountNumber,
                    startDate,
                    endDate
                )
                
                if (!ordersResult.success) {
                    return@runBlocking BrokerSyncResult(
                        success = false,
                        message = "Failed to fetch orders from broker",
                        error = ordersResult.error
                    )
                }
                
                // Convert and save orders
                var importedCount = 0
                var skippedCount = 0
                val errors = mutableListOf<String>()
                
                ordersResult.orders.forEach { brokerOrder ->
                    try {
                        // Check if order already exists
                        val existingOrder = orderService.getOrderByBrokerOrderId(brokerOrder.brokerOrderId)
                        if (existingOrder != null) {
                            skippedCount++
                            return@forEach
                        }
                        
                        // Find or create symbol
                        val symbol = findOrCreateSymbol(brokerOrder)
                        
                        // Create order request
                        val orderRequest = CreateOrderRequest(
                            brokerOrderId = brokerOrder.brokerOrderId,
                            tradingAccountId = tradingAccountId,
                            symbolId = symbol.id,
                            side = OrderSide.valueOf(brokerOrder.side),
                            type = mapToOrderType(brokerOrder.orderType),
                            status = mapToOrderStatus(brokerOrder.status),
                            quantity = BigDecimal.valueOf(brokerOrder.quantity),
                            filledQuantity = brokerOrder.filledQuantity?.let { BigDecimal.valueOf(it) },
                            price = brokerOrder.price?.let { BigDecimal.valueOf(it) },
                            avgFillPrice = brokerOrder.avgFillPrice?.let { BigDecimal.valueOf(it) },
                            commission = brokerOrder.commission?.let { BigDecimal.valueOf(it) },
                            fees = brokerOrder.fees?.let { BigDecimal.valueOf(it) },
                            placedAt = brokerOrder.placedAt,
                            filledAt = brokerOrder.filledAt
                        )
                        
                        orderService.createOrder(orderRequest)
                        importedCount++
                        
                    } catch (e: Exception) {
                        errors.add("Failed to import order ${brokerOrder.brokerOrderId}: ${e.message}")
                    }
                }
                
                BrokerSyncResult(
                    success = true,
                    message = "Successfully synced orders from $brokerName",
                    importedOrders = importedCount,
                    skippedOrders = skippedCount,
                    totalFetched = ordersResult.totalCount,
                    errors = errors
                )
                
            } catch (e: Exception) {
                BrokerSyncResult(
                    success = false,
                    message = "Sync failed",
                    error = e.message ?: "Unknown error during sync"
                )
            }
        }
    }
    
    fun fetchAccountInfoFromBroker(
        brokerName: String,
        credentials: BrokerCredentials,
        accountId: String
    ): BrokerAccountResult {
        val integration = integrations[brokerName.lowercase()]
            ?: return BrokerAccountResult(
                success = false,
                error = "Unsupported broker: $brokerName"
            )
        
        return runBlocking {
            integration.fetchAccountInfo(credentials, accountId)
        }
    }
    
    private fun findOrCreateSymbol(brokerOrder: BrokerOrderData): Symbol {
        return symbolRepository.findByTicker(brokerOrder.symbol) ?: run {
            val assetType = when (brokerOrder.assetType.uppercase()) {
                "STOCK", "EQUITY" -> AssetType.STOCK
                "OPTION" -> AssetType.OPTION
                "FUTURE" -> AssetType.FUTURE
                "FOREX", "CASH" -> AssetType.FOREX
                "ETF" -> AssetType.ETF
                "BOND", "FIXED_INCOME" -> AssetType.BOND
                "CRYPTO" -> AssetType.CRYPTO
                else -> AssetType.STOCK
            }
            
            val newSymbol = Symbol(
                ticker = brokerOrder.symbol,
                name = brokerOrder.symbol, // Could be enhanced with symbol lookup
                exchange = brokerOrder.exchange ?: "UNKNOWN",
                assetType = assetType,
                currency = brokerOrder.currency,
                contractMultiplier = BigDecimal.ONE, // Default multiplier, could be enhanced
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            symbolRepository.save(newSymbol)
        }
    }
    
    private fun mapToOrderType(brokerOrderType: String): OrderType {
        return when (brokerOrderType.uppercase()) {
            "MARKET" -> OrderType.MARKET
            "LIMIT" -> OrderType.LIMIT
            "STOP", "STOP_LOSS" -> OrderType.STOP
            "STOP_LIMIT" -> OrderType.STOP_LIMIT
            else -> OrderType.MARKET
        }
    }
    
    private fun mapToOrderStatus(brokerStatus: String): OrderStatus {
        return when (brokerStatus.uppercase()) {
            "FILLED" -> OrderStatus.FILLED
            "CANCELLED" -> OrderStatus.CANCELLED
            "PENDING" -> OrderStatus.PENDING
            "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED
            "REJECTED" -> OrderStatus.REJECTED
            else -> OrderStatus.PENDING
        }
    }
}

data class BrokerIntegrationInfo(
    val name: String,
    val isEnabled: Boolean,
    val supportedOrderTypes: List<String>,
    val supportedAssetTypes: List<String>
)

data class BrokerSyncResult(
    val success: Boolean,
    val message: String,
    val importedOrders: Int = 0,
    val skippedOrders: Int = 0,
    val totalFetched: Int = 0,
    val errors: List<String> = emptyList(),
    val error: String? = null
)

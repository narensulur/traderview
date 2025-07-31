package com.example.traderview.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class InteractiveBrokersIntegration(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : BrokerIntegration {
    
    override val brokerName = "Interactive Brokers"
    override val isEnabled = true
    
    private val baseUrl = "https://localhost:5000/v1/api" // IB Gateway/TWS API
    private val sandboxUrl = "https://cdcdyn.interactivebrokers.com/portal.proxy/v1/api"
    
    override suspend fun testConnection(credentials: BrokerCredentials): BrokerConnectionResult {
        return try {
            val url = if (credentials.sandbox) sandboxUrl else baseUrl
            val headers = createHeaders(credentials)
            
            val response = restTemplate.exchange(
                "$url/iserver/auth/status",
                HttpMethod.POST,
                HttpEntity(null, headers),
                IBAuthResponse::class.java
            )
            
            if (response.statusCode.is2xxSuccessful && response.body?.authenticated == true) {
                // Fetch available accounts
                val accountsResponse = restTemplate.exchange(
                    "$url/iserver/accounts",
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                    IBAccountsResponse::class.java
                )
                
                val accounts = accountsResponse.body?.accounts?.map { it.accountId } ?: emptyList()
                
                BrokerConnectionResult(
                    success = true,
                    message = "Successfully connected to Interactive Brokers",
                    accountsFound = accounts
                )
            } else {
                BrokerConnectionResult(
                    success = false,
                    message = "Authentication failed",
                    error = "Invalid credentials or session expired"
                )
            }
        } catch (e: Exception) {
            BrokerConnectionResult(
                success = false,
                message = "Connection failed",
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    override suspend fun fetchOrders(
        credentials: BrokerCredentials,
        accountId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BrokerOrdersResult {
        return try {
            val url = if (credentials.sandbox) sandboxUrl else baseUrl
            val headers = createHeaders(credentials)
            
            // IB API requires specific date format
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss")
            
            val response = restTemplate.exchange(
                "$url/iserver/account/$accountId/orders",
                HttpMethod.GET,
                HttpEntity(null, headers),
                IBOrdersResponse::class.java
            )
            
            val orders = response.body?.orders?.map { ibOrder ->
                BrokerOrderData(
                    brokerOrderId = ibOrder.orderId.toString(),
                    symbol = ibOrder.ticker ?: "UNKNOWN",
                    side = if (ibOrder.side == "BUY") "BUY" else "SELL",
                    orderType = mapIBOrderType(ibOrder.orderType),
                    status = mapIBOrderStatus(ibOrder.status),
                    quantity = ibOrder.totalSize?.toDouble() ?: 0.0,
                    filledQuantity = ibOrder.filledQuantity?.toDouble(),
                    price = ibOrder.price?.toDouble(),
                    avgFillPrice = ibOrder.avgPrice?.toDouble(),
                    commission = ibOrder.commission?.toDouble(),
                    fees = 0.0, // IB includes fees in commission
                    placedAt = parseIBDateTime(ibOrder.lastExecutionTime),
                    filledAt = if (ibOrder.status == "Filled") parseIBDateTime(ibOrder.lastExecutionTime) else null,
                    assetType = mapIBAssetType(ibOrder.secType),
                    exchange = ibOrder.exchange,
                    currency = ibOrder.currency ?: "USD",
                    metadata = mapOf(
                        "conid" to (ibOrder.conid ?: 0),
                        "parentId" to (ibOrder.parentId ?: 0),
                        "orderRef" to (ibOrder.orderRef ?: "")
                    )
                )
            } ?: emptyList()
            
            BrokerOrdersResult(
                success = true,
                orders = orders,
                totalCount = orders.size,
                message = "Successfully fetched ${orders.size} orders from Interactive Brokers"
            )
            
        } catch (e: Exception) {
            BrokerOrdersResult(
                success = false,
                error = e.message ?: "Failed to fetch orders from Interactive Brokers"
            )
        }
    }
    
    override suspend fun fetchAccountInfo(credentials: BrokerCredentials, accountId: String): BrokerAccountResult {
        return try {
            val url = if (credentials.sandbox) sandboxUrl else baseUrl
            val headers = createHeaders(credentials)
            
            val response = restTemplate.exchange(
                "$url/iserver/account/$accountId/summary",
                HttpMethod.GET,
                HttpEntity(null, headers),
                IBAccountSummaryResponse::class.java
            )
            
            val summary = response.body
            if (summary != null) {
                BrokerAccountResult(
                    success = true,
                    accountInfo = BrokerAccountInfo(
                        accountNumber = accountId,
                        accountName = summary.accountType ?: "IB Account",
                        accountType = summary.accountType ?: "INDIVIDUAL",
                        balance = summary.totalCashValue?.toDouble() ?: 0.0,
                        availableBalance = summary.availableFunds?.toDouble() ?: 0.0,
                        currency = summary.currency ?: "USD",
                        isActive = true
                    )
                )
            } else {
                BrokerAccountResult(
                    success = false,
                    error = "Failed to fetch account information"
                )
            }
        } catch (e: Exception) {
            BrokerAccountResult(
                success = false,
                error = e.message ?: "Failed to fetch account info from Interactive Brokers"
            )
        }
    }
    
    override fun getSupportedOrderTypes(): List<String> {
        return listOf("MARKET", "LIMIT", "STOP", "STOP_LIMIT", "TRAIL", "TRAIL_LIMIT")
    }
    
    override fun getSupportedAssetTypes(): List<String> {
        return listOf("STOCK", "OPTION", "FUTURE", "FOREX", "BOND", "FUND", "CRYPTO")
    }
    
    private fun createHeaders(credentials: BrokerCredentials): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("User-Agent", "TraderView/1.0")
        // IB uses session-based authentication
        return headers
    }
    
    private fun mapIBOrderType(ibType: String?): String {
        return when (ibType?.uppercase()) {
            "MKT" -> "MARKET"
            "LMT" -> "LIMIT"
            "STP" -> "STOP"
            "STP LMT" -> "STOP_LIMIT"
            "TRAIL" -> "TRAIL"
            else -> "MARKET"
        }
    }
    
    private fun mapIBOrderStatus(ibStatus: String?): String {
        return when (ibStatus?.uppercase()) {
            "FILLED" -> "FILLED"
            "CANCELLED" -> "CANCELLED"
            "SUBMITTED" -> "PENDING"
            "PRESUBMITTED" -> "PENDING"
            "PARTIALFILLED" -> "PARTIALLY_FILLED"
            else -> "PENDING"
        }
    }
    
    private fun mapIBAssetType(secType: String?): String {
        return when (secType?.uppercase()) {
            "STK" -> "STOCK"
            "OPT" -> "OPTION"
            "FUT" -> "FUTURE"
            "CASH" -> "FOREX"
            "BOND" -> "BOND"
            "FUND" -> "ETF"
            "CRYPTO" -> "CRYPTO"
            else -> "STOCK"
        }
    }
    
    private fun parseIBDateTime(dateTimeStr: String?): LocalDateTime {
        return try {
            if (dateTimeStr.isNullOrBlank()) {
                LocalDateTime.now()
            } else {
                // IB typically returns timestamps in milliseconds
                val timestamp = dateTimeStr.toLongOrNull()
                if (timestamp != null) {
                    LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
                } else {
                    LocalDateTime.now()
                }
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

// IB API Response DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class IBAuthResponse(
    val authenticated: Boolean? = false,
    val competing: Boolean? = false,
    val connected: Boolean? = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IBAccountsResponse(
    val accounts: List<IBAccount> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IBAccount(
    @JsonProperty("id") val accountId: String,
    @JsonProperty("accountVan") val accountVan: String? = null,
    @JsonProperty("accountTitle") val accountTitle: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IBOrdersResponse(
    val orders: List<IBOrder> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IBOrder(
    @JsonProperty("orderId") val orderId: Long? = null,
    @JsonProperty("conid") val conid: Long? = null,
    @JsonProperty("ticker") val ticker: String? = null,
    @JsonProperty("secType") val secType: String? = null,
    @JsonProperty("side") val side: String? = null,
    @JsonProperty("orderType") val orderType: String? = null,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("totalSize") val totalSize: String? = null,
    @JsonProperty("filledQuantity") val filledQuantity: String? = null,
    @JsonProperty("price") val price: String? = null,
    @JsonProperty("avgPrice") val avgPrice: String? = null,
    @JsonProperty("commission") val commission: String? = null,
    @JsonProperty("lastExecutionTime") val lastExecutionTime: String? = null,
    @JsonProperty("exchange") val exchange: String? = null,
    @JsonProperty("currency") val currency: String? = null,
    @JsonProperty("parentId") val parentId: Long? = null,
    @JsonProperty("orderRef") val orderRef: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IBAccountSummaryResponse(
    @JsonProperty("accountcode") val accountCode: String? = null,
    @JsonProperty("accounttype") val accountType: String? = null,
    @JsonProperty("totalcashvalue") val totalCashValue: String? = null,
    @JsonProperty("availablefunds") val availableFunds: String? = null,
    @JsonProperty("currency") val currency: String? = null
)

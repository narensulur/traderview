package com.example.traderview.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class RobinhoodBrokerIntegration(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : BrokerIntegration {
    
    override val brokerName = "Robinhood"
    override val isEnabled = true
    
    private val baseUrl = "https://robinhood.com/api"
    
    override suspend fun testConnection(credentials: BrokerCredentials): BrokerConnectionResult {
        return try {
            val headers = createHeaders(credentials)
            
            // Test authentication with user endpoint
            val response = restTemplate.exchange(
                "$baseUrl/user/",
                HttpMethod.GET,
                HttpEntity(null, headers),
                RobinhoodUserResponse::class.java
            )
            
            if (response.statusCode.is2xxSuccessful && response.body?.username != null) {
                // Fetch accounts
                val accountsResponse = restTemplate.exchange(
                    "$baseUrl/accounts/",
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                    RobinhoodAccountsResponse::class.java
                )
                
                val accounts = accountsResponse.body?.results?.map { 
                    it.accountNumber ?: it.url?.substringAfterLast("/")?.removeSuffix("/") ?: "unknown"
                } ?: emptyList()
                
                BrokerConnectionResult(
                    success = true,
                    message = "Successfully connected to Robinhood",
                    accountsFound = accounts
                )
            } else {
                BrokerConnectionResult(
                    success = false,
                    message = "Authentication failed",
                    error = "Invalid access token or session expired"
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
            val headers = createHeaders(credentials)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            // Robinhood API pagination
            var nextUrl: String? = "$baseUrl/orders/?cursor="
            val allOrders = mutableListOf<RobinhoodOrder>()
            
            while (nextUrl != null) {
                val response = restTemplate.exchange(
                    nextUrl,
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                    RobinhoodOrdersResponse::class.java
                )
                
                val ordersResponse = response.body
                if (ordersResponse?.results != null) {
                    // Filter orders by date range
                    val filteredOrders = ordersResponse.results.filter { order ->
                        val orderDate = parseRobinhoodDateTime(order.createdAt)
                        orderDate.isAfter(startDate.minusDays(1)) && orderDate.isBefore(endDate.plusDays(1))
                    }
                    allOrders.addAll(filteredOrders)
                    nextUrl = ordersResponse.next
                } else {
                    break
                }
                
                // Safety break to avoid infinite loops
                if (allOrders.size > 1000) break
            }
            
            val orders = allOrders.map { rhOrder ->
                BrokerOrderData(
                    brokerOrderId = rhOrder.id ?: "unknown",
                    symbol = extractSymbolFromInstrument(rhOrder.instrumentUrl),
                    side = if (rhOrder.side == "buy") "BUY" else "SELL",
                    orderType = mapRobinhoodOrderType(rhOrder.type),
                    status = mapRobinhoodOrderStatus(rhOrder.state),
                    quantity = rhOrder.quantity?.toDouble() ?: 0.0,
                    filledQuantity = rhOrder.quantityFilled?.toDouble(),
                    price = rhOrder.price?.toDouble(),
                    avgFillPrice = rhOrder.averageFillPrice?.toDouble(),
                    commission = 0.0, // Robinhood is commission-free
                    fees = rhOrder.fees?.toDouble() ?: 0.0,
                    placedAt = parseRobinhoodDateTime(rhOrder.createdAt),
                    filledAt = if (rhOrder.state == "filled") parseRobinhoodDateTime(rhOrder.updatedAt) else null,
                    assetType = "STOCK", // Robinhood primarily handles stocks
                    exchange = "NASDAQ", // Default exchange
                    currency = "USD",
                    metadata = mapOf(
                        "instrumentUrl" to (rhOrder.instrumentUrl ?: ""),
                        "timeInForce" to (rhOrder.timeInForce ?: ""),
                        "trigger" to (rhOrder.trigger ?: "")
                    )
                )
            }
            
            BrokerOrdersResult(
                success = true,
                orders = orders,
                totalCount = orders.size,
                message = "Successfully fetched ${orders.size} orders from Robinhood"
            )
            
        } catch (e: Exception) {
            BrokerOrdersResult(
                success = false,
                error = e.message ?: "Failed to fetch orders from Robinhood"
            )
        }
    }
    
    override suspend fun fetchAccountInfo(credentials: BrokerCredentials, accountId: String): BrokerAccountResult {
        return try {
            val headers = createHeaders(credentials)
            
            val response = restTemplate.exchange(
                "$baseUrl/accounts/$accountId/",
                HttpMethod.GET,
                HttpEntity(null, headers),
                RobinhoodAccount::class.java
            )
            
            val account = response.body
            if (account != null) {
                // Fetch portfolio for balance information
                val portfolioResponse = restTemplate.exchange(
                    account.portfolioUrl ?: "$baseUrl/accounts/$accountId/portfolio/",
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                    RobinhoodPortfolio::class.java
                )
                
                val portfolio = portfolioResponse.body
                
                BrokerAccountResult(
                    success = true,
                    accountInfo = BrokerAccountInfo(
                        accountNumber = account.accountNumber ?: accountId,
                        accountName = "Robinhood Account",
                        accountType = account.type ?: "INDIVIDUAL",
                        balance = portfolio?.totalEquity?.toDouble() ?: 0.0,
                        availableBalance = portfolio?.withdrawableAmount?.toDouble() ?: 0.0,
                        currency = "USD",
                        isActive = account.deactivated != true
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
                error = e.message ?: "Failed to fetch account info from Robinhood"
            )
        }
    }
    
    override fun getSupportedOrderTypes(): List<String> {
        return listOf("MARKET", "LIMIT", "STOP_LOSS", "STOP_LIMIT")
    }
    
    override fun getSupportedAssetTypes(): List<String> {
        return listOf("STOCK", "ETF", "OPTION", "CRYPTO")
    }
    
    private fun createHeaders(credentials: BrokerCredentials): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer ${credentials.accessToken}")
        headers.set("User-Agent", "TraderView/1.0")
        return headers
    }
    
    private fun mapRobinhoodOrderType(type: String?): String {
        return when (type?.lowercase()) {
            "market" -> "MARKET"
            "limit" -> "LIMIT"
            "stop_loss" -> "STOP"
            "stop_limit" -> "STOP_LIMIT"
            else -> "MARKET"
        }
    }
    
    private fun mapRobinhoodOrderStatus(state: String?): String {
        return when (state?.lowercase()) {
            "filled" -> "FILLED"
            "cancelled" -> "CANCELLED"
            "queued", "unconfirmed", "confirmed" -> "PENDING"
            "partially_filled" -> "PARTIALLY_FILLED"
            "rejected", "failed" -> "REJECTED"
            else -> "PENDING"
        }
    }
    
    private fun parseRobinhoodDateTime(dateTimeStr: String?): LocalDateTime {
        return try {
            if (dateTimeStr.isNullOrBlank()) {
                LocalDateTime.now()
            } else {
                // Robinhood uses ISO format: 2021-01-01T10:00:00.000000Z
                LocalDateTime.parse(dateTimeStr.substring(0, 19))
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
    
    private fun extractSymbolFromInstrument(instrumentUrl: String?): String {
        // This would require an additional API call to get instrument details
        // For now, return a placeholder - in real implementation, cache instrument data
        return instrumentUrl?.substringAfterLast("/")?.removeSuffix("/") ?: "UNKNOWN"
    }
}

// Robinhood API Response DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class RobinhoodUserResponse(
    @JsonProperty("username") val username: String? = null,
    @JsonProperty("email") val email: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RobinhoodAccountsResponse(
    @JsonProperty("results") val results: List<RobinhoodAccount> = emptyList(),
    @JsonProperty("next") val next: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RobinhoodAccount(
    @JsonProperty("account_number") val accountNumber: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("portfolio") val portfolioUrl: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("deactivated") val deactivated: Boolean? = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RobinhoodOrdersResponse(
    @JsonProperty("results") val results: List<RobinhoodOrder> = emptyList(),
    @JsonProperty("next") val next: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RobinhoodOrder(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("instrument") val instrumentUrl: String? = null,
    @JsonProperty("side") val side: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("state") val state: String? = null,
    @JsonProperty("quantity") val quantity: String? = null,
    @JsonProperty("filled_quantity") val quantityFilled: String? = null,
    @JsonProperty("price") val price: String? = null,
    @JsonProperty("average_fill_price") val averageFillPrice: String? = null,
    @JsonProperty("fees") val fees: String? = null,
    @JsonProperty("created_at") val createdAt: String? = null,
    @JsonProperty("updated_at") val updatedAt: String? = null,
    @JsonProperty("time_in_force") val timeInForce: String? = null,
    @JsonProperty("trigger") val trigger: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RobinhoodPortfolio(
    @JsonProperty("total_equity") val totalEquity: String? = null,
    @JsonProperty("withdrawable_amount") val withdrawableAmount: String? = null
)

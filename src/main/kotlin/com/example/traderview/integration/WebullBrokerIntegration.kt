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
class WebullBrokerIntegration(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : BrokerIntegration {
    
    override val brokerName = "Webull"
    override val isEnabled = true
    
    private val baseUrl = "https://userapi.webull.com/api"
    private val tradingUrl = "https://tradeapi.webullfintech.com/api"
    
    override suspend fun testConnection(credentials: BrokerCredentials): BrokerConnectionResult {
        return try {
            val headers = createHeaders(credentials)
            
            // Test authentication with account info endpoint
            val response = restTemplate.exchange(
                "$baseUrl/user",
                HttpMethod.GET,
                HttpEntity(null, headers),
                WebullUserResponse::class.java
            )
            
            if (response.statusCode.is2xxSuccessful && response.body?.success == true) {
                // Fetch trading accounts
                val accountsResponse = restTemplate.exchange(
                    "$tradingUrl/account/getSecAccountList/v4",
                    HttpMethod.GET,
                    HttpEntity(null, headers),
                    WebullAccountsResponse::class.java
                )
                
                val accounts = accountsResponse.body?.data?.map { it.secAccountId.toString() } ?: emptyList()
                
                BrokerConnectionResult(
                    success = true,
                    message = "Successfully connected to Webull",
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
            
            // Webull uses timestamp in milliseconds
            val startTimestamp = startDate.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            val endTimestamp = endDate.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            
            val url = "$tradingUrl/trading/$accountId/stockOrders" +
                    "?startTime=$startTimestamp&endTime=$endTimestamp&pageSize=100"
            
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, headers),
                WebullOrdersResponse::class.java
            )
            
            val orders = response.body?.data?.map { webullOrder ->
                BrokerOrderData(
                    brokerOrderId = webullOrder.orderId.toString(),
                    symbol = webullOrder.ticker ?: "UNKNOWN",
                    side = if (webullOrder.action == "BUY") "BUY" else "SELL",
                    orderType = mapWebullOrderType(webullOrder.orderType),
                    status = mapWebullOrderStatus(webullOrder.status),
                    quantity = webullOrder.totalQuantity?.toDouble() ?: 0.0,
                    filledQuantity = webullOrder.filledQuantity?.toDouble(),
                    price = webullOrder.lmtPrice?.toDouble(),
                    avgFillPrice = webullOrder.avgFilledPrice?.toDouble(),
                    commission = webullOrder.fees?.toDouble() ?: 0.0,
                    fees = 0.0, // Webull is commission-free for stocks
                    placedAt = parseWebullDateTime(webullOrder.createTime),
                    filledAt = if (webullOrder.status == "Filled") parseWebullDateTime(webullOrder.updateTime) else null,
                    assetType = "STOCK", // Webull primarily handles stocks
                    exchange = webullOrder.exchange ?: "NASDAQ",
                    currency = "USD",
                    metadata = mapOf(
                        "tickerId" to (webullOrder.tickerId ?: 0),
                        "comboType" to (webullOrder.comboType ?: ""),
                        "timeInForce" to (webullOrder.timeInForce ?: "")
                    )
                )
            } ?: emptyList()
            
            BrokerOrdersResult(
                success = true,
                orders = orders,
                totalCount = orders.size,
                message = "Successfully fetched ${orders.size} orders from Webull"
            )
            
        } catch (e: Exception) {
            BrokerOrdersResult(
                success = false,
                error = e.message ?: "Failed to fetch orders from Webull"
            )
        }
    }
    
    override suspend fun fetchAccountInfo(credentials: BrokerCredentials, accountId: String): BrokerAccountResult {
        return try {
            val headers = createHeaders(credentials)
            
            val response = restTemplate.exchange(
                "$tradingUrl/account/$accountId/detail",
                HttpMethod.GET,
                HttpEntity(null, headers),
                WebullAccountDetailResponse::class.java
            )
            
            val accountDetail = response.body?.data
            if (accountDetail != null) {
                BrokerAccountResult(
                    success = true,
                    accountInfo = BrokerAccountInfo(
                        accountNumber = accountId,
                        accountName = "Webull Account",
                        accountType = accountDetail.accountType ?: "INDIVIDUAL",
                        balance = accountDetail.netLiquidation?.toDouble() ?: 0.0,
                        availableBalance = accountDetail.dayBuyingPower?.toDouble() ?: 0.0,
                        currency = "USD",
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
                error = e.message ?: "Failed to fetch account info from Webull"
            )
        }
    }
    
    override fun getSupportedOrderTypes(): List<String> {
        return listOf("MARKET", "LIMIT", "STOP", "STOP_LIMIT")
    }
    
    override fun getSupportedAssetTypes(): List<String> {
        return listOf("STOCK", "ETF", "OPTION", "CRYPTO")
    }
    
    private fun createHeaders(credentials: BrokerCredentials): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer ${credentials.accessToken}")
        headers.set("User-Agent", "TraderView/1.0")
        // Webull requires specific headers
        headers.set("app", "global")
        headers.set("appid", "wb_web_app")
        headers.set("ver", "3.39.15")
        return headers
    }
    
    private fun mapWebullOrderType(orderType: String?): String {
        return when (orderType?.uppercase()) {
            "MKT" -> "MARKET"
            "LMT" -> "LIMIT"
            "STP" -> "STOP"
            "STP_LMT" -> "STOP_LIMIT"
            else -> "MARKET"
        }
    }
    
    private fun mapWebullOrderStatus(status: String?): String {
        return when (status?.uppercase()) {
            "FILLED" -> "FILLED"
            "CANCELLED" -> "CANCELLED"
            "WORKING", "PENDING", "SUBMITTED" -> "PENDING"
            "PARTIAL" -> "PARTIALLY_FILLED"
            "REJECTED" -> "REJECTED"
            else -> "PENDING"
        }
    }
    
    private fun parseWebullDateTime(timestamp: Long?): LocalDateTime {
        return try {
            if (timestamp != null && timestamp > 0) {
                LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
            } else {
                LocalDateTime.now()
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

// Webull API Response DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullUserResponse(
    @JsonProperty("success") val success: Boolean? = false,
    @JsonProperty("data") val data: WebullUser? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullUser(
    @JsonProperty("uuid") val uuid: String? = null,
    @JsonProperty("username") val username: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullAccountsResponse(
    @JsonProperty("success") val success: Boolean? = false,
    @JsonProperty("data") val data: List<WebullAccount> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullAccount(
    @JsonProperty("secAccountId") val secAccountId: Long? = null,
    @JsonProperty("accountType") val accountType: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullOrdersResponse(
    @JsonProperty("success") val success: Boolean? = false,
    @JsonProperty("data") val data: List<WebullOrder> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullOrder(
    @JsonProperty("orderId") val orderId: Long? = null,
    @JsonProperty("tickerId") val tickerId: Long? = null,
    @JsonProperty("ticker") val ticker: String? = null,
    @JsonProperty("action") val action: String? = null,
    @JsonProperty("orderType") val orderType: String? = null,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("totalQuantity") val totalQuantity: String? = null,
    @JsonProperty("filledQuantity") val filledQuantity: String? = null,
    @JsonProperty("lmtPrice") val lmtPrice: String? = null,
    @JsonProperty("avgFilledPrice") val avgFilledPrice: String? = null,
    @JsonProperty("fees") val fees: String? = null,
    @JsonProperty("createTime") val createTime: Long? = null,
    @JsonProperty("updateTime") val updateTime: Long? = null,
    @JsonProperty("exchange") val exchange: String? = null,
    @JsonProperty("comboType") val comboType: String? = null,
    @JsonProperty("timeInForce") val timeInForce: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullAccountDetailResponse(
    @JsonProperty("success") val success: Boolean? = false,
    @JsonProperty("data") val data: WebullAccountDetail? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebullAccountDetail(
    @JsonProperty("accountType") val accountType: String? = null,
    @JsonProperty("netLiquidation") val netLiquidation: String? = null,
    @JsonProperty("dayBuyingPower") val dayBuyingPower: String? = null
)

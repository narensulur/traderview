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
class TDAmeritradeBrokerIntegration(
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : BrokerIntegration {
    
    override val brokerName = "TD Ameritrade"
    override val isEnabled = true
    
    private val baseUrl = "https://api.tdameritrade.com/v1"
    private val sandboxUrl = "https://api.tdameritrade.com/v1" // TD uses same URL for sandbox
    
    override suspend fun testConnection(credentials: BrokerCredentials): BrokerConnectionResult {
        return try {
            val headers = createHeaders(credentials)
            
            val response = restTemplate.exchange(
                "$baseUrl/accounts",
                HttpMethod.GET,
                HttpEntity(null, headers),
                Array<TDAccount>::class.java
            )
            
            if (response.statusCode.is2xxSuccessful) {
                val accounts = response.body?.map { it.securitiesAccount.accountId } ?: emptyList()
                
                BrokerConnectionResult(
                    success = true,
                    message = "Successfully connected to TD Ameritrade",
                    accountsFound = accounts
                )
            } else {
                BrokerConnectionResult(
                    success = false,
                    message = "Authentication failed",
                    error = "Invalid access token or expired session"
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
            
            val url = "$baseUrl/accounts/$accountId/orders" +
                    "?fromEnteredTime=${startDate.format(formatter)}" +
                    "&toEnteredTime=${endDate.format(formatter)}"
            
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, headers),
                Array<TDOrder>::class.java
            )
            
            val orders = response.body?.map { tdOrder ->
                BrokerOrderData(
                    brokerOrderId = tdOrder.orderId.toString(),
                    symbol = tdOrder.orderLegCollection?.firstOrNull()?.instrument?.symbol ?: "UNKNOWN",
                    side = mapTDInstruction(tdOrder.orderLegCollection?.firstOrNull()?.instruction),
                    orderType = mapTDOrderType(tdOrder.orderType),
                    status = mapTDOrderStatus(tdOrder.status),
                    quantity = tdOrder.quantity?.toDouble() ?: 0.0,
                    filledQuantity = tdOrder.filledQuantity?.toDouble(),
                    price = tdOrder.price?.toDouble(),
                    avgFillPrice = tdOrder.orderActivityCollection?.firstOrNull()?.executionLegs?.firstOrNull()?.price?.toDouble(),
                    commission = tdOrder.orderActivityCollection?.firstOrNull()?.executionLegs?.firstOrNull()?.commission?.toDouble(),
                    fees = 0.0, // TD includes fees in commission
                    placedAt = parseDateTime(tdOrder.enteredTime),
                    filledAt = if (tdOrder.status == "FILLED") parseDateTime(tdOrder.closeTime) else null,
                    assetType = mapTDAssetType(tdOrder.orderLegCollection?.firstOrNull()?.instrument?.assetType),
                    exchange = "NYSE", // TD doesn't always provide exchange info
                    currency = "USD",
                    metadata = mapOf(
                        "accountId" to accountId,
                        "orderStrategyType" to (tdOrder.orderStrategyType ?: ""),
                        "duration" to (tdOrder.duration ?: "")
                    )
                )
            }?.toList() ?: emptyList()
            
            BrokerOrdersResult(
                success = true,
                orders = orders,
                totalCount = orders.size,
                message = "Successfully fetched ${orders.size} orders from TD Ameritrade"
            )
            
        } catch (e: Exception) {
            BrokerOrdersResult(
                success = false,
                error = e.message ?: "Failed to fetch orders from TD Ameritrade"
            )
        }
    }
    
    override suspend fun fetchAccountInfo(credentials: BrokerCredentials, accountId: String): BrokerAccountResult {
        return try {
            val headers = createHeaders(credentials)
            
            val response = restTemplate.exchange(
                "$baseUrl/accounts/$accountId",
                HttpMethod.GET,
                HttpEntity(null, headers),
                TDAccount::class.java
            )
            
            val account = response.body?.securitiesAccount
            if (account != null) {
                BrokerAccountResult(
                    success = true,
                    accountInfo = BrokerAccountInfo(
                        accountNumber = account.accountId,
                        accountName = account.type ?: "TD Ameritrade Account",
                        accountType = account.type ?: "INDIVIDUAL",
                        balance = account.currentBalances?.liquidationValue?.toDouble() ?: 0.0,
                        availableBalance = account.currentBalances?.availableFunds?.toDouble() ?: 0.0,
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
                error = e.message ?: "Failed to fetch account info from TD Ameritrade"
            )
        }
    }
    
    override fun getSupportedOrderTypes(): List<String> {
        return listOf("MARKET", "LIMIT", "STOP", "STOP_LIMIT", "TRAILING_STOP")
    }
    
    override fun getSupportedAssetTypes(): List<String> {
        return listOf("EQUITY", "OPTION", "ETF", "MUTUAL_FUND", "FIXED_INCOME")
    }
    
    private fun createHeaders(credentials: BrokerCredentials): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(credentials.accessToken ?: "")
        return headers
    }
    
    private fun mapTDInstruction(instruction: String?): String {
        return when (instruction?.uppercase()) {
            "BUY", "BUY_TO_OPEN", "BUY_TO_COVER" -> "BUY"
            "SELL", "SELL_TO_CLOSE", "SELL_SHORT" -> "SELL"
            else -> "BUY"
        }
    }
    
    private fun mapTDOrderType(orderType: String?): String {
        return when (orderType?.uppercase()) {
            "MARKET" -> "MARKET"
            "LIMIT" -> "LIMIT"
            "STOP" -> "STOP"
            "STOP_LIMIT" -> "STOP_LIMIT"
            "TRAILING_STOP" -> "TRAIL"
            else -> "MARKET"
        }
    }
    
    private fun mapTDOrderStatus(status: String?): String {
        return when (status?.uppercase()) {
            "FILLED" -> "FILLED"
            "CANCELED" -> "CANCELLED"
            "PENDING_ACTIVATION", "QUEUED", "WORKING" -> "PENDING"
            "PARTIALLY_FILLED" -> "PARTIALLY_FILLED"
            "REJECTED" -> "REJECTED"
            else -> "PENDING"
        }
    }
    
    private fun mapTDAssetType(assetType: String?): String {
        return when (assetType?.uppercase()) {
            "EQUITY" -> "STOCK"
            "OPTION" -> "OPTION"
            "ETF" -> "ETF"
            "MUTUAL_FUND" -> "ETF"
            "FIXED_INCOME" -> "BOND"
            else -> "STOCK"
        }
    }
    
    private fun parseDateTime(dateTimeStr: String?): LocalDateTime {
        return try {
            if (dateTimeStr.isNullOrBlank()) {
                LocalDateTime.now()
            } else {
                // TD Ameritrade uses ISO format: 2021-01-01T10:00:00+0000
                LocalDateTime.parse(dateTimeStr.substring(0, 19))
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

// TD Ameritrade API Response DTOs
@JsonIgnoreProperties(ignoreUnknown = true)
data class TDAccount(
    @JsonProperty("securitiesAccount") val securitiesAccount: TDSecuritiesAccount
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDSecuritiesAccount(
    @JsonProperty("accountId") val accountId: String,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("currentBalances") val currentBalances: TDBalances? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDBalances(
    @JsonProperty("liquidationValue") val liquidationValue: String? = null,
    @JsonProperty("availableFunds") val availableFunds: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDOrder(
    @JsonProperty("orderId") val orderId: Long? = null,
    @JsonProperty("orderType") val orderType: String? = null,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("enteredTime") val enteredTime: String? = null,
    @JsonProperty("closeTime") val closeTime: String? = null,
    @JsonProperty("quantity") val quantity: String? = null,
    @JsonProperty("filledQuantity") val filledQuantity: String? = null,
    @JsonProperty("price") val price: String? = null,
    @JsonProperty("orderStrategyType") val orderStrategyType: String? = null,
    @JsonProperty("duration") val duration: String? = null,
    @JsonProperty("orderLegCollection") val orderLegCollection: List<TDOrderLeg>? = null,
    @JsonProperty("orderActivityCollection") val orderActivityCollection: List<TDOrderActivity>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDOrderLeg(
    @JsonProperty("instruction") val instruction: String? = null,
    @JsonProperty("quantity") val quantity: String? = null,
    @JsonProperty("instrument") val instrument: TDInstrument? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDInstrument(
    @JsonProperty("symbol") val symbol: String? = null,
    @JsonProperty("assetType") val assetType: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDOrderActivity(
    @JsonProperty("executionLegs") val executionLegs: List<TDExecutionLeg>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TDExecutionLeg(
    @JsonProperty("price") val price: String? = null,
    @JsonProperty("commission") val commission: String? = null
)

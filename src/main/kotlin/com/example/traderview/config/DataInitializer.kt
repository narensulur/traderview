package com.example.traderview.config

import com.example.traderview.entity.*
import com.example.traderview.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class DataInitializer(
    private val brokerRepository: BrokerRepository,
    private val tradingAccountRepository: TradingAccountRepository,
    private val symbolRepository: SymbolRepository,
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository
) : CommandLineRunner {
    
    override fun run(vararg args: String?) {
        if (brokerRepository.count() == 0L) {
            initializeSampleData()
        }
    }
    
    private fun initializeSampleData() {
        // Create sample brokers
        val interactiveBrokers = brokerRepository.save(
            Broker(
                name = "interactive_brokers",
                displayName = "Interactive Brokers",
                apiEndpoint = "https://api.interactivebrokers.com",
                isActive = true
            )
        )
        
        val tdAmeritrade = brokerRepository.save(
            Broker(
                name = "td_ameritrade",
                displayName = "TD Ameritrade",
                apiEndpoint = "https://api.tdameritrade.com",
                isActive = true
            )
        )
        
        // Create sample symbols
        val appleStock = symbolRepository.save(
            Symbol(
                ticker = "AAPL",
                name = "Apple Inc.",
                exchange = "NASDAQ",
                assetType = AssetType.STOCK,
                currency = "USD"
            )
        )
        
        val teslaStock = symbolRepository.save(
            Symbol(
                ticker = "TSLA",
                name = "Tesla, Inc.",
                exchange = "NASDAQ",
                assetType = AssetType.STOCK,
                currency = "USD"
            )
        )
        
        val spyEtf = symbolRepository.save(
            Symbol(
                ticker = "SPY",
                name = "SPDR S&P 500 ETF Trust",
                exchange = "NYSE",
                assetType = AssetType.ETF,
                currency = "USD"
            )
        )
        
        // Create sample trading accounts
        val account1 = tradingAccountRepository.save(
            TradingAccount(
                accountNumber = "IB123456789",
                accountName = "Main Trading Account",
                broker = interactiveBrokers,
                initialBalance = BigDecimal("100000.00"),
                currentBalance = BigDecimal("105000.00"),
                currency = "USD"
            )
        )
        
        val account2 = tradingAccountRepository.save(
            TradingAccount(
                accountNumber = "TD987654321",
                accountName = "Secondary Account",
                broker = tdAmeritrade,
                initialBalance = BigDecimal("50000.00"),
                currentBalance = BigDecimal("52500.00"),
                currency = "USD"
            )
        )
        
        // Create sample orders
        val buyOrder1 = orderRepository.save(
            Order(
                brokerOrderId = "IB_ORDER_001",
                tradingAccount = account1,
                symbol = appleStock,
                side = OrderSide.BUY,
                type = OrderType.MARKET,
                status = OrderStatus.FILLED,
                quantity = BigDecimal("100"),
                filledQuantity = BigDecimal("100"),
                price = BigDecimal("150.00"),
                avgFillPrice = BigDecimal("150.25"),
                commission = BigDecimal("1.00"),
                fees = BigDecimal("0.50"),
                placedAt = LocalDateTime.now().minusDays(5),
                filledAt = LocalDateTime.now().minusDays(5)
            )
        )
        
        val sellOrder1 = orderRepository.save(
            Order(
                brokerOrderId = "IB_ORDER_002",
                tradingAccount = account1,
                symbol = appleStock,
                side = OrderSide.SELL,
                type = OrderType.LIMIT,
                status = OrderStatus.FILLED,
                quantity = BigDecimal("100"),
                filledQuantity = BigDecimal("100"),
                price = BigDecimal("155.00"),
                avgFillPrice = BigDecimal("154.75"),
                commission = BigDecimal("1.00"),
                fees = BigDecimal("0.50"),
                placedAt = LocalDateTime.now().minusDays(2),
                filledAt = LocalDateTime.now().minusDays(2)
            )
        )
        
        // Create sample trade
        tradeRepository.save(
            Trade(
                tradingAccount = account1,
                symbol = appleStock,
                entryOrder = buyOrder1,
                exitOrder = sellOrder1,
                quantity = BigDecimal("100"),
                entryPrice = BigDecimal("150.25"),
                exitPrice = BigDecimal("154.75"),
                entryValue = BigDecimal("15025.00"),
                exitValue = BigDecimal("15475.00"),
                realizedPnl = BigDecimal("448.00"), // 450 - 2 (commission + fees)
                totalCommission = BigDecimal("2.00"),
                totalFees = BigDecimal("1.00"),
                entryTime = buyOrder1.filledAt!!,
                exitTime = sellOrder1.filledAt!!,
                status = TradeStatus.CLOSED
            )
        )
        
        println("Sample data initialized successfully!")
    }
}

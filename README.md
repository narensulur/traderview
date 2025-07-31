# TraderView - Trading Journal Backend

A comprehensive Kotlin Spring Boot backend application for managing trading journals, connecting to different brokers, and providing analytics on trading performance.

## Features

- **Broker Management**: Support for multiple brokers with API configurations
- **Trading Accounts**: Manage multiple trading accounts per broker
- **Order Tracking**: Track all executed orders with detailed information
- **Trade Analytics**: Generate P/L statements, performance metrics, and trends
- **Dashboard**: Comprehensive dashboard with trading statistics
- **Symbol Management**: Support for stocks, ETFs, options, futures, forex, and crypto
- **REST API**: Full RESTful API with OpenAPI/Swagger documentation

## Technology Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3.5.4
- **Database**: H2 (development), PostgreSQL (production ready)
- **ORM**: Hibernate/JPA
- **API Documentation**: OpenAPI 3 with Swagger UI
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Mockito

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 7.0 or higher

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application:

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### React Plugin Setup

To run the standalone React plugin that integrates with this backend:

**Install Dependencies:**
```bash
# Backend
cd plugin-server && npm install

# Frontend
cd plugin-frontend && npm install
```

**Start Services:**
```bash
# Start your TraderView backend first
./gradlew bootRun

# Start plugin backend
cd plugin-server && npm run dev

# Start plugin frontend
cd plugin-frontend && npm start
```

**Access Points:**
- TraderView Backend: http://localhost:8080
- Plugin Frontend: http://localhost:3000
- Plugin Backend: http://localhost:3001

### API Documentation

Once the application is running, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **H2 Console**: http://localhost:8080/h2-console (username: `sa`, password: `password`)

## API Endpoints

### Brokers
- `GET /api/brokers` - Get all brokers
- `GET /api/brokers/active` - Get active brokers
- `GET /api/brokers/{id}` - Get broker by ID
- `POST /api/brokers` - Create new broker
- `PUT /api/brokers/{id}` - Update broker
- `DELETE /api/brokers/{id}` - Delete broker

### Trading Accounts
- `GET /api/trading-accounts` - Get all trading accounts
- `GET /api/trading-accounts/active` - Get active accounts
- `GET /api/trading-accounts/{id}` - Get account by ID
- `GET /api/trading-accounts/broker/{brokerId}` - Get accounts by broker
- `POST /api/trading-accounts` - Create new account
- `PUT /api/trading-accounts/{id}` - Update account
- `DELETE /api/trading-accounts/{id}` - Delete account

### Symbols
- `GET /api/symbols` - Get all symbols
- `GET /api/symbols/active` - Get active symbols
- `GET /api/symbols/{id}` - Get symbol by ID
- `GET /api/symbols/ticker/{ticker}` - Get symbol by ticker
- `GET /api/symbols/asset-type/{assetType}` - Get symbols by asset type
- `GET /api/symbols/search?ticker={ticker}` - Search symbols by ticker
- `POST /api/symbols` - Create new symbol
- `DELETE /api/symbols/{id}` - Delete symbol

### Orders
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/trading-account/{accountId}` - Get orders by account
- `GET /api/orders/symbol/{symbolId}` - Get orders by symbol
- `GET /api/orders/status/{status}` - Get orders by status
- `GET /api/orders/date-range` - Get orders by date range
- `POST /api/orders` - Create new order
- `DELETE /api/orders/{id}` - Delete order

### Analytics
- `GET /api/analytics/dashboard/{accountId}` - Get dashboard summary
- `GET /api/analytics/symbol-performance/{accountId}` - Get symbol performance
- `GET /api/analytics/daily-pnl/{accountId}` - Get daily P&L analysis

## Sample Data

The application comes with sample data including:
- 2 brokers (Interactive Brokers, TD Ameritrade)
- 2 trading accounts
- 3 symbols (AAPL, TSLA, SPY)
- Sample orders and trades with P&L calculations

## Database Schema

### Core Entities
- **Broker**: Broker configurations and API endpoints
- **TradingAccount**: User trading accounts linked to brokers
- **Symbol**: Trading instruments (stocks, ETFs, options, etc.)
- **Order**: Individual trade orders with execution details
- **Trade**: Aggregated trade information with P&L calculations

## Configuration

### Database Configuration
The application uses H2 in-memory database by default. To use PostgreSQL:

1. Uncomment PostgreSQL dependency in `build.gradle.kts`
2. Update `application.properties` with PostgreSQL configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/traderview
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

## Testing

Run tests with:

```bash
./gradlew test
```

## Building for Production

Build the application:

```bash
./gradlew build
```

The JAR file will be created in `build/libs/`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

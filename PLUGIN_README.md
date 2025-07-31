# TraderView

A standalone React application with Node.js backend that integrates with the TraderView trading journal backend. This application provides an independent dashboard for trading analytics, order management, and account monitoring.

## Architecture

- **Backend**: Node.js + Express server (Port 3001)
- **Frontend**: React + Bootstrap (Port 3000)
- **Integration**: Connects to TraderView backend API (Port 8080)

## Features

- 📊 **Dashboard**: Real-time trading performance overview
- 📈 **Analytics**: Interactive charts and performance metrics
- 📋 **Orders Management**: View and filter trading orders
- 💰 **Account Management**: Manage multiple trading accounts
- 🔍 **Symbol Browser**: Search and filter trading symbols
- 🎨 **Dark Theme**: Professional trading interface

## Quick Start

### 1. Install Dependencies

**Backend:**
```bash
cd plugin-server
npm install
```

**Frontend:**
```bash
cd plugin-frontend
npm install
```

### 2. Start the Services

**Start TraderView Backend (if not already running):**
```bash
# In your TraderView backend directory
./gradlew bootRun
```

**Start TraderView Backend:**
```bash
cd plugin-server
npm run dev
```

**Start TraderView Frontend:**
```bash
cd plugin-frontend
npm start
```

### 3. Access the Application

- TraderView Frontend: http://localhost:3000
- TraderView Backend: http://localhost:3001
- TraderView API: http://localhost:8080/swagger-ui.html

## Configuration

### Environment Variables

Edit `plugin-server/.env`:

```env
# Server Configuration
PORT=3001
NODE_ENV=development

# TraderView Backend Configuration
TRADERVIEW_API_BASE_URL=http://localhost:8080/api

# CORS Configuration
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
```

## Components

### Dashboard
- Real-time P&L summary
- Trading statistics
- Performance metrics
- Account overview

### Analytics
- Daily P&L charts
- Symbol performance analysis
- Win/loss ratio visualization
- Interactive date range filtering

### Orders List
- Comprehensive order history
- Status-based filtering
- Real-time order updates

### Trading Accounts
- Multi-account management
- Account creation and deletion
- Balance tracking

### Symbols Browser
- Symbol search and filtering
- Asset type categorization
- Exchange-based grouping

## Development

### Project Structure

```
plugin-server/
├── server.js          # Express server with API proxy
├── package.json       # Backend dependencies
└── .env              # Environment configuration

plugin-frontend/
├── src/
│   ├── components/    # React components
│   ├── services/      # API service layer
│   ├── App.js        # Main application
│   └── index.js      # React entry point
├── public/           # Static assets
└── package.json      # Frontend dependencies
```

## Troubleshooting

### Common Issues

1. **CORS Errors**: Ensure TraderView backend allows requests from plugin origin
2. **API Connection**: Verify TraderView backend is running on port 8080
3. **Port Conflicts**: Change ports in configuration if needed

### Debug Mode

Enable debug logging:
```bash
NODE_ENV=development npm run dev
```

import axios from 'axios';
import { mockApi } from './mockApi';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.status} ${response.config.url}`);
    return response;
  },
  (error) => {
    console.error('API Response Error:', error.response?.status, error.message);
    return Promise.reject(error);
  }
);

// Analytics API
export const analyticsApi = {
  getDashboardSummary: (accountId, startDate, endDate) => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getDashboardData(accountId);
    }
    return api.get(`/analytics/dashboard/${accountId}`, {
      params: { startDate, endDate }
    });
  },
  
  getSymbolPerformance: (accountId, startDate, endDate) => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getSymbolPerformance(accountId, startDate, endDate);
    }
    return api.get(`/analytics/symbol-performance/${accountId}`, {
      params: { startDate, endDate }
    });
  },

  getDailyPnl: (accountId, startDate, endDate) => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getDailyPnl(accountId, startDate, endDate);
    }
    return api.get(`/analytics/daily-pnl/${accountId}`, {
      params: { startDate, endDate }
    });
  },
};

// Advanced Analytics API
export const advancedAnalyticsApi = {
  getTradeStats: (accountId, filters = {}) => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getTradeStats(accountId, filters);
    }
    return api.get(`/advanced-analytics/stats/${accountId}`, {
      params: filters
    });
  },
  
  getIntradayAnalysis: (accountId, date) =>
    api.get(`/advanced-analytics/intraday/${accountId}`, {
      params: { date }
    }),
  
  getFilteredStats: (filterRequest) =>
    api.post('/advanced-analytics/filtered-stats', filterRequest),
};

// Trading Accounts API
export const tradingAccountsApi = {
  getAllAccounts: () => api.get('/trading-accounts'),
  getActiveAccounts: () => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getActiveAccounts();
    }
    return api.get('/trading-accounts/active');
  },
  getAccountById: (id) => api.get(`/trading-accounts/${id}`),
  getAccountsByBroker: (brokerId) => api.get(`/trading-accounts/broker/${brokerId}`),
  createAccount: (account) => api.post('/trading-accounts', account),
  updateAccount: (id, account) => api.put(`/trading-accounts/${id}`, account),
  deleteAccount: (id) => api.delete(`/trading-accounts/${id}`),
};

// Orders API
export const ordersApi = {
  getAllOrders: () => api.get('/orders'),
  getOrderById: (id) => api.get(`/orders/${id}`),
  getOrdersByAccount: (accountId) => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getOrdersByAccount(accountId);
    }
    return api.get(`/orders/trading-account/${accountId}`);
  },
  getOrdersBySymbol: (symbolId) => api.get(`/orders/symbol/${symbolId}`),
  getOrdersByStatus: (status) => api.get(`/orders/status/${status}`),
  getOrdersByDateRange: (startDate, endDate) =>
    api.get('/orders/date-range', {
      params: { startDate, endDate }
    }),
  createOrder: (order) => api.post('/orders', order),
  deleteOrder: (id) => api.delete(`/orders/${id}`),
};

// Symbols API
export const symbolsApi = {
  getAllSymbols: () => api.get('/symbols'),
  getActiveSymbols: () => api.get('/symbols/active'),
  getSymbolById: (id) => api.get(`/symbols/${id}`),
  getSymbolByTicker: (ticker) => api.get(`/symbols/ticker/${ticker}`),
  getSymbolsByAssetType: (assetType) => api.get(`/symbols/asset-type/${assetType}`),
  searchSymbols: (ticker) => api.get('/symbols/search', { params: { ticker } }),
  getSymbolsByExchange: (exchange) => api.get(`/symbols/exchange/${exchange}`),
  getSymbolsWithWeeklyStats: (accountId) => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getSymbolsWithWeeklyStats(accountId);
    }
    return api.get(`/symbols/with-weekly-stats/${accountId}`);
  },
  createSymbol: (symbol) => api.post('/symbols', symbol),
  deleteSymbol: (id) => api.delete(`/symbols/${id}`),
};

// Brokers API
export const brokersApi = {
  getAllBrokers: () => api.get('/brokers'),
  getActiveBrokers: () => {
    if (mockApi.isUsingMockData()) {
      return mockApi.getActiveBrokers();
    }
    return api.get('/brokers/active');
  },
  getBrokerById: (id) => api.get(`/brokers/${id}`),
  createBroker: (broker) => api.post('/brokers', broker),
  updateBroker: (id, broker) => api.put(`/brokers/${id}`, broker),
  deleteBroker: (id) => api.delete(`/brokers/${id}`),
};

// Broker Integration API
export const brokerIntegrationApi = {
  getAvailableIntegrations: () => api.get('/broker-integration/available'),
  testConnection: (request) => api.post('/broker-integration/test-connection', request),
  syncOrders: (request) => api.post('/broker-integration/sync-orders', request),
  fetchAccountInfo: (request) => api.post('/broker-integration/account-info', request),
};

export default api;

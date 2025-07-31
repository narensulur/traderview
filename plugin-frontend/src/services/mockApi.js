// Mock API service for development mode with sample data
export const mockApi = {
  // Check if we're in development mode with sample data
  isUsingMockData: () => {
    return process.env.NODE_ENV === 'development' && 
           localStorage.getItem('traderview_sample_data') !== null;
  },

  // Get sample data from localStorage
  getSampleData: () => {
    const data = localStorage.getItem('traderview_sample_data');
    return data ? JSON.parse(data) : null;
  },

  // Mock trading accounts API
  getActiveAccounts: () => {
    const sampleData = mockApi.getSampleData();
    return Promise.resolve({
      data: sampleData?.accounts || []
    });
  },

  // Mock orders API
  getOrdersByAccount: (accountId) => {
    const sampleData = mockApi.getSampleData();
    return Promise.resolve({
      data: sampleData?.orders || []
    });
  },

  // Mock symbols API
  getSymbolsWithWeeklyStats: (accountId) => {
    const sampleData = mockApi.getSampleData();
    return Promise.resolve({
      data: sampleData?.symbols || []
    });
  },

  // Mock brokers API
  getActiveBrokers: () => {
    const sampleData = mockApi.getSampleData();
    return Promise.resolve({
      data: sampleData?.brokers || []
    });
  },

  // Mock daily PnL API
  getDailyPnl: (accountId, startDate, endDate) => {
    const sampleData = mockApi.getSampleData();
    if (!sampleData) return Promise.resolve({ data: [] });

    return Promise.resolve({
      data: sampleData.orders ? mockApi.generateDailyPnlFromOrders(sampleData.orders) : []
    });
  },

  // Mock symbol performance API
  getSymbolPerformance: (accountId, startDate, endDate) => {
    const sampleData = mockApi.getSampleData();
    if (!sampleData) return Promise.resolve({ data: [] });

    return Promise.resolve({
      data: sampleData.symbols ? sampleData.symbols.map(symbol => ({
        symbol: symbol.ticker,
        totalPnl: symbol.weeklyPnl,
        trades: symbol.tradesThisWeek,
        winRate: Math.random() * 100,
        avgTradeSize: symbol.totalVolumeThisWeek / symbol.tradesThisWeek || 0
      })) : []
    });
  },

  // Mock trade stats API
  getTradeStats: (accountId, params) => {
    const sampleData = mockApi.getSampleData();
    if (!sampleData) return Promise.resolve({ data: null });

    const orders = sampleData.orders || [];
    const totalTrades = orders.length;
    const totalPnl = orders.reduce((sum, order) => sum + (order.realizedPnl || 0), 0);
    const winningTrades = orders.filter(order => (order.realizedPnl || 0) > 0).length;

    return Promise.resolve({
      data: {
        totalTrades,
        totalPnl,
        winningTrades,
        losingTrades: totalTrades - winningTrades,
        winRate: totalTrades > 0 ? (winningTrades / totalTrades) * 100 : 0,
        avgTradeSize: totalTrades > 0 ? orders.reduce((sum, order) => sum + (order.quantity || 0), 0) / totalTrades : 0,
        largestWin: Math.max(...orders.map(order => order.realizedPnl || 0)),
        largestLoss: Math.min(...orders.map(order => order.realizedPnl || 0))
      }
    });
  },

  // Helper function to generate daily PnL from orders
  generateDailyPnlFromOrders: (orders) => {
    const dailyPnl = [];
    const dailyMap = {};

    // Group orders by date
    orders.forEach(order => {
      const date = new Date(order.placedAt).toISOString().split('T')[0];
      if (!dailyMap[date]) {
        dailyMap[date] = { pnl: 0, trades: 0 };
      }
      dailyMap[date].pnl += order.realizedPnl || 0;
      dailyMap[date].trades += 1;
    });

    // Generate last 30 days
    let cumulativePnl = 0;
    for (let i = 29; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dateStr = date.toISOString().split('T')[0];

      const dayData = dailyMap[dateStr] || { pnl: 0, trades: 0 };
      cumulativePnl += dayData.pnl;

      dailyPnl.push({
        date: dateStr,
        pnl: dayData.pnl,
        cumulativePnl: cumulativePnl,
        trades: dayData.trades
      });
    }

    return dailyPnl;
  },

  // Mock analytics API
  getAnalytics: (accountId) => {
    const sampleData = mockApi.getSampleData();
    const orders = sampleData?.orders || [];
    
    // Calculate analytics from sample orders
    const totalTrades = orders.length;
    const totalPnl = orders.reduce((sum, order) => sum + (order.realizedPnl || 0), 0);
    const winningTrades = orders.filter(order => (order.realizedPnl || 0) > 0).length;
    const losingTrades = orders.filter(order => (order.realizedPnl || 0) < 0).length;
    const winRate = totalTrades > 0 ? (winningTrades / totalTrades) * 100 : 0;
    
    // Group by symbol for top performers
    const symbolStats = {};
    orders.forEach(order => {
      if (!symbolStats[order.symbolTicker]) {
        symbolStats[order.symbolTicker] = {
          symbol: order.symbolTicker,
          trades: 0,
          pnl: 0,
          volume: 0
        };
      }
      symbolStats[order.symbolTicker].trades++;
      symbolStats[order.symbolTicker].pnl += order.realizedPnl || 0;
      symbolStats[order.symbolTicker].volume += order.quantity || 0;
    });

    const topPerformers = Object.values(symbolStats)
      .sort((a, b) => b.pnl - a.pnl)
      .slice(0, 5);

    // Generate daily PnL data for the last 30 days
    const dailyPnl = [];
    for (let i = 29; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dayOrders = orders.filter(order => {
        const orderDate = new Date(order.placedAt);
        return orderDate.toDateString() === date.toDateString();
      });
      const dayPnl = dayOrders.reduce((sum, order) => sum + (order.realizedPnl || 0), 0);
      
      dailyPnl.push({
        date: date.toISOString().split('T')[0],
        pnl: dayPnl,
        trades: dayOrders.length
      });
    }

    return Promise.resolve({
      data: {
        totalTrades,
        totalPnl,
        winningTrades,
        losingTrades,
        winRate,
        topPerformers,
        dailyPnl,
        avgTradeSize: totalTrades > 0 ? orders.reduce((sum, order) => sum + (order.quantity || 0), 0) / totalTrades : 0,
        largestWin: Math.max(...orders.map(order => order.realizedPnl || 0)),
        largestLoss: Math.min(...orders.map(order => order.realizedPnl || 0))
      }
    });
  },

  // Mock dashboard API
  getDashboardData: (accountId) => {
    const sampleData = mockApi.getSampleData();
    const orders = sampleData?.orders || [];
    const symbols = sampleData?.symbols || [];
    
    // Recent orders (last 10)
    const recentOrders = orders
      .sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt))
      .slice(0, 10);

    // Top symbols by trades this week
    const topSymbols = symbols
      .sort((a, b) => b.tradesThisWeek - a.tradesThisWeek)
      .slice(0, 5);

    // Calculate summary stats
    const todayOrders = orders.filter(order => {
      const orderDate = new Date(order.placedAt);
      const today = new Date();
      return orderDate.toDateString() === today.toDateString();
    });

    const weekOrders = orders.filter(order => {
      const orderDate = new Date(order.placedAt);
      const weekAgo = new Date();
      weekAgo.setDate(weekAgo.getDate() - 7);
      return orderDate >= weekAgo;
    });

    return Promise.resolve({
      data: {
        recentOrders,
        topSymbols,
        todayStats: {
          trades: todayOrders.length,
          pnl: todayOrders.reduce((sum, order) => sum + (order.realizedPnl || 0), 0),
          volume: todayOrders.reduce((sum, order) => sum + (order.quantity || 0), 0)
        },
        weekStats: {
          trades: weekOrders.length,
          pnl: weekOrders.reduce((sum, order) => sum + (order.realizedPnl || 0), 0),
          volume: weekOrders.reduce((sum, order) => sum + (order.quantity || 0), 0)
        }
      }
    });
  }
};

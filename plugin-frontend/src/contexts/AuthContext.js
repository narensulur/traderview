import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Session timeout: 2 hours (in milliseconds)
  const SESSION_TIMEOUT = 2 * 60 * 60 * 1000;

  useEffect(() => {
    // Check for existing session on app load
    checkExistingSession();
    
    // Load Google Identity Services
    loadGoogleScript();
  }, []);

  const checkExistingSession = () => {
    try {
      const storedUser = localStorage.getItem('traderview_user');
      const loginTime = localStorage.getItem('traderview_login_time');
      
      if (storedUser && loginTime) {
        const currentTime = new Date().getTime();
        const timeDiff = currentTime - parseInt(loginTime);
        
        if (timeDiff < SESSION_TIMEOUT) {
          const userData = JSON.parse(storedUser);
          setUser(userData);
          setIsAuthenticated(true);
          
          // Set up auto-logout timer for remaining time
          const remainingTime = SESSION_TIMEOUT - timeDiff;
          setTimeout(() => {
            logout();
          }, remainingTime);
        } else {
          // Session expired
          clearSession();
        }
      }
    } catch (error) {
      console.error('Error checking existing session:', error);
      clearSession();
    } finally {
      setLoading(false);
    }
  };

  const loadGoogleScript = () => {
    if (document.getElementById('google-identity-script')) {
      return;
    }

    const script = document.createElement('script');
    script.id = 'google-identity-script';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = initializeGoogleSignIn;
    document.head.appendChild(script);
  };

  const initializeGoogleSignIn = () => {
    if (window.google) {
      try {
        window.google.accounts.id.initialize({
          client_id: process.env.REACT_APP_GOOGLE_CLIENT_ID || 'your-google-client-id',
          callback: handleGoogleSignIn,
          auto_select: false,
          cancel_on_tap_outside: false,
        });
        console.log('Google Sign-In initialized with Client ID:', process.env.REACT_APP_GOOGLE_CLIENT_ID);
      } catch (error) {
        console.error('Failed to initialize Google Sign-In:', error);
      }
    }
  };

  const handleGoogleSignIn = (response) => {
    try {
      // Decode the JWT token to get user info
      const payload = JSON.parse(atob(response.credential.split('.')[1]));
      
      const userData = {
        id: payload.sub,
        email: payload.email,
        name: payload.name,
        picture: payload.picture,
        given_name: payload.given_name,
        family_name: payload.family_name,
      };

      const loginTime = new Date().getTime();
      
      // Store user data and login time
      localStorage.setItem('traderview_user', JSON.stringify(userData));
      localStorage.setItem('traderview_login_time', loginTime.toString());
      
      setUser(userData);
      setIsAuthenticated(true);
      
      // Set up auto-logout timer
      setTimeout(() => {
        logout();
      }, SESSION_TIMEOUT);
      
    } catch (error) {
      console.error('Error handling Google sign in:', error);
    }
  };

  const login = () => {
    if (window.google) {
      window.google.accounts.id.prompt();
    }
  };

  // Development bypass for testing (remove in production)
  const loginAsDeveloper = () => {
    const devUser = {
      id: 'dev-user-123',
      email: 'developer@traderview.com',
      name: 'Developer User',
      picture: 'https://via.placeholder.com/40x40/007bff/ffffff?text=DEV',
      given_name: 'Developer',
      family_name: 'User',
    };

    const loginTime = new Date().getTime();

    // Create sample data based on your CSV
    const sampleData = createSampleData();

    // Store user data and sample data
    localStorage.setItem('traderview_user', JSON.stringify(devUser));
    localStorage.setItem('traderview_login_time', loginTime.toString());
    localStorage.setItem('traderview_sample_data', JSON.stringify(sampleData));

    setUser(devUser);
    setIsAuthenticated(true);

    setTimeout(() => {
      logout();
    }, SESSION_TIMEOUT);
  };

  const createSampleData = () => {
    // Sample orders based on your CSV data
    const sampleOrders = [
      {
        id: 1,
        symbolTicker: 'AAPL',
        side: 'BUY',
        quantity: 100,
        price: 150.25,
        filledQuantity: 100,
        avgTradesPerDay: 2.5,
        realizedPnl: 250.75,
        placedAt: '2025-07-25T14:30:00.000Z',
        status: 'FILLED'
      },
      {
        id: 2,
        symbolTicker: 'AAPL',
        side: 'SELL',
        quantity: 50,
        price: 152.80,
        filledQuantity: 50,
        avgTradesPerDay: 1.8,
        realizedPnl: 127.50,
        placedAt: '2025-07-28T10:15:00.000Z',
        status: 'FILLED'
      },
      {
        id: 3,
        symbolTicker: 'MES',
        side: 'BUY',
        quantity: 2,
        price: 4520.25,
        filledQuantity: 2,
        avgTradesPerDay: 3.2,
        realizedPnl: -45.50,
        placedAt: '2025-07-02T09:45:00.000Z',
        status: 'FILLED'
      },
      {
        id: 4,
        symbolTicker: 'MES',
        side: 'SELL',
        quantity: 1,
        price: 4535.75,
        filledQuantity: 1,
        avgTradesPerDay: 2.1,
        realizedPnl: 15.50,
        placedAt: '2025-07-02T15:20:00.000Z',
        status: 'FILLED'
      },
      {
        id: 5,
        symbolTicker: 'TSLA',
        side: 'BUY',
        quantity: 25,
        price: 245.60,
        filledQuantity: 25,
        avgTradesPerDay: 1.5,
        realizedPnl: -32.25,
        placedAt: '2025-07-26T11:30:00.000Z',
        status: 'FILLED'
      },
      {
        id: 6,
        symbolTicker: 'NVDA',
        side: 'BUY',
        quantity: 10,
        price: 425.80,
        filledQuantity: 10,
        avgTradesPerDay: 4.2,
        realizedPnl: 85.40,
        placedAt: '2025-07-27T13:45:00.000Z',
        status: 'FILLED'
      }
    ];

    // Generate more sample orders to test pagination
    const additionalOrders = [];
    const symbols = ['AAPL', 'TSLA', 'NVDA', 'MSFT', 'GOOGL', 'AMZN', 'META', 'MES', 'NQ', 'ES'];
    const sides = ['BUY', 'SELL'];
    const statuses = ['FILLED', 'PARTIALLY_FILLED', 'PENDING'];

    for (let i = 7; i <= 150; i++) {
      const symbol = symbols[Math.floor(Math.random() * symbols.length)];
      const side = sides[Math.floor(Math.random() * sides.length)];
      const quantity = Math.floor(Math.random() * 100) + 1;
      const price = Math.random() * 500 + 50;
      const filledQty = Math.floor(Math.random() * quantity);
      const pnl = (Math.random() - 0.5) * 1000;
      const daysAgo = Math.floor(Math.random() * 30);
      const date = new Date();
      date.setDate(date.getDate() - daysAgo);

      additionalOrders.push({
        id: i,
        symbolTicker: symbol,
        side: side,
        quantity: quantity,
        price: price,
        filledQuantity: filledQty,
        avgTradesPerDay: Math.random() * 5,
        realizedPnl: pnl,
        placedAt: date.toISOString(),
        status: statuses[Math.floor(Math.random() * statuses.length)]
      });
    }

    const allOrders = [...sampleOrders, ...additionalOrders];

    // Sample symbols with weekly stats
    const sampleSymbols = [
      {
        id: 1,
        ticker: 'AAPL',
        name: 'Apple Inc.',
        tradesThisWeek: 15,
        avgPriceThisWeek: 151.25,
        totalVolumeThisWeek: 2500,
        weeklyPnl: 378.25
      },
      {
        id: 2,
        ticker: 'MES',
        name: 'Micro E-mini S&P 500',
        tradesThisWeek: 8,
        avgPriceThisWeek: 4528.50,
        totalVolumeThisWeek: 12,
        weeklyPnl: -30.00
      },
      {
        id: 3,
        ticker: 'TSLA',
        name: 'Tesla Inc.',
        tradesThisWeek: 5,
        avgPriceThisWeek: 248.75,
        totalVolumeThisWeek: 125,
        weeklyPnl: -32.25
      },
      {
        id: 4,
        ticker: 'NVDA',
        name: 'NVIDIA Corporation',
        tradesThisWeek: 12,
        avgPriceThisWeek: 428.90,
        totalVolumeThisWeek: 180,
        weeklyPnl: 245.80
      }
    ];

    // Generate more symbols for pagination testing
    const additionalSymbols = [];
    const symbolNames = [
      { ticker: 'MSFT', name: 'Microsoft Corporation' },
      { ticker: 'GOOGL', name: 'Alphabet Inc.' },
      { ticker: 'AMZN', name: 'Amazon.com Inc.' },
      { ticker: 'META', name: 'Meta Platforms Inc.' },
      { ticker: 'NQ', name: 'Micro E-mini NASDAQ-100' },
      { ticker: 'ES', name: 'E-mini S&P 500' },
      { ticker: 'AMD', name: 'Advanced Micro Devices' },
      { ticker: 'NFLX', name: 'Netflix Inc.' },
      { ticker: 'CRM', name: 'Salesforce Inc.' },
      { ticker: 'ADBE', name: 'Adobe Inc.' }
    ];

    symbolNames.forEach((symbol, index) => {
      additionalSymbols.push({
        id: index + 5,
        ticker: symbol.ticker,
        name: symbol.name,
        tradesThisWeek: Math.floor(Math.random() * 20) + 1,
        avgPriceThisWeek: Math.random() * 400 + 50,
        totalVolumeThisWeek: Math.floor(Math.random() * 1000) + 50,
        weeklyPnl: (Math.random() - 0.5) * 500
      });
    });

    const allSymbols = [...sampleSymbols, ...additionalSymbols];

    return {
      orders: allOrders,
      symbols: allSymbols,
      accounts: [
        {
          id: 1,
          accountName: 'Main Trading Account',
          accountNumber: 'DU123456',
          brokerName: 'Interactive Brokers',
          brokerId: 1,
          isActive: true
        },
        {
          id: 2,
          accountName: 'Futures Account',
          accountNumber: 'DU789012',
          brokerName: 'TD Ameritrade',
          brokerId: 2,
          isActive: true
        }
      ],
      brokers: [
        {
          id: 1,
          name: 'interactive_brokers',
          displayName: 'Interactive Brokers',
          apiEndpoint: 'https://api.interactivebrokers.com',
          isActive: true
        },
        {
          id: 2,
          name: 'td_ameritrade',
          displayName: 'TD Ameritrade',
          apiEndpoint: 'https://api.tdameritrade.com',
          isActive: true
        }
      ]
    };
  };

  const logout = () => {
    // Sign out from Google
    if (window.google) {
      window.google.accounts.id.disableAutoSelect();
    }
    
    clearSession();
  };

  const clearSession = () => {
    localStorage.removeItem('traderview_user');
    localStorage.removeItem('traderview_login_time');
    setUser(null);
    setIsAuthenticated(false);
  };

  const value = {
    user,
    isAuthenticated,
    loading,
    login,
    logout,
    loginAsDeveloper, // Development only
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

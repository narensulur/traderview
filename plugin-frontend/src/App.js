import React, { useState, useEffect } from 'react';
import { Routes, Route, Link, useLocation } from 'react-router-dom';
import { Navbar, Nav, Container, Alert } from 'react-bootstrap';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import UserProfile from './components/UserProfile';
import SessionTimeoutWarning from './components/SessionTimeoutWarning';
import Dashboard from './components/Dashboard';
import Analytics from './components/Analytics';
import TradingAccounts from './components/TradingAccounts';
import OrdersList from './components/OrdersList';
import SymbolsList from './components/SymbolsList';
import { tradingAccountsApi } from './services/api';

function App() {
  return (
    <AuthProvider>
      <ProtectedRoute>
        <AuthenticatedApp />
      </ProtectedRoute>
    </AuthProvider>
  );
}

function AuthenticatedApp() {
  const location = useLocation();
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    try {
      setLoading(true);
      const response = await tradingAccountsApi.getActiveAccounts();
      const accountsData = Array.isArray(response.data) ? response.data : [];
      setAccounts(accountsData);

      // Auto-select first account if available
      if (accountsData.length > 0 && !selectedAccount) {
        setSelectedAccount(accountsData[0]);
      }
      setError(null);
    } catch (err) {
      console.error('Failed to load accounts:', err);
      setAccounts([]); // Ensure accounts is always an array
      setError('Failed to load trading accounts. Please ensure the TraderView backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleAccountChange = (account) => {
    setSelectedAccount(account);
  };

  return (
    <div className="App">
      <Navbar bg="dark" variant="dark" expand="lg" className="navbar-dark">
        <Container>
          <Navbar.Brand href="#home">
            <i className="bi bi-graph-up me-2"></i>
            TraderView
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link 
                as={Link} 
                to="/" 
                className={location.pathname === '/' ? 'active' : ''}
              >
                <i className="bi bi-speedometer2 me-1"></i>
                Dashboard
              </Nav.Link>
              <Nav.Link 
                as={Link} 
                to="/analytics" 
                className={location.pathname === '/analytics' ? 'active' : ''}
              >
                <i className="bi bi-bar-chart me-1"></i>
                Analytics
              </Nav.Link>
              <Nav.Link 
                as={Link} 
                to="/orders" 
                className={location.pathname === '/orders' ? 'active' : ''}
              >
                <i className="bi bi-list-ul me-1"></i>
                Orders
              </Nav.Link>
              <Nav.Link 
                as={Link} 
                to="/symbols" 
                className={location.pathname === '/symbols' ? 'active' : ''}
              >
                <i className="bi bi-currency-exchange me-1"></i>
                Symbols
              </Nav.Link>
              <Nav.Link 
                as={Link} 
                to="/accounts" 
                className={location.pathname === '/accounts' ? 'active' : ''}
              >
                <i className="bi bi-bank me-1"></i>
                Accounts
              </Nav.Link>
            </Nav>
            
            {/* Account Selector */}
            {Array.isArray(accounts) && accounts.length > 0 && (
              <Nav className="me-3">
                <select
                  className="form-select form-select-sm"
                  value={selectedAccount?.id || ''}
                  onChange={(e) => {
                    const account = accounts.find(acc => acc.id === parseInt(e.target.value));
                    handleAccountChange(account);
                  }}
                  style={{ maxWidth: '200px' }}
                >
                  <option value="">Select Account</option>
                  {accounts.map(account => (
                    <option key={account.id} value={account.id}>
                      {account.accountName} ({account.brokerName})
                    </option>
                  ))}
                </select>
              </Nav>
            )}

            {/* User Profile */}
            <Nav>
              <UserProfile />
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <Container fluid className="mt-4">
        {error && (
          <Alert variant="danger" className="mb-4">
            <i className="bi bi-exclamation-triangle me-2"></i>
            {error}
          </Alert>
        )}

        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3">Loading TraderView data...</p>
          </div>
        ) : (
          <Routes>
            <Route 
              path="/" 
              element={
                <Dashboard 
                  selectedAccount={selectedAccount} 
                  onAccountChange={handleAccountChange}
                />
              } 
            />
            <Route 
              path="/analytics" 
              element={
                <Analytics 
                  selectedAccount={selectedAccount}
                />
              } 
            />
            <Route 
              path="/orders" 
              element={
                <OrdersList 
                  selectedAccount={selectedAccount}
                />
              } 
            />
            <Route
              path="/symbols"
              element={
                <SymbolsList
                  selectedAccount={selectedAccount}
                />
              }
            />
            <Route 
              path="/accounts" 
              element={
                <TradingAccounts 
                  accounts={accounts}
                  onAccountsChange={loadAccounts}
                />
              } 
            />
          </Routes>
        )}
      </Container>

      <footer className="mt-5 py-4 text-center text-muted">
        <Container>
          <small>
            TraderView v1.0.0 |
            <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noopener noreferrer" className="text-info ms-2">
              API Documentation
            </a>
          </small>
        </Container>
      </footer>

      {/* Session Timeout Warning */}
      <SessionTimeoutWarning />
    </div>
  );
}

export default App;

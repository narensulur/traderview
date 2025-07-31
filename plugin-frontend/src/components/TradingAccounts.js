import React, { useState, useEffect } from 'react';
import { Table, Card, Alert, Spinner, Badge, Button, Modal, Form } from 'react-bootstrap';
import { tradingAccountsApi, brokersApi } from '../services/api';

const TradingAccounts = ({ accounts, onAccountsChange }) => {
  const [brokers, setBrokers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [newAccount, setNewAccount] = useState({
    accountName: '',
    accountNumber: '',
    brokerId: '',
    isActive: true
  });

  useEffect(() => {
    loadBrokers();
  }, []);

  const loadBrokers = async () => {
    try {
      const response = await brokersApi.getActiveBrokers();
      const brokersData = Array.isArray(response.data) ? response.data : [];
      setBrokers(brokersData);
    } catch (err) {
      console.error('Failed to load brokers:', err);
      setBrokers([]); // Ensure brokers is always an array
      setError('Failed to load brokers');
    }
  };

  const handleCreateAccount = async () => {
    try {
      setLoading(true);
      await tradingAccountsApi.createAccount(newAccount);
      setShowModal(false);
      setNewAccount({
        accountName: '',
        accountNumber: '',
        brokerId: '',
        isActive: true
      });
      onAccountsChange(); // Refresh accounts list
    } catch (err) {
      console.error('Failed to create account:', err);
      setError('Failed to create account');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteAccount = async (accountId) => {
    if (window.confirm('Are you sure you want to delete this account?')) {
      try {
        setLoading(true);
        await tradingAccountsApi.deleteAccount(accountId);
        onAccountsChange(); // Refresh accounts list
      } catch (err) {
        console.error('Failed to delete account:', err);
        setError('Failed to delete account');
      } finally {
        setLoading(false);
      }
    }
  };

  const formatCurrency = (value) => {
    if (value === null || value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-bank me-2"></i>
          Trading Accounts
        </h2>
        <Button variant="primary" onClick={() => setShowModal(true)}>
          <i className="bi bi-plus-circle me-2"></i>
          Add Account
        </Button>
      </div>

      {error && (
        <Alert variant="danger" className="mb-4">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      <Card>
        <Card.Header>
          <div className="d-flex justify-content-between align-items-center">
            <h5 className="mb-0">Accounts List</h5>
            <Badge bg="info">{accounts.length} accounts</Badge>
          </div>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
              <p className="mt-3">Loading accounts...</p>
            </div>
          ) : accounts.length > 0 ? (
            <div className="table-responsive">
              <Table striped hover variant="dark" className="mb-0">
                <thead>
                  <tr>
                    <th>Account Name</th>
                    <th>Account Number</th>
                    <th>Broker</th>
                    <th>Balance</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {accounts.map(account => (
                    <tr key={account.id}>
                      <td>
                        <strong className="text-primary">{account.accountName}</strong>
                      </td>
                      <td>
                        <code>{account.accountNumber}</code>
                      </td>
                      <td>{account.brokerName}</td>
                      <td>{formatCurrency(account.balance)}</td>
                      <td>
                        <Badge bg={account.isActive ? 'success' : 'secondary'}>
                          {account.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </td>
                      <td>
                        <small className="text-muted">
                          {new Date(account.createdAt).toLocaleDateString()}
                        </small>
                      </td>
                      <td>
                        <Button
                          variant="outline-danger"
                          size="sm"
                          onClick={() => handleDeleteAccount(account.id)}
                          disabled={loading}
                        >
                          <i className="bi bi-trash"></i>
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          ) : (
            <div className="text-center py-5">
              <i className="bi bi-bank display-1 text-muted"></i>
              <h4 className="mt-3">No Trading Accounts</h4>
              <p className="text-muted">Create your first trading account to get started.</p>
            </div>
          )}
        </Card.Body>
      </Card>

      {/* Add Account Modal */}
      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton className="bg-dark text-white">
          <Modal.Title>Add Trading Account</Modal.Title>
        </Modal.Header>
        <Modal.Body className="bg-dark text-white">
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Account Name</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter account name"
                value={newAccount.accountName}
                onChange={(e) => setNewAccount(prev => ({
                  ...prev,
                  accountName: e.target.value
                }))}
              />
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Label>Account Number</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter account number"
                value={newAccount.accountNumber}
                onChange={(e) => setNewAccount(prev => ({
                  ...prev,
                  accountNumber: e.target.value
                }))}
              />
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Label>Broker</Form.Label>
              <Form.Select
                value={newAccount.brokerId}
                onChange={(e) => setNewAccount(prev => ({
                  ...prev,
                  brokerId: e.target.value
                }))}
              >
                <option value="">Select a broker</option>
                {Array.isArray(brokers) && brokers.map(broker => (
                  <option key={broker.id} value={broker.id}>
                    {broker.displayName}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            
            <Form.Group className="mb-3">
              <Form.Check
                type="checkbox"
                label="Active"
                checked={newAccount.isActive}
                onChange={(e) => setNewAccount(prev => ({
                  ...prev,
                  isActive: e.target.checked
                }))}
              />
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer className="bg-dark">
          <Button variant="secondary" onClick={() => setShowModal(false)}>
            Cancel
          </Button>
          <Button 
            variant="primary" 
            onClick={handleCreateAccount}
            disabled={loading || !newAccount.accountName || !newAccount.brokerId}
          >
            {loading ? <Spinner animation="border" size="sm" /> : 'Create Account'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default TradingAccounts;

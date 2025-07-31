import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Alert, Spinner } from 'react-bootstrap';
import { analyticsApi } from '../services/api';

const Dashboard = ({ selectedAccount }) => {
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (selectedAccount) {
      loadDashboardData();
    }
  }, [selectedAccount]);

  const loadDashboardData = async () => {
    if (!selectedAccount) return;

    try {
      setLoading(true);
      setError(null);
      
      // Get last 30 days of data
      const endDate = new Date().toISOString();
      const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();
      
      const response = await analyticsApi.getDashboardSummary(
        selectedAccount.id, 
        startDate, 
        endDate
      );
      
      setDashboardData(response.data);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value) => {
    if (value === null || value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  };

  const formatPercentage = (value) => {
    if (value === null || value === undefined) return 'N/A';
    return `${Number(value).toFixed(2)}%`;
  };

  if (!selectedAccount) {
    return (
      <div className="text-center py-5">
        <i className="bi bi-bank display-1 text-muted"></i>
        <h3 className="mt-3">No Trading Account Selected</h3>
        <p className="text-muted">Please select a trading account to view dashboard data.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Loading dashboard data...</p>
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="danger">
        <i className="bi bi-exclamation-triangle me-2"></i>
        {error}
      </Alert>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-speedometer2 me-2"></i>
          Dashboard
        </h2>
        <div className="text-muted">
          Account: <strong>{selectedAccount.accountName}</strong> ({selectedAccount.brokerName})
        </div>
      </div>

      {dashboardData ? (
        <Row>
          {/* Total P&L */}
          <Col md={3} className="mb-4">
            <Card>
              <Card.Body>
                <div className="d-flex justify-content-between align-items-center">
                  <div>
                    <h6 className="text-muted mb-1">Total P&L</h6>
                    <h4 className={dashboardData.totalPnl >= 0 ? 'text-success' : 'text-danger'}>
                      {formatCurrency(dashboardData.totalPnl)}
                    </h4>
                  </div>
                  <i className="bi bi-graph-up-arrow display-6 text-primary"></i>
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Total Trades */}
          <Col md={3} className="mb-4">
            <Card>
              <Card.Body>
                <div className="d-flex justify-content-between align-items-center">
                  <div>
                    <h6 className="text-muted mb-1">Total Trades</h6>
                    <h4>{dashboardData.totalTrades || 0}</h4>
                  </div>
                  <i className="bi bi-list-ul display-6 text-info"></i>
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Win Rate */}
          <Col md={3} className="mb-4">
            <Card>
              <Card.Body>
                <div className="d-flex justify-content-between align-items-center">
                  <div>
                    <h6 className="text-muted mb-1">Win Rate</h6>
                    <h4 className="text-warning">
                      {formatPercentage(dashboardData.winRate)}
                    </h4>
                  </div>
                  <i className="bi bi-trophy display-6 text-warning"></i>
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Average Trade */}
          <Col md={3} className="mb-4">
            <Card>
              <Card.Body>
                <div className="d-flex justify-content-between align-items-center">
                  <div>
                    <h6 className="text-muted mb-1">Avg Trade</h6>
                    <h4 className={dashboardData.averageTrade >= 0 ? 'text-success' : 'text-danger'}>
                      {formatCurrency(dashboardData.averageTrade)}
                    </h4>
                  </div>
                  <i className="bi bi-calculator display-6 text-secondary"></i>
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Recent Activity */}
          <Col md={6} className="mb-4">
            <Card>
              <Card.Header>
                <h5 className="mb-0">
                  <i className="bi bi-clock-history me-2"></i>
                  Recent Activity
                </h5>
              </Card.Header>
              <Card.Body>
                <div className="text-center py-4">
                  <i className="bi bi-info-circle display-6 text-muted"></i>
                  <p className="mt-3 text-muted">Recent trades and orders will appear here</p>
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Performance Summary */}
          <Col md={6} className="mb-4">
            <Card>
              <Card.Header>
                <h5 className="mb-0">
                  <i className="bi bi-bar-chart me-2"></i>
                  Performance Summary
                </h5>
              </Card.Header>
              <Card.Body>
                <div className="row">
                  <div className="col-6">
                    <small className="text-muted">Winning Trades</small>
                    <div className="h5 text-success">
                      {dashboardData.winningTrades || 0}
                    </div>
                  </div>
                  <div className="col-6">
                    <small className="text-muted">Losing Trades</small>
                    <div className="h5 text-danger">
                      {dashboardData.losingTrades || 0}
                    </div>
                  </div>
                  <div className="col-6 mt-3">
                    <small className="text-muted">Best Trade</small>
                    <div className="h6 text-success">
                      {formatCurrency(dashboardData.bestTrade)}
                    </div>
                  </div>
                  <div className="col-6 mt-3">
                    <small className="text-muted">Worst Trade</small>
                    <div className="h6 text-danger">
                      {formatCurrency(dashboardData.worstTrade)}
                    </div>
                  </div>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      ) : (
        <div className="text-center py-5">
          <i className="bi bi-graph-up display-1 text-muted"></i>
          <h3 className="mt-3">No Data Available</h3>
          <p className="text-muted">No trading data found for the selected account.</p>
        </div>
      )}
    </div>
  );
};

export default Dashboard;

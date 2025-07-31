import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Alert, Spinner, Form, Button } from 'react-bootstrap';
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { analyticsApi, advancedAnalyticsApi } from '../services/api';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

const Analytics = ({ selectedAccount }) => {
  const [dailyPnlData, setDailyPnlData] = useState([]);
  const [symbolPerformance, setSymbolPerformance] = useState([]);
  const [tradeStats, setTradeStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    if (selectedAccount) {
      loadAnalyticsData();
    }
  }, [selectedAccount, dateRange]);

  const loadAnalyticsData = async () => {
    if (!selectedAccount) return;

    try {
      setLoading(true);
      setError(null);

      const startDate = new Date(dateRange.startDate).toISOString();
      const endDate = new Date(dateRange.endDate).toISOString();

      // Load all analytics data
      const [dailyPnlResponse, symbolResponse, statsResponse] = await Promise.all([
        analyticsApi.getDailyPnl(selectedAccount.id, startDate, endDate),
        analyticsApi.getSymbolPerformance(selectedAccount.id, startDate, endDate),
        advancedAnalyticsApi.getTradeStats(selectedAccount.id, {
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        })
      ]);

      const dailyPnlArray = Array.isArray(dailyPnlResponse.data) ? dailyPnlResponse.data : [];
      const symbolPerfArray = Array.isArray(symbolResponse.data) ? symbolResponse.data : [];

      setDailyPnlData(dailyPnlArray);
      setSymbolPerformance(symbolPerfArray);
      setTradeStats(statsResponse.data);
    } catch (err) {
      console.error('Failed to load analytics data:', err);
      setError('Failed to load analytics data');
      // Ensure arrays are always arrays on error
      setDailyPnlData([]);
      setSymbolPerformance([]);
      setTradeStats(null);
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (field, value) => {
    setDateRange(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Chart configurations
  const dailyPnlChartData = {
    labels: Array.isArray(dailyPnlData) ? dailyPnlData.map(item => new Date(item.date).toLocaleDateString()) : [],
    datasets: [
      {
        label: 'Daily P&L',
        data: Array.isArray(dailyPnlData) ? dailyPnlData.map(item => item.pnl) : [],
        borderColor: 'rgb(52, 152, 219)',
        backgroundColor: 'rgba(52, 152, 219, 0.1)',
        tension: 0.1,
      },
      {
        label: 'Cumulative P&L',
        data: Array.isArray(dailyPnlData) ? dailyPnlData.map(item => item.cumulativePnl) : [],
        borderColor: 'rgb(39, 174, 96)',
        backgroundColor: 'rgba(39, 174, 96, 0.1)',
        tension: 0.1,
      }
    ],
  };

  const symbolPerformanceChartData = {
    labels: Array.isArray(symbolPerformance) ? symbolPerformance.slice(0, 10).map(item => item.symbol) : [],
    datasets: [
      {
        label: 'P&L by Symbol',
        data: Array.isArray(symbolPerformance) ? symbolPerformance.slice(0, 10).map(item => item.totalPnl) : [],
        backgroundColor: Array.isArray(symbolPerformance) ? symbolPerformance.slice(0, 10).map(item =>
          item.totalPnl >= 0 ? 'rgba(39, 174, 96, 0.8)' : 'rgba(231, 76, 60, 0.8)'
        ) : [],
        borderColor: Array.isArray(symbolPerformance) ? symbolPerformance.slice(0, 10).map(item =>
          item.totalPnl >= 0 ? 'rgb(39, 174, 96)' : 'rgb(231, 76, 60)'
        ) : [],
        borderWidth: 1,
      },
    ],
  };

  const winLossChartData = tradeStats ? {
    labels: ['Winning Trades', 'Losing Trades'],
    datasets: [
      {
        data: [tradeStats.winningTrades || 0, tradeStats.losingTrades || 0],
        backgroundColor: ['rgba(39, 174, 96, 0.8)', 'rgba(231, 76, 60, 0.8)'],
        borderColor: ['rgb(39, 174, 96)', 'rgb(231, 76, 60)'],
        borderWidth: 1,
      },
    ],
  } : null;

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        labels: {
          color: '#ffffff'
        }
      }
    },
    scales: {
      x: {
        ticks: {
          color: '#ffffff'
        },
        grid: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      },
      y: {
        ticks: {
          color: '#ffffff'
        },
        grid: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      }
    }
  };

  if (!selectedAccount) {
    return (
      <div className="text-center py-5">
        <i className="bi bi-bar-chart display-1 text-muted"></i>
        <h3 className="mt-3">No Trading Account Selected</h3>
        <p className="text-muted">Please select a trading account to view analytics.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-bar-chart me-2"></i>
          Analytics
        </h2>
        <div className="text-muted">
          Account: <strong>{selectedAccount.accountName}</strong>
        </div>
      </div>

      {/* Date Range Filter */}
      <Card className="mb-4">
        <Card.Body>
          <Row className="align-items-end">
            <Col md={4}>
              <Form.Group>
                <Form.Label>Start Date</Form.Label>
                <Form.Control
                  type="date"
                  value={dateRange.startDate}
                  onChange={(e) => handleDateRangeChange('startDate', e.target.value)}
                />
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group>
                <Form.Label>End Date</Form.Label>
                <Form.Control
                  type="date"
                  value={dateRange.endDate}
                  onChange={(e) => handleDateRangeChange('endDate', e.target.value)}
                />
              </Form.Group>
            </Col>
            <Col md={4}>
              <Button variant="primary" onClick={loadAnalyticsData} disabled={loading}>
                {loading ? <Spinner animation="border" size="sm" /> : 'Update Charts'}
              </Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {error && (
        <Alert variant="danger" className="mb-4">
          <i className="bi bi-exclamation-triangle me-2"></i>
          {error}
        </Alert>
      )}

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
          <p className="mt-3">Loading analytics data...</p>
        </div>
      ) : (
        <Row>
          {/* Daily P&L Chart */}
          <Col lg={8} className="mb-4">
            <Card>
              <Card.Header>
                <h5 className="mb-0">Daily P&L Trend</h5>
              </Card.Header>
              <Card.Body>
                <div className="chart-container">
                  {dailyPnlData.length > 0 ? (
                    <Line data={dailyPnlChartData} options={chartOptions} />
                  ) : (
                    <div className="text-center py-4">
                      <p className="text-muted">No daily P&L data available</p>
                    </div>
                  )}
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Win/Loss Ratio */}
          <Col lg={4} className="mb-4">
            <Card>
              <Card.Header>
                <h5 className="mb-0">Win/Loss Ratio</h5>
              </Card.Header>
              <Card.Body>
                <div className="chart-container">
                  {winLossChartData ? (
                    <Doughnut data={winLossChartData} options={chartOptions} />
                  ) : (
                    <div className="text-center py-4">
                      <p className="text-muted">No trade data available</p>
                    </div>
                  )}
                </div>
              </Card.Body>
            </Card>
          </Col>

          {/* Symbol Performance */}
          <Col lg={12} className="mb-4">
            <Card>
              <Card.Header>
                <h5 className="mb-0">Top 10 Symbols by P&L</h5>
              </Card.Header>
              <Card.Body>
                <div className="chart-container">
                  {symbolPerformance.length > 0 ? (
                    <Bar data={symbolPerformanceChartData} options={chartOptions} />
                  ) : (
                    <div className="text-center py-4">
                      <p className="text-muted">No symbol performance data available</p>
                    </div>
                  )}
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
};

export default Analytics;

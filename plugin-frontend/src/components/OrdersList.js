import React, { useState, useEffect } from 'react';
import { Table, Card, Alert, Spinner, Form, Row, Col, Badge, Button, Pagination } from 'react-bootstrap';
import { useSearchParams } from 'react-router-dom';
import { ordersApi } from '../services/api';

const OrdersList = ({ selectedAccount }) => {
  const [searchParams] = useSearchParams();
  const symbolFilter = searchParams.get('symbol');

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    status: '',
    symbol: symbolFilter || '',
    orderPlacedDate: ''
  });
  const [sortConfig, setSortConfig] = useState({
    key: null,
    direction: 'asc'
  });
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 100;

  useEffect(() => {
    if (selectedAccount) {
      loadOrders();
    }
  }, [selectedAccount]);

  const loadOrders = async () => {
    if (!selectedAccount) return;

    try {
      setLoading(true);
      setError(null);
      
      const response = await ordersApi.getOrdersByAccount(selectedAccount.id);
      setOrders(response.data);
    } catch (err) {
      console.error('Failed to load orders:', err);
      setError('Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const applyFilters = () => {
    loadOrders();
  };

  // Filter orders based on current filters
  const filteredOrders = orders.filter(order => {
    if (filters.status && order.status !== filters.status) return false;
    if (filters.symbol && !order.symbolTicker.toLowerCase().includes(filters.symbol.toLowerCase())) return false;
    if (filters.orderPlacedDate) {
      // Extract date part from order timestamp (YYYY-MM-DD format)
      const orderDatePart = order.placedAt.split('T')[0];
      if (orderDatePart !== filters.orderPlacedDate) return false;
    }
    return true;
  });

  // Sort filtered orders
  const sortedOrders = React.useMemo(() => {
    if (!sortConfig.key) return filteredOrders;

    return [...filteredOrders].sort((a, b) => {
      let aValue = a[sortConfig.key];
      let bValue = b[sortConfig.key];

      // Handle null/undefined values
      if (aValue === null || aValue === undefined) aValue = '';
      if (bValue === null || bValue === undefined) bValue = '';

      // Convert to numbers for numeric fields
      if (['quantity', 'price', 'filledQuantity', 'avgTradesPerDay', 'realizedPnl'].includes(sortConfig.key)) {
        aValue = Number(aValue) || 0;
        bValue = Number(bValue) || 0;
      }

      // Convert dates to timestamps for date fields
      if (['placedAt', 'filledAt'].includes(sortConfig.key)) {
        aValue = new Date(aValue).getTime() || 0;
        bValue = new Date(bValue).getTime() || 0;
      }

      if (aValue < bValue) {
        return sortConfig.direction === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return sortConfig.direction === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }, [filteredOrders, sortConfig]);

  // Pagination logic
  const totalPages = Math.ceil(sortedOrders.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedOrders = sortedOrders.slice(startIndex, endIndex);

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [filters, sortConfig]);

  const formatCurrency = (value) => {
    if (value === null || value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  };

  const handleSort = (key) => {
    let direction = 'asc';
    if (sortConfig.key === key && sortConfig.direction === 'asc') {
      direction = 'desc';
    }
    setSortConfig({ key, direction });
  };

  const getSortIcon = (columnKey) => {
    if (sortConfig.key !== columnKey) {
      return <i className="bi bi-arrow-down-up text-muted ms-1"></i>;
    }
    return sortConfig.direction === 'asc'
      ? <i className="bi bi-arrow-up text-primary ms-1"></i>
      : <i className="bi bi-arrow-down text-primary ms-1"></i>;
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return 'N/A';
    const date = new Date(dateTime);
    const day = date.getDate();
    const month = date.toLocaleString('en-US', { month: 'long' });
    const time = date.toLocaleString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
    return `${day} ${month}, ${time}`;
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      'FILLED': 'success',
      'PARTIALLY_FILLED': 'warning',
      'PENDING': 'info',
      'CANCELLED': 'secondary',
      'REJECTED': 'danger'
    };
    
    return (
      <Badge bg={statusColors[status] || 'secondary'}>
        {status}
      </Badge>
    );
  };

  const getSideBadge = (side) => {
    return (
      <Badge bg={side === 'BUY' ? 'success' : 'danger'}>
        {side}
      </Badge>
    );
  };

  if (!selectedAccount) {
    return (
      <div className="text-center py-5">
        <i className="bi bi-list-ul display-1 text-muted"></i>
        <h3 className="mt-3">No Trading Account Selected</h3>
        <p className="text-muted">Please select a trading account to view orders.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-list-ul me-2"></i>
          Orders
        </h2>
        <div className="text-muted">
          Account: <strong>{selectedAccount.accountName}</strong>
        </div>
      </div>

      {/* Filters */}
      <Card className="mb-4">
        <Card.Body>
          <Row className="align-items-end">
            <Col md={4}>
              <Form.Group>
                <Form.Label>Status</Form.Label>
                <Form.Select
                  value={filters.status}
                  onChange={(e) => handleFilterChange('status', e.target.value)}
                >
                  <option value="">All Statuses</option>
                  <option value="FILLED">Filled</option>
                  <option value="PARTIALLY_FILLED">Partially Filled</option>
                  <option value="PENDING">Pending</option>
                  <option value="CANCELLED">Cancelled</option>
                  <option value="REJECTED">Rejected</option>
                </Form.Select>
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group>
                <Form.Label>Symbol</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="Filter by symbol..."
                  value={filters.symbol}
                  onChange={(e) => handleFilterChange('symbol', e.target.value)}
                />
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group>
                <Form.Label>Order Placed</Form.Label>
                <Form.Control
                  type="date"
                  value={filters.orderPlacedDate}
                  onChange={(e) => handleFilterChange('orderPlacedDate', e.target.value)}
                  placeholder="Filter by order date..."
                />
              </Form.Group>
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

      <Card>
        <Card.Header>
          <div className="d-flex justify-content-between align-items-center">
            <h5 className="mb-0">Orders List</h5>
            <Badge bg="info">{orders.length} orders</Badge>
          </div>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
              <p className="mt-3">Loading orders...</p>
            </div>
          ) : paginatedOrders.length > 0 ? (
            <>
              <div className="table-responsive">
              <Table striped hover variant="dark" className="mb-0">
                <thead>
                  <tr>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('symbolTicker')}
                    >
                      Symbol {getSortIcon('symbolTicker')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('side')}
                    >
                      Side {getSortIcon('side')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('quantity')}
                    >
                      Quantity {getSortIcon('quantity')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('price')}
                    >
                      Price {getSortIcon('price')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('filledQuantity')}
                    >
                      Filled {getSortIcon('filledQuantity')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('avgTradesPerDay')}
                    >
                      Avg Trades/Day {getSortIcon('avgTradesPerDay')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('realizedPnl')}
                    >
                      P/L Amount {getSortIcon('realizedPnl')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('placedAt')}
                    >
                      Order Placed {getSortIcon('placedAt')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedOrders.map(order => (
                    <tr key={order.id}>
                      <td>
                        <strong className="text-warning">{order.symbolTicker}</strong>
                      </td>
                      <td>
                        {getSideBadge(order.side)}
                      </td>
                      <td>{order.quantity}</td>
                      <td>{formatCurrency(order.price)}</td>
                      <td>
                        {order.filledQuantity || 0} / {order.quantity}
                      </td>
                      <td>
                        <span className="text-info">
                          {order.avgTradesPerDay ? Number(order.avgTradesPerDay).toFixed(2) : 'N/A'}
                        </span>
                      </td>
                      <td>
                        <span className={order.realizedPnl >= 0 ? 'text-success' : 'text-danger'}>
                          {formatCurrency(order.realizedPnl)}
                        </span>
                      </td>
                      <td>
                        <small>{formatDateTime(order.placedAt)}</small>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>

            {totalPages > 1 && (
              <div className="d-flex justify-content-between align-items-center mt-3 px-3 pb-3">
                <div className="text-muted">
                  Showing {startIndex + 1}-{Math.min(endIndex, sortedOrders.length)} of {sortedOrders.length} orders
                </div>
                <Pagination>
                  <Pagination.First
                    onClick={() => setCurrentPage(1)}
                    disabled={currentPage === 1}
                  />
                  <Pagination.Prev
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 1}
                  />

                  {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                    let pageNum;
                    if (totalPages <= 5) {
                      pageNum = i + 1;
                    } else if (currentPage <= 3) {
                      pageNum = i + 1;
                    } else if (currentPage >= totalPages - 2) {
                      pageNum = totalPages - 4 + i;
                    } else {
                      pageNum = currentPage - 2 + i;
                    }

                    return (
                      <Pagination.Item
                        key={pageNum}
                        active={pageNum === currentPage}
                        onClick={() => setCurrentPage(pageNum)}
                      >
                        {pageNum}
                      </Pagination.Item>
                    );
                  })}

                  <Pagination.Next
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  />
                  <Pagination.Last
                    onClick={() => setCurrentPage(totalPages)}
                    disabled={currentPage === totalPages}
                  />
                </Pagination>
              </div>
            )}
            </>
          ) : (
            <div className="text-center py-5">
              <i className="bi bi-inbox display-1 text-muted"></i>
              <h4 className="mt-3">No Orders Found</h4>
              <p className="text-muted">No orders found for the selected account and filters.</p>
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default OrdersList;

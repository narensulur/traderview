import React, { useState, useEffect } from 'react';
import { Table, Card, Alert, Spinner, Form, Row, Col, Badge, InputGroup, Button, Pagination } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { symbolsApi } from '../services/api';

const SymbolsList = ({ selectedAccount }) => {
  const navigate = useNavigate();
  const [symbols, setSymbols] = useState([]);
  const [filteredSymbols, setFilteredSymbols] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [sortConfig, setSortConfig] = useState({
    key: 'tradesThisWeek',
    direction: 'desc'
  });
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 100;
  const [filters, setFilters] = useState({
    search: '',
    assetType: '',
    exchange: ''
  });

  useEffect(() => {
    loadSymbols();
  }, [selectedAccount]);

  useEffect(() => {
    applyFilters();
  }, [symbols, filters]);

  const loadSymbols = async () => {
    if (!selectedAccount) return;

    try {
      setLoading(true);
      setError(null);

      const response = await symbolsApi.getSymbolsWithWeeklyStats(selectedAccount.id);
      setSymbols(response.data);
    } catch (err) {
      console.error('Failed to load symbols:', err);
      setError('Failed to load symbols');
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

  const handleViewOrders = (symbolTicker) => {
    navigate(`/orders?symbol=${encodeURIComponent(symbolTicker)}`);
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

  const sortedSymbols = React.useMemo(() => {
    if (!sortConfig.key) return filteredSymbols;

    return [...filteredSymbols].sort((a, b) => {
      const aValue = a[sortConfig.key];
      const bValue = b[sortConfig.key];

      if (aValue < bValue) {
        return sortConfig.direction === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return sortConfig.direction === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }, [filteredSymbols, sortConfig]);

  // Pagination logic
  const totalPages = Math.ceil(sortedSymbols.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedSymbols = sortedSymbols.slice(startIndex, endIndex);

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [filters, sortConfig]);

  const applyFilters = () => {
    let filtered = [...symbols];

    // Search filter
    if (filters.search) {
      const searchTerm = filters.search.toLowerCase();
      filtered = filtered.filter(symbol => 
        symbol.ticker.toLowerCase().includes(searchTerm) ||
        symbol.name.toLowerCase().includes(searchTerm)
      );
    }

    // Asset type filter
    if (filters.assetType) {
      filtered = filtered.filter(symbol => symbol.assetType === filters.assetType);
    }

    // Exchange filter
    if (filters.exchange) {
      filtered = filtered.filter(symbol => symbol.exchange === filters.exchange);
    }

    setFilteredSymbols(filtered);
  };

  const getAssetTypeBadge = (assetType) => {
    const assetTypeColors = {
      'STOCK': 'primary',
      'OPTION': 'warning',
      'FUTURE': 'info',
      'FOREX': 'success',
      'CRYPTO': 'danger',
      'ETF': 'secondary',
      'BOND': 'dark'
    };
    
    return (
      <Badge bg={assetTypeColors[assetType] || 'secondary'}>
        {assetType}
      </Badge>
    );
  };

  // Get unique values for filter dropdowns
  const uniqueAssetTypes = [...new Set(symbols.map(s => s.assetType))].sort();
  const uniqueExchanges = [...new Set(symbols.map(s => s.exchange))].sort();

  if (!selectedAccount) {
    return (
      <div className="text-center py-5">
        <i className="bi bi-diagram-3 display-1 text-muted"></i>
        <h3 className="mt-3">No Trading Account Selected</h3>
        <p className="text-muted">Please select a trading account to view symbols.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
        <p className="mt-3">Loading symbols...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>
          <i className="bi bi-currency-exchange me-2"></i>
          Symbols
        </h2>
        <Badge bg="info">{sortedSymbols.length} symbols</Badge>
      </div>

      {/* Filters */}
      <Card className="mb-4">
        <Card.Body>
          <Row>
            <Col md={4}>
              <Form.Group>
                <Form.Label>Search</Form.Label>
                <InputGroup>
                  <InputGroup.Text>
                    <i className="bi bi-search"></i>
                  </InputGroup.Text>
                  <Form.Control
                    type="text"
                    placeholder="Search by ticker or name..."
                    value={filters.search}
                    onChange={(e) => handleFilterChange('search', e.target.value)}
                  />
                </InputGroup>
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group>
                <Form.Label>Asset Type</Form.Label>
                <Form.Select
                  value={filters.assetType}
                  onChange={(e) => handleFilterChange('assetType', e.target.value)}
                >
                  <option value="">All Asset Types</option>
                  {uniqueAssetTypes.map(type => (
                    <option key={type} value={type}>{type}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col md={4}>
              <Form.Group>
                <Form.Label>Exchange</Form.Label>
                <Form.Select
                  value={filters.exchange}
                  onChange={(e) => handleFilterChange('exchange', e.target.value)}
                >
                  <option value="">All Exchanges</option>
                  {uniqueExchanges.map(exchange => (
                    <option key={exchange} value={exchange}>{exchange}</option>
                  ))}
                </Form.Select>
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
            <h5 className="mb-0">Symbols List</h5>
            <div className="text-muted">
              Showing {filteredSymbols.length} of {symbols.length} symbols
            </div>
          </div>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
              <p className="mt-3">Loading symbols...</p>
            </div>
          ) : paginatedSymbols.length > 0 ? (
            <>
              <div className="table-responsive">
              <Table striped hover variant="dark" className="mb-0">
                <thead>
                  <tr>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('ticker')}
                    >
                      Ticker {getSortIcon('ticker')}
                    </th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('name')}
                    >
                      Name {getSortIcon('name')}
                    </th>
                    <th>Asset Type</th>
                    <th>Exchange</th>
                    <th
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleSort('tradesThisWeek')}
                    >
                      Trades This Week {getSortIcon('tradesThisWeek')}
                    </th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedSymbols.map(symbol => (
                    <tr key={symbol.id}>
                      <td>
                        <strong className="text-primary">{symbol.ticker}</strong>
                      </td>
                      <td>{symbol.name}</td>
                      <td>
                        {getAssetTypeBadge(symbol.assetType)}
                      </td>
                      <td>
                        <Badge bg="outline-secondary">{symbol.exchange}</Badge>
                      </td>
                      <td>
                        <span className="badge bg-info fs-6">
                          {symbol.tradesThisWeek || 0}
                        </span>
                      </td>
                      <td>
                        <Button
                          variant="outline-primary"
                          size="sm"
                          onClick={() => handleViewOrders(symbol.ticker)}
                        >
                          <i className="bi bi-list-ul me-1"></i>
                          View Orders
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>

            {totalPages > 1 && (
              <div className="d-flex justify-content-between align-items-center mt-3 px-3 pb-3">
                <div className="text-muted">
                  Showing {startIndex + 1}-{Math.min(endIndex, sortedSymbols.length)} of {sortedSymbols.length} symbols
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
              <i className="bi bi-search display-1 text-muted"></i>
              <h4 className="mt-3">No Symbols Found</h4>
              <p className="text-muted">
                {filters.search || filters.assetType || filters.exchange
                  ? 'No symbols match your current filters.'
                  : 'No symbols available in the system.'
                }
              </p>
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default SymbolsList;

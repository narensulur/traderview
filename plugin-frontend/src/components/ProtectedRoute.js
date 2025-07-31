import React from 'react';
import { Container, Spinner } from 'react-bootstrap';
import { useAuth } from '../contexts/AuthContext';
import Login from './Login';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <Container fluid className="min-vh-100 d-flex align-items-center justify-content-center bg-dark">
        <div className="text-center text-light">
          <Spinner animation="border" variant="primary" style={{ width: '3rem', height: '3rem' }} />
          <div className="mt-3">
            <h4>Loading TraderView...</h4>
            <p className="text-muted">Checking authentication status</p>
          </div>
        </div>
      </Container>
    );
  }

  if (!isAuthenticated) {
    return <Login />;
  }

  return children;
};

export default ProtectedRoute;

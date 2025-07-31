import React, { useEffect } from 'react';
import { Container, Card, Button, Row, Col } from 'react-bootstrap';
import { useAuth } from '../contexts/AuthContext';

const Login = () => {
  const { login, loginAsDeveloper } = useAuth();

  useEffect(() => {
    // Render Google Sign-In button when component mounts
    if (window.google) {
      window.google.accounts.id.renderButton(
        document.getElementById('google-signin-button'),
        {
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          shape: 'rectangular',
          logo_alignment: 'left',
          width: 250,
        }
      );
    }
  }, []);

  const handleSignInClick = () => {
    login();
  };

  return (
    <Container fluid className="min-vh-100 d-flex align-items-center justify-content-center bg-dark">
      <Row className="w-100 justify-content-center">
        <Col xs={12} sm={8} md={6} lg={4} xl={3}>
          <Card className="shadow-lg border-0">
            <Card.Body className="p-5 text-center">
              <div className="mb-4">
                <i className="bi bi-graph-up display-1 text-primary"></i>
              </div>
              
              <h2 className="mb-3 fw-bold">TraderView</h2>
              <p className="text-muted mb-4">
                Professional trading analytics dashboard
              </p>
              
              <div className="mb-4">
                <div id="google-signin-button" className="d-flex justify-content-center"></div>
              </div>
              
              <div className="mb-3">
                <Button
                  variant="primary"
                  size="lg"
                  className="w-100"
                  onClick={handleSignInClick}
                >
                  <i className="bi bi-google me-2"></i>
                  Sign in with Google
                </Button>
              </div>

              {/* Development Login - Remove in Production */}
              {process.env.NODE_ENV === 'development' && (
                <div className="mb-3">
                  <Button
                    variant="outline-warning"
                    size="lg"
                    className="w-100"
                    onClick={loginAsDeveloper}
                  >
                    <i className="bi bi-code-slash me-2"></i>
                    Developer Login (Testing)
                  </Button>
                  <small className="text-muted d-block mt-1 text-center">
                    For development testing only
                  </small>
                </div>
              )}
              
              <div className="text-center">
                <small className="text-muted">
                  Secure authentication powered by Google
                </small>
              </div>
              
              <hr className="my-4" />
              
              <div className="text-start">
                <h6 className="fw-bold mb-3">Features:</h6>
                <ul className="list-unstyled small text-muted">
                  <li className="mb-2">
                    <i className="bi bi-check-circle-fill text-success me-2"></i>
                    Real-time trading analytics
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-check-circle-fill text-success me-2"></i>
                    Order management & tracking
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-check-circle-fill text-success me-2"></i>
                    Symbol performance insights
                  </li>
                  <li className="mb-2">
                    <i className="bi bi-check-circle-fill text-success me-2"></i>
                    Secure 2-hour sessions
                  </li>
                </ul>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default Login;

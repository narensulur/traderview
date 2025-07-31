import React, { useState, useEffect } from 'react';
import { Modal, Button, ProgressBar } from 'react-bootstrap';
import { useAuth } from '../contexts/AuthContext';

const SessionTimeoutWarning = () => {
  const { logout } = useAuth();
  const [showWarning, setShowWarning] = useState(false);
  const [timeLeft, setTimeLeft] = useState(0);
  const [warningTimer, setWarningTimer] = useState(null);

  useEffect(() => {
    const checkSessionTimeout = () => {
      const loginTime = localStorage.getItem('traderview_login_time');
      if (!loginTime) return;

      const currentTime = new Date().getTime();
      const sessionDuration = 2 * 60 * 60 * 1000; // 2 hours
      const warningTime = 5 * 60 * 1000; // 5 minutes before expiry
      const timeSinceLogin = currentTime - parseInt(loginTime);
      const timeUntilExpiry = sessionDuration - timeSinceLogin;

      if (timeUntilExpiry <= warningTime && timeUntilExpiry > 0) {
        setShowWarning(true);
        setTimeLeft(Math.floor(timeUntilExpiry / 1000));
        
        // Start countdown
        const timer = setInterval(() => {
          setTimeLeft(prev => {
            if (prev <= 1) {
              clearInterval(timer);
              logout();
              return 0;
            }
            return prev - 1;
          });
        }, 1000);
        
        setWarningTimer(timer);
      } else if (timeUntilExpiry <= 0) {
        logout();
      }
    };

    // Check immediately
    checkSessionTimeout();
    
    // Check every minute
    const interval = setInterval(checkSessionTimeout, 60000);

    return () => {
      clearInterval(interval);
      if (warningTimer) {
        clearInterval(warningTimer);
      }
    };
  }, [logout, warningTimer]);

  const handleExtendSession = () => {
    // Reset login time to extend session
    const newLoginTime = new Date().getTime();
    localStorage.setItem('traderview_login_time', newLoginTime.toString());
    setShowWarning(false);
    if (warningTimer) {
      clearInterval(warningTimer);
    }
  };

  const handleLogoutNow = () => {
    logout();
  };

  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const progressPercentage = Math.max(0, (timeLeft / (5 * 60)) * 100);

  return (
    <Modal 
      show={showWarning} 
      backdrop="static" 
      keyboard={false}
      centered
    >
      <Modal.Header className="bg-warning text-dark">
        <Modal.Title>
          <i className="bi bi-exclamation-triangle me-2"></i>
          Session Expiring Soon
        </Modal.Title>
      </Modal.Header>
      
      <Modal.Body>
        <div className="text-center mb-3">
          <p className="mb-3">
            Your session will expire in <strong>{formatTime(timeLeft)}</strong>
          </p>
          
          <ProgressBar 
            variant="warning" 
            now={progressPercentage} 
            className="mb-3"
            style={{ height: '10px' }}
          />
          
          <p className="text-muted small">
            For security reasons, sessions automatically expire after 2 hours of activity.
          </p>
        </div>
      </Modal.Body>
      
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={handleLogoutNow}>
          <i className="bi bi-box-arrow-right me-1"></i>
          Logout Now
        </Button>
        <Button variant="primary" onClick={handleExtendSession}>
          <i className="bi bi-arrow-clockwise me-1"></i>
          Extend Session
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default SessionTimeoutWarning;

import React, { useState } from 'react';
import { Dropdown, Image } from 'react-bootstrap';
import { useAuth } from '../contexts/AuthContext';

const UserProfile = () => {
  const { user, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);

  if (!user) return null;

  const handleLogout = () => {
    logout();
  };

  return (
    <Dropdown show={showDropdown} onToggle={setShowDropdown} align="end">
      <Dropdown.Toggle
        as="div"
        className="d-flex align-items-center text-decoration-none text-light"
        style={{ cursor: 'pointer' }}
      >
        <Image
          src={user.picture}
          alt={user.name}
          roundedCircle
          width={32}
          height={32}
          className="me-2"
        />
        <span className="d-none d-md-inline">{user.given_name}</span>
        <i className="bi bi-chevron-down ms-1"></i>
      </Dropdown.Toggle>

      <Dropdown.Menu className="shadow">
        <Dropdown.Header>
          <div className="d-flex align-items-center">
            <Image
              src={user.picture}
              alt={user.name}
              roundedCircle
              width={40}
              height={40}
              className="me-3"
            />
            <div>
              <div className="fw-bold">{user.name}</div>
              <small className="text-muted">{user.email}</small>
            </div>
          </div>
        </Dropdown.Header>
        
        <Dropdown.Divider />
        
        <Dropdown.Item>
          <i className="bi bi-person me-2"></i>
          Profile Settings
        </Dropdown.Item>
        
        <Dropdown.Item>
          <i className="bi bi-gear me-2"></i>
          Preferences
        </Dropdown.Item>
        
        <Dropdown.Divider />
        
        <Dropdown.Item onClick={handleLogout} className="text-danger">
          <i className="bi bi-box-arrow-right me-2"></i>
          Sign Out
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
};

export default UserProfile;

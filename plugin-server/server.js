const express = require('express');
const cors = require('cors');
const axios = require('axios');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3001;
const TRADERVIEW_API_BASE_URL = process.env.TRADERVIEW_API_BASE_URL || 'http://localhost:8080/api';

// Middleware
app.use(helmet());
app.use(morgan('combined'));
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
  credentials: true
}));
app.use(express.json());
app.use(express.static('public'));

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Proxy middleware for TraderView API
const proxyHandler = async (req, res) => {
  try {
    // Use req.originalUrl to get the full path including /api
    const fullPath = req.originalUrl;

    // Remove /api prefix and construct target URL
    const apiPath = fullPath.replace('/api', '');
    const targetUrl = `${TRADERVIEW_API_BASE_URL}${apiPath}`;

    console.log(`Proxying ${req.method} ${targetUrl}`);

    const config = {
      method: req.method,
      url: targetUrl,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      params: req.query
    };

    if (req.body && Object.keys(req.body).length > 0) {
      config.data = req.body;
    }

    const response = await axios(config);
    res.status(response.status).json(response.data);
  } catch (error) {
    console.error('Proxy error:', error.message);
    if (error.response) {
      res.status(error.response.status).json(error.response.data);
    } else {
      res.status(500).json({
        error: 'Internal server error',
        message: error.message
      });
    }
  }
};

// API Routes - Proxy to TraderView backend
app.use('/api', proxyHandler);

// Serve React app (when built)
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Error handling middleware
app.use((error, req, res, next) => {
  console.error('Server error:', error);
  res.status(500).json({ 
    error: 'Internal server error',
    message: process.env.NODE_ENV === 'development' ? error.message : 'Something went wrong'
  });
});

app.listen(PORT, () => {
  console.log(`🚀 TraderView Server running on port ${PORT}`);
  console.log(`📊 Proxying to TraderView API: ${TRADERVIEW_API_BASE_URL}`);
  console.log(`🌐 Environment: ${process.env.NODE_ENV}`);
});

module.exports = app;

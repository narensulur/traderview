# Google OAuth Setup Guide

## 1. Create Google OAuth Credentials

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API:
   - Go to "APIs & Services" > "Library"
   - Search for "Google+ API" and enable it
4. Create OAuth 2.0 credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Choose "Web application"
   - Add authorized JavaScript origins:
     - `http://localhost:3000` (for development)
     - Your production domain (for production)
   - Add authorized redirect URIs:
     - `http://localhost:3000` (for development)
     - Your production domain (for production)

## 2. Configure Environment Variables

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Update the `.env` file with your Google Client ID:
   ```
   REACT_APP_GOOGLE_CLIENT_ID=your-actual-client-id.apps.googleusercontent.com
   ```

## 3. Test Authentication

1. Start the development server:
   ```bash
   npm start
   ```

2. Navigate to `http://localhost:3000`
3. You should see the login page with Google sign-in options
4. Click "Sign in with Google" to test the authentication flow

## 4. Session Management

- Sessions automatically expire after 2 hours
- Users receive a 5-minute warning before expiration
- Users can extend their session or logout manually
- All pages are protected behind authentication

## 5. Security Features

- JWT token validation
- Automatic session timeout
- Secure token storage in localStorage
- Session extension capability
- Manual logout functionality

## Troubleshooting

### Common Issues:

1. **"Invalid client ID"**: Make sure your Google Client ID is correct in the `.env` file
2. **"Unauthorized domain"**: Add your domain to authorized JavaScript origins in Google Console
3. **"Sign-in popup blocked"**: Allow popups for your domain in browser settings
4. **Session not persisting**: Check browser localStorage and ensure no extensions are clearing it

### Development vs Production:

- Development: Use `http://localhost:3000`
- Production: Use your actual domain (must be HTTPS)
- Update Google Console settings for each environment

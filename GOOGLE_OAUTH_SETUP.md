# Google OAuth2 Login Setup Guide

This guide will help you set up Google OAuth2 login for the SwapTicket application.

## Prerequisites

1. A Google account
2. Access to Google Cloud Console
3. SwapTicket application running locally

## Step 1: Create a Google Cloud Project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Click on "Select a project" dropdown at the top
3. Click "New Project"
4. Enter project name (e.g., "SwapTicket OAuth")
5. Click "Create"

## Step 2: Enable Google+ API

1. In the Google Cloud Console, go to "APIs & Services" > "Library"
2. Search for "Google+ API"
3. Click on it and click "Enable"
4. Also enable "Google OAuth2 API" if available

## Step 3: Configure OAuth Consent Screen

1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type (unless you have a Google Workspace account)
3. Click "Create"
4. Fill in the required information:
   - App name: SwapTicket
   - User support email: Your email
   - Developer contact information: Your email
5. Click "Save and Continue"
6. Skip the "Scopes" section for now (click "Save and Continue")
7. Add test users if needed (for development)
8. Click "Save and Continue"

## Step 4: Create OAuth2 Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Web application"
4. Enter name: "SwapTicket Web Client"
5. Add Authorized JavaScript origins:
   - `http://localhost:8085`
   - `https://yourdomain.com` (for production)
6. Add Authorized redirect URIs:
   - `http://localhost:8085/login/oauth2/code/google`
   - `https://yourdomain.com/login/oauth2/code/google` (for production)
7. Click "Create"
8. Copy the Client ID and Client Secret

## Step 5: Update Application Configuration

1. Open `src/main/resources/application.properties`
2. Replace the placeholder values:

```properties
# Google OAuth2 Properties
spring.security.oauth2.client.registration.google.client-id=YOUR_ACTUAL_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_ACTUAL_CLIENT_SECRET
```

## Step 6: Test the Setup

1. Start your SwapTicket application:
   ```bash
   mvn spring-boot:run
   ```

2. Navigate to `http://localhost:8085/login`

3. You should see both:
   - Traditional login form
   - "Continue with Google" button

4. Click "Continue with Google" to test OAuth2 login

## Features Implemented

### 1. Dual Login System
- **Traditional Login**: Email/password authentication (existing)
- **Google OAuth2**: One-click Google authentication (new)

### 2. User Account Management
- New users logging in with Google are automatically created in the database
- Existing users can link their accounts with Google
- Google users get default values for required fields (mobile, DOB) that they can update later

### 3. Security Configuration
- Spring Security handles OAuth2 flow automatically
- Session management works the same for both login methods
- Proper logout functionality for both authentication types

### 4. Database Changes
- Added `provider` field to User model (LOCAL/GOOGLE)
- Added `googleId` field for OAuth2 users
- Backward compatible with existing users

## Troubleshooting

### Common Issues

1. **"redirect_uri_mismatch" error**
   - Check that your redirect URIs in Google Console match exactly
   - Make sure you're using the correct port (8085)

2. **"invalid_client" error**
   - Verify your Client ID and Client Secret are correct
   - Check for any extra spaces in the configuration

3. **"access_denied" error**
   - Make sure your OAuth consent screen is properly configured
   - Add your email as a test user during development

4. **Database errors after login**
   - Ensure your database schema is updated (run with `spring.jpa.hibernate.ddl-auto=update`)
   - Check that all required fields have default values

### Development vs Production

**Development (localhost):**
- Use `http://localhost:8085` as origin
- Use `http://localhost:8085/login/oauth2/code/google` as redirect URI

**Production:**
- Use your actual domain as origin
- Use `https://yourdomain.com/login/oauth2/code/google` as redirect URI
- Make sure to update the application.properties for production

## Security Best Practices

1. **Never commit credentials**: Keep your Client ID and Client Secret secure
2. **Use environment variables**: For production, use environment variables instead of hardcoded values
3. **HTTPS in production**: Always use HTTPS for OAuth2 in production
4. **Scope limitation**: Only request necessary scopes (profile, email)

## Next Steps

After successful setup, you can:
1. Customize the Google login button styling
2. Add more OAuth2 providers (Facebook, GitHub, etc.)
3. Implement account linking for users with both local and OAuth2 accounts
4. Add user profile completion flow for Google users

## Support

If you encounter any issues:
1. Check the application logs for detailed error messages
2. Verify your Google Cloud Console configuration
3. Ensure all dependencies are properly installed
4. Test with a fresh browser session (clear cookies)
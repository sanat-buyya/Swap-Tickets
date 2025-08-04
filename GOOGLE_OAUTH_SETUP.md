# Google OAuth2 Setup Guide for SwapTicket

## Overview
This guide will help you set up Google OAuth2 authentication for the SwapTicket application, allowing users to login with their Google accounts.

## Prerequisites
- Google account
- Access to Google Cloud Console
- SwapTicket application running locally or deployed

## Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Select a project" and then "New Project"
3. Enter project name: `SwapTicket OAuth`
4. Click "Create"

## Step 2: Enable Google+ API

1. In the Google Cloud Console, go to "APIs & Services" > "Library"
2. Search for "Google+ API" and click on it
3. Click "Enable"

## Step 3: Configure OAuth Consent Screen

1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type and click "Create"
3. Fill in the required information:
   - **App name**: SwapTicket
   - **User support email**: Your email
   - **Developer contact information**: Your email
4. Click "Save and Continue"
5. On the "Scopes" page, click "Save and Continue"
6. On the "Test users" page, add your email for testing
7. Click "Save and Continue"

## Step 4: Create OAuth2 Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Choose "Web application"
4. Enter name: `SwapTicket Web Client`
5. Add Authorized redirect URIs:
   - For local development: `http://localhost:8080/login/oauth2/code/google`
   - For production: `https://yourdomain.com/login/oauth2/code/google`
6. Click "Create"
7. Copy the **Client ID** and **Client Secret**

## Step 5: Update Application Configuration

1. Open `src/main/resources/application.properties`
2. Replace the placeholder values:

```properties
# Google OAuth2 Configuration
spring.security.oauth2.client.registration.google.client-id=YOUR_ACTUAL_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_ACTUAL_CLIENT_SECRET
```

**Example:**
```properties
spring.security.oauth2.client.registration.google.client-id=123456789-abcdefghijklmnop.apps.googleusercontent.com
spring.security.oauth2.client.registration.google.client-secret=GOCSPX-1234567890abcdefghijklmnop
```

## Step 6: Update Database Configuration

Make sure your database configuration is also updated in `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce?createDatabaseIfNotExist=true
spring.datasource.username=your_actual_db_username
spring.datasource.password=your_actual_db_password
```

## Step 7: Test the Integration

1. Start your application
2. Navigate to `http://localhost:8080/login`
3. You should see both:
   - Traditional email/password login form
   - "Continue with Google" button
4. Click "Continue with Google" to test OAuth2 login

## How It Works

### User Flow
1. User clicks "Continue with Google" on login page
2. User is redirected to Google's OAuth2 authorization server
3. User grants permission to SwapTicket
4. Google redirects back to SwapTicket with authorization code
5. SwapTicket exchanges code for access token
6. SwapTicket fetches user profile information
7. If user doesn't exist, a new account is created
8. User is logged in and redirected to user home page

### Database Changes
The User model now includes:
- `googleId`: Stores Google's unique user identifier
- `emailVerified`: Tracks email verification status (Google emails are pre-verified)

### Security Features
- OAuth2 users don't need to provide mobile numbers or date of birth
- Existing users can link their Google accounts
- Session management works the same for both authentication methods

## Troubleshooting

### Common Issues

1. **"redirect_uri_mismatch" error**
   - Check that the redirect URI in Google Console matches exactly
   - For local development: `http://localhost:8080/login/oauth2/code/google`

2. **"invalid_client" error**
   - Verify Client ID and Client Secret are correct
   - Check for extra spaces or characters

3. **"access_denied" error**
   - User declined to grant permissions
   - Check OAuth consent screen configuration

4. **Database errors**
   - Ensure database is running and accessible
   - Check that new columns (googleId, emailVerified) are created

### Logs to Check
- Application startup logs for OAuth2 configuration
- Database connection logs
- Authentication flow logs in browser developer tools

## Production Deployment

### Additional Steps for Production:
1. Update redirect URI in Google Console to use HTTPS
2. Set up proper SSL certificates
3. Update `application.properties` with production database settings
4. Consider using environment variables for sensitive data:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
```

## Security Considerations

1. **Keep credentials secure**: Never commit actual Client ID/Secret to version control
2. **Use HTTPS in production**: OAuth2 requires secure connections
3. **Validate redirect URIs**: Only allow trusted domains
4. **Monitor OAuth usage**: Check Google Cloud Console for usage patterns

## Support

If you encounter issues:
1. Check Google Cloud Console for error messages
2. Review application logs for authentication errors
3. Verify all configuration steps were completed
4. Test with a simple OAuth2 flow first

---

**Note**: This implementation allows users to login with either traditional email/password or Google OAuth2. Both authentication methods can coexist, and users can link their existing accounts with Google.
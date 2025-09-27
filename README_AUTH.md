# CitiAlerts PH - Authentication System

## Overview

This authentication system provides user registration and login functionality with two user levels:
**User** and **Moderator**.

## Components Created

### Android Activities

1. **SplashActivity** - Entry point that checks user session
2. **LoginActivity** - User authentication
3. **RegisterActivity** - User registration with moderator verification
4. **MainActivity** - Main app with logout functionality

### PHP Backend API

Location: `app/api/`

1. **config.php** - Database configuration and table creation
2. **login.php** - Login endpoint
3. **register.php** - Registration endpoint with file upload support

### Utility Classes

1. **SessionManager** - Handles user session data
2. **User** - User model class
3. **ApiClient** - HTTP request handling

## Database Setup

### Database Configuration

- **Host**: localhost
- **Username**: root
- **Password**: (empty)
- **Database**: citialerts

### Users Table Structure

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_type ENUM('user', 'moderator') DEFAULT 'user',
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    profile_image VARCHAR(255),
    verification_documents TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## User Types

### User

- Automatically verified upon registration
- Can immediately access the app after registration
- No document verification required

### Moderator

- Requires document verification for approval
- Must upload valid IDs, certificates, or authority documents
- Account pending until manually verified by admin
- Can post alerts and announcements once verified

## API Endpoints

### POST /login.php

**Request:**

```json
{
    "username": "user@example.com",
    "password": "password123"
}
```

**Response:**

```json
{
    "success": true,
    "message": "Login successful",
    "user": {
        "id": 1,
        "username": "johndoe",
        "email": "john@example.com",
        "first_name": "John",
        "last_name": "Doe",
        "user_type": "user",
        "is_verified": true
    }
}
```

### POST /register.php

**For Regular Users (JSON):**

```json
{
    "first_name": "John",
    "last_name": "Doe",
    "username": "johndoe",
    "email": "john@example.com",
    "phone": "09123456789",
    "password": "password123",
    "user_type": "user"
}
```

**For Moderators (Multipart Form Data):**

- All user fields above
- `documents[]`: File uploads for verification

## Setup Instructions

### Backend Setup

1. Create MySQL database named `citialerts`
2. Place PHP files in web server directory (e.g., `xampp/htdocs/citialerts/app/api/`)
3. Ensure `uploads/verification/` directory exists and is writable
4. Start Apache and MySQL services

### Android App Setup

1. Update API base URL in code if needed:
    - Emulator: `http://10.0.2.2/citialerts/app/api/`
    - Physical device: `http://[YOUR_IP]/citialerts/app/api/`
2. Build and run the app

## Features

### Security

- Password hashing using PHP's `password_hash()`
- Session management with SharedPreferences
- Input validation and sanitization
- File type validation for document uploads

### User Experience

- Splash screen with session check
- Auto-login for regular users after registration
- Document upload interface for moderators
- Logout functionality
- Error handling and user feedback

### File Upload Support

- Supported formats: JPG, JPEG, PNG, GIF, PDF, DOC, DOCX
- Multiple file selection for moderators
- Unique filename generation to prevent conflicts

## Testing

1. Register as a regular user - should auto-login
2. Register as a moderator - should require login after registration
3. Test login with both username and email
4. Test document upload for moderator registration
5. Test logout functionality

## Notes

- Moderator verification is manual and requires admin intervention
- Document files are stored in `uploads/verification/` directory
- Session data is cleared on logout
- Network requests are handled asynchronously to prevent UI blocking
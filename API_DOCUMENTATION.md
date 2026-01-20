# MyBooks API Documentation

## Base URL
```
http://localhost:8080
```

## Authentication
Most endpoints require authentication via session cookies (set after login) or HTTP Basic Auth.

---

## Public Endpoints

### 1. Health Check
**GET** `/hello`

**Description:** Simple health check endpoint

**Headers:** None required

**Response:**
- **200 OK**
  ```
  Hello from MyBooks API!
  ```

---

### 2. User Registration
**POST** `/api/auth/register`

**Description:** Register a new user account

**Headers:**
- `Content-Type: application/json`

**Request Body:**
```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 6 chars, required)"
}
```

**Responses:**
- **201 Created**
  ```json
  {
    "message": "User registered successfully",
    "userId": 1,
    "username": "john_doe",
    "email": "john@example.com"
  }
  ```
- **400 Bad Request** - Validation errors or email already exists
  ```json
  {
    "error": "Email already exists"
  }
  ```

---

### 3. User Login
**POST** `/api/auth/login`

**Description:** Authenticate user and create session

**Headers:**
- `Content-Type: application/json`

**Request Body:**
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**Responses:**
- **200 OK**
  ```json
  {
    "message": "Login successful",
    "userId": 1,
    "username": "john_doe",
    "sessionId": "ABC123..."
  }
  ```
  Sets `JSESSIONID` cookie

- **401 Unauthorized** - Invalid credentials
  ```json
  {
    "error": "Invalid credentials"
  }
  ```

---

## Protected Endpoints (Authentication Required)

### 4. User Logout
**POST** `/api/auth/logout`

**Description:** Invalidate user session

**Headers:** Session cookie required

**Response:**
- **200 OK**
  ```json
  {
    "message": "Logout successful"
  }
  ```

---

### 5. Create Book
**POST** `/api/books`

**Description:** Add a new book to user's collection

**Headers:**
- `Content-Type: application/json`
- Session cookie or Basic Auth

**Request Body:**
```json
{
  "title": "string (required)",
  "author": "string (required)",
  "isbn": "string (optional)",
  "publicationYear": "integer (1000-2100, optional)",
  "genre": "string (optional)",
  "readingStatus": "enum (NOT_STARTED|READING|COMPLETED, optional)",
  "rating": "integer (1-5, optional)",
  "notes": "string (optional)"
}
```

**Responses:**
- **201 Created**
  ```json
  {
    "id": 1,
    "title": "1984",
    "author": "George Orwell",
    "isbn": "978-0451524935",
    "publicationYear": 1949,
    "genre": "Fiction",
    "readingStatus": "READING",
    "rating": 5,
    "notes": "Classic dystopian novel",
    "createdAt": "2026-01-19T22:00:00"
  }
  ```
- **400 Bad Request** - Validation errors
- **401 Unauthorized** - Not authenticated

---

### 6. Get All Books
**GET** `/api/books`

**Description:** Retrieve all books for authenticated user

**Headers:** Session cookie or Basic Auth

**Query Parameters:** None

**Response:**
- **200 OK**
  ```json
  [
    {
      "id": 1,
      "title": "1984",
      "author": "George Orwell",
      ...
    }
  ]
  ```
- **401 Unauthorized** - Not authenticated

---

### 7. Get Book by ID
**GET** `/api/books/{id}`

**Description:** Retrieve specific book details

**Headers:** Session cookie or Basic Auth

**Path Parameters:**
- `id` - Book ID (integer)

**Responses:**
- **200 OK** - Returns book object
- **401 Unauthorized** - Not authenticated
- **403 Forbidden** - Book belongs to another user
- **404 Not Found** - Book doesn't exist

---

### 8. Get Books by Reading Status
**GET** `/api/books/status/{status}`

**Description:** Filter books by reading status

**Headers:** Session cookie or Basic Auth

**Path Parameters:**
- `status` - Reading status (NOT_STARTED|READING|COMPLETED)

**Response:**
- **200 OK** - Returns array of books
- **401 Unauthorized** - Not authenticated

---

### 9. Get Books by Genre
**GET** `/api/books/genre/{genre}`

**Description:** Filter books by genre

**Headers:** Session cookie or Basic Auth

**Path Parameters:**
- `genre` - Genre name (string)

**Response:**
- **200 OK** - Returns array of books
- **401 Unauthorized** - Not authenticated

---

### 10. Update Book
**PUT** `/api/books/{id}`

**Description:** Update existing book details

**Headers:**
- `Content-Type: application/json`
- Session cookie or Basic Auth

**Path Parameters:**
- `id` - Book ID (integer)

**Request Body:** Same as Create Book

**Responses:**
- **200 OK** - Returns updated book
- **400 Bad Request** - Validation errors
- **401 Unauthorized** - Not authenticated
- **403 Forbidden** - Book belongs to another user
- **404 Not Found** - Book doesn't exist

---

### 11. Delete Book
**DELETE** `/api/books/{id}`

**Description:** Remove book from collection

**Headers:** Session cookie or Basic Auth

**Path Parameters:**
- `id` - Book ID (integer)

**Responses:**
- **204 No Content** - Successfully deleted
- **401 Unauthorized** - Not authenticated
- **403 Forbidden** - Book belongs to another user
- **404 Not Found** - Book doesn't exist

---

## HTTP Status Codes Used

- **200 OK** - Successful GET/PUT request
- **201 Created** - Successful POST (resource created)
- **204 No Content** - Successful DELETE
- **400 Bad Request** - Validation errors or malformed input
- **401 Unauthorized** - Authentication required
- **403 Forbidden** - Authenticated but not authorized
- **404 Not Found** - Resource doesn't exist
- **415 Unsupported Media Type** - Wrong Content-Type
- **500 Internal Server Error** - Server-side error

---

## Content Types

**Request:**
- `application/json` - All POST/PUT endpoints

**Response:**
- `application/json` - All endpoints return JSON

---

## Authentication Methods

1. **Session Cookie** (Recommended)
    - Login via `/api/auth/login`
    - Cookie automatically sent with subsequent requests

2. **HTTP Basic Auth** (Alternative)
    - Header: `Authorization: Basic <base64(email:password)>`
    - Supported on all protected endpoints

---

## Error Response Format

All errors follow this structure:
```json
{
  "error": "Error message description"
}
```

For validation errors:
```json
{
  "error": "Validation failed",
  "details": {
    "username": "Username must be between 3 and 50 characters",
    "email": "Email must be valid"
  }
}
```
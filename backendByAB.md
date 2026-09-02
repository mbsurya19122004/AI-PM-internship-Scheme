# Backend Documentation — Internship & Job Recommendation Platform

> Complete backend knowledge base and documentation reference and explaination by @abhaypratap08 

---

# 1. Project Overview

## For a Layman

This backend is the "brain" of a career recommendation platform. When a student uploads their resume, this backend:

1. **Registers the student** and keeps their account secure.
2. **Stores their resume files** safely.
3. **Stores internship listings** from various companies.
4. **Sends the resume to an AI service** (a separate Python program) to understand what the student knows.
5. **Compares the student's profile against internships** and returns ranked recommendations.
6. **Protects everything** with login, passwords, and security tokens.
7. **Verifies email addresses** with secure tokens and email delivery.
8. **Manages profile pictures** with secure file upload, storage, and retrieval.
9. **Enforces role-based access** — admins can manage users, regular users cannot.
10. **Sends password reset emails** with time-limited secure tokens.

Think of it like a **smart career counselor** that reads your resume, understands your skills, and tells you which internships are the best fit — while keeping your information safe.

## For an Evaluator / Developer

This is a **RESTful API backend** built with **Java 17** and **Spring Boot 3.2.5**. It follows a standard **layered architecture**:

```
Controller → Service → Repository → Database
```

**Major modules:**

| Module | Responsibility |
|--------|---------------|
| Authentication | User registration, login, JWT tokens, password management, email verification |
| Resume Management | Upload, store, download, activate, delete resume files |
| Internship Management | CRUD operations for internship listings |
| ML Integration | Proxy calls to a separate Python ML service for resume analysis and job matching |
| Security | JWT authentication, input sanitization, rate limiting, account lockout, audit logging |
| Profile Pictures | Secure image upload, storage, retrieval, and deletion |
| Admin / RBAC | Role-based access control, user management, admin endpoints |
| Email Service | Password reset emails, email verification emails |

**Communication:** The frontend (React/Vue at `localhost:3000` or `localhost:5173`) sends HTTP requests to this backend at `localhost:8080`. The backend proxies ML requests to a separate Python FastAPI service at `localhost:8000`.

---

# 2. Technology Stack

| Technology | Where Used | Why It Is Used | Simple Explanation |
|------------|------------|----------------|-------------------|
| Java 17 | Entire backend | Language runtime | The programming language everything is written in |
| Spring Boot 3.2.5 | Entire backend | Application framework | Provides structure, auto-configuration, and ready-made tools for building web apps |
| Spring Security | Authentication & Authorization | Security framework | Handles login, JWT tokens, password hashing, protecting routes |
| Spring Data JPA | Database layer | ORM (Object-Relational Mapping) | Lets Java objects map to database tables automatically |
| H2 Database | Database | In-memory database for development | A lightweight database that runs in memory (no installation needed) |
| Hibernate | JPA Implementation | Database schema management | Automatically creates and updates database tables from Java entity classes |
| JWT (jjwt 0.12.5) | Authentication | Stateless token-based auth | Creates and validates digital tokens that prove a user is logged in |
| BCrypt | Password security | Password hashing | Converts passwords into irreversible encrypted form |
| Lombok | All Java files | Reduces boilerplate code | Automatically generates getters, setters, constructors, builders |
| Maven | Build & dependencies | Build automation tool | Downloads libraries and compiles the project |
| Spring Web | REST API | HTTP request handling | Receives and responds to API requests |
| Spring Validation | Input validation | Request data validation | Ensures user input meets required rules before processing |

---

# 3. Backend Architecture

## How It All Connects

```
Client / Frontend (React, Vue, or Postman)
        ↓
HTTP Request (with JWT token in Authorization header)
        ↓
Spring Security Filter Chain
   ├── JwtAuthenticationFilter validates token
   ├── Checks token version matches stored version
   └── Sets authentication context if valid
        ↓
Controller Layer
   ├── Receives request
   ├── Validates input (@Valid annotation)
   └── Calls appropriate Service method
        ↓
Service Layer
   ├── Contains all business logic
   ├── Calls Repository for data access
   ├── Calls SecurityEventLogger for audit
   └── Handles transactions
        ↓
Repository Layer (Spring Data JPA)
   ├── Interfaces extending JpaRepository
   ├── Auto-generated CRUD methods
   └── Custom JPQL queries
        ↓
Database (H2 in-memory)
        ↓
Response travels back through same layers
        ↓
Client / Frontend receives JSON response
```

## Layer Explanations

### Controller Layer — The Receptionist

Controllers receive HTTP requests and route them to the correct service. They do NOT contain business logic.

**Technical:** Spring `@RestController` classes that map HTTP endpoints to Java methods. They use `@Valid` for input validation and `@AuthenticationPrincipal` to identify the current user.

**Files:**
- `AuthController.java` — handles registration, login, token refresh, profile, password operations
- `ResumeController.java` — handles resume upload, download, activation, deletion
- `InternshipController.java` — handles internship CRUD
- `MlController.java` — proxies ML service calls

### Service Layer — The Worker

Services contain all business logic. They process data, enforce rules, and coordinate between controllers and repositories.

**Technical:** Spring `@Service` classes with `@Transactional` methods. They throw exceptions that are caught by the global exception handler.

**Files:**
- `AuthService.java` — registration, login, token management, password change/reset
- `ResumeService.java` — file validation, storage, retrieval, activation
- `InternshipService.java` — internship CRUD operations
- `MlService.java` — HTTP calls to external Python ML service

### Repository Layer — The Librarian

Repositories communicate with the database. They use Spring Data JPA to automatically generate database queries from method names.

**Technical:** Java interfaces extending `JpaRepository<Entity, ID>`. Custom queries use `@Query` annotations with JPQL.

**Files:**
- `UserRepository.java`
- `ResumeRepository.java`
- `InternshipRepository.java`
- `PasswordResetTokenRepository.java`

### Entity/Model Layer — The Blueprint

Entities define database table structures. Each entity class maps to exactly one database table.

**Files:**
- `User.java` — users table
- `Resume.java` — resumes table
- `Internship.java` — internships table
- `PasswordResetToken.java` — password_reset_tokens table

### DTO Layer — The Translation

DTOs (Data Transfer Objects) define the shape of data sent to and received from the API. They prevent exposing internal database structure.

**Files:** 13 DTO classes in `dto/` package (see Section 4).

### Security Layer — The Guard

Handles authentication, authorization, input sanitization, rate limiting, and audit logging.

**Files:** 8 security classes in `security/` package (see Section 11).

---

# 4. Important Folder and File Structure

```
backend/
├── pom.xml                          ← Maven build config, dependencies
├── src/main/java/com/internshipplatform/
│   ├── InternshipPlatformApplication.java   ← Application entry point
│   ├── config/
│   │   ├── SecurityConfig.java      ← Security rules, CORS, headers
│   │   └── DataLoader.java          ← Seeds sample internship data on startup
│   ├── controller/
│   │   ├── AuthController.java      ← Auth endpoints (register, login, etc.)
│   │   ├── ResumeController.java    ← Resume CRUD endpoints
│   │   ├── InternshipController.java ← Internship endpoints
│   │   ├── MlController.java        ← ML service proxy endpoints
│   │   ├── UserController.java      ← Profile picture endpoints
│   │   └── AdminController.java     ← Admin user management endpoints
│   ├── service/
│   │   ├── AuthService.java         ← Auth business logic
│   │   ├── ResumeService.java       ← Resume business logic
│   │   ├── InternshipService.java   ← Internship business logic
│   │   ├── MlService.java           ← ML service HTTP client
│   │   ├── EmailService.java        ← SMTP email sending
│   │   └── ProfilePictureService.java ← Profile picture file storage
│   ├── repository/
│   │   ├── UserRepository.java      ← User database operations
│   │   ├── ResumeRepository.java    ← Resume database operations
│   │   ├── InternshipRepository.java ← Internship database operations
│   │   ├── PasswordResetTokenRepository.java ← Reset token operations
│   │   └── EmailVerificationTokenRepository.java ← Verification token ops
│   ├── entity/
│   │   ├── User.java                ← User table definition
│   │   ├── Resume.java              ← Resume table definition
│   │   ├── Internship.java          ← Internship table definition
│   │   ├── PasswordResetToken.java  ← Reset token table definition
│   │   └── EmailVerificationToken.java ← Email verification token definition
│   ├── dto/
│   │   ├── ApiResponse.java         ← Standard API response wrapper
│   │   ├── AuthResponse.java        ← Login/register response
│   │   ├── LoginRequest.java        ← Login request body
│   │   ├── RegisterRequest.java     ← Registration request body
│   │   ├── RefreshTokenRequest.java ← Token refresh request
│   │   ├── ChangePasswordRequest.java ← Password change request
│   │   ├── ForgotPasswordRequest.java ← Forgot password request
│   │   ├── ResetPasswordRequest.java ← Password reset request
│   │   ├── UserResponse.java        ← User profile response
│   │   ├── InternshipDto.java       ← Internship request/response
│   │   ├── ResumeResponse.java      ← Resume response
│   │   ├── AnalyzeResumeRequest.java ← ML analyze request
│   │   └── JobMatchResponse.java    ← ML match response
│   ├── security/
│   │   ├── JwtTokenProvider.java    ← JWT token creation/validation
│   │   ├── JwtAuthenticationFilter.java ← Validates JWT on every request
│   │   ├── JwtAuthenticationEntryPoint.java ← Returns 401 for unauthenticated
│   │   ├── JwtAccessDeniedHandler.java ← Returns 403 for forbidden
│   │   ├── CustomUserDetailsService.java ← Loads user data for Spring Security
│   │   ├── InputSanitizer.java      ← Strips HTML/XSS from user input
│   │   ├── SecurityEventLogger.java ← Audit trail logging
│   │   └── RateLimiter.java         ← Rate limiting for password reset
│   └── exception/
│       └── GlobalExceptionHandler.java ← Catches all exceptions, returns JSON
├── src/main/resources/
│   ├── application.properties       ← Main config (server, DB, JWT, etc.)
│   └── application-dev.properties   ← Dev-only overrides (H2 console, SQL)
└── src/test/java/com/internshipplatform/
    ├── AuthControllerTest.java      ← 35 auth endpoint tests (incl. password change/reset)
    ├── ResumeControllerTest.java    ← 12 resume endpoint tests
    └── JwtTokenProviderTest.java    ← 6 JWT token tests
```

---

# 5. Application Startup Flow

```
Application Starts
        ↓
main() method executes in InternshipPlatformApplication.java
        ↓
Spring Boot framework initializes
        ↓
application.properties is loaded
   ├── Server starts on port 8080
   ├── H2 database configured (jdbc:h2:mem:internshipdb)
   ├── JWT configuration loaded (secret, expiry times)
   └── File upload limits configured (10MB)
        ↓
If --spring.profiles.active=dev:
   application-dev.properties loaded
   ├── H2 console enabled at /h2-console
   └── SQL logging enabled
        ↓
Spring Security configuration applied (SecurityConfig.java)
   ├── JWT filter registered
   ├── CORS configured (localhost:3000, localhost:5173)
   ├── Public endpoints defined
   ├── Security headers configured (HSTS, CSP, etc.)
   └── Password encoder (BCrypt) created
        ↓
Hibernate scans entity classes and creates/updates database tables
   ├── users table created
   ├── resumes table created
   ├── internships table created
   └── password_reset_tokens table created
        ↓
DataLoader runs (CommandLineRunner)
   └── Seeds 8 sample internships if database is empty
        ↓
Application is ready and listening on port 8080
```

---

# 6. Database Documentation

## Database Technology

**H2 Database** — an in-memory Java database used for development. Data is lost when the application restarts. For production, the configuration should be swapped to PostgreSQL (comment in `application.properties` notes this).

**Connection:** `jdbc:h2:mem:internshipdb` (in-memory, named database)

**Schema management:** Hibernate's `ddl-auto=update` — automatically creates and modifies tables based on entity classes.

## Entities / Tables

### User (users table)

**Purpose:** Stores registered user accounts.

| Field | Type | Constraints | Purpose |
|-------|------|-------------|---------|
| `id` | Long | PK, auto-increment | Unique user identifier |
| `email` | String | Not null, unique | Login credential |
| `password` | String | Not null | BCrypt-hashed password |
| `full_name` | String | Not null | User's display name |
| `phone_number` | String | Not null, unique | Contact number |
| `college` | String | Not null | Educational institution |
| `department` | String | Nullable | Field of study |
| `graduation_year` | String | Nullable | Expected graduation |
| `profile_picture_url` | String | Nullable | Profile image URL |
| `role` | String | Not null, default "ROLE_USER" | User role (ROLE_USER or ROLE_ADMIN) |
| `enabled` | boolean | Not null, default true | Account active flag |
| `email_verified` | boolean | Not null, default false | Email verification status |
| `failed_login_attempts` | int | Not null, default 0 | Brute-force protection counter |
| `account_locked_until` | LocalDateTime | Nullable | Lock expiry time |
| `token_version` | int | Not null, default 0 | JWT invalidation counter |
| `created_at` | LocalDateTime | Auto-generated | Account creation time |
| `updated_at` | LocalDateTime | Auto-generated | Last modification time |

**Relationships:**
- One User → Many Resumes (OneToMany, cascade ALL, orphan removal)

**Where used:** AuthService, CustomUserDetailsService, JwtAuthenticationFilter, ResumeService

### Resume (resumes table)

**Purpose:** Stores uploaded resume files as binary data.

| Field | Type | Constraints | Purpose |
|-------|------|-------------|---------|
| `id` | Long | PK, auto-increment | Unique resume identifier |
| `file_name` | String | Not null | Generated internal filename |
| `original_file_name` | String | Not null | User's original filename |
| `content_type` | String | Not null | MIME type (application/pdf, etc.) |
| `file_size` | Long | Not null | File size in bytes |
| `file_data` | byte[] | Not null, @Lob | Actual file content stored as binary |
| `description` | String | Nullable | User-provided description |
| `active` | boolean | Not null, default false | Whether this is the active resume |
| `user_id` | Long | FK → users.id, Not null | Owner of this resume |
| `created_at` | LocalDateTime | Auto-generated | Upload time |
| `updated_at` | LocalDateTime | Auto-generated | Last modification |

**Relationships:**
- Many Resumes → One User (ManyToOne, LAZY fetch)

**Where used:** ResumeService, ResumeRepository, ResumeController, MlService

### Internship (internships table)

**Purpose:** Stores internship/job listings.

| Field | Type | Constraints | Purpose |
|-------|------|-------------|---------|
| `id` | Long | PK, auto-increment | Unique listing identifier |
| `title` | String | Not null | Job title |
| `company` | String | Not null | Company name |
| `description` | String | TEXT column, nullable | Detailed description |
| `application_link` | String | Not null | URL to apply |
| `created_at` | LocalDateTime | Auto-generated | Listing creation time |
| `updated_at` | LocalDateTime | Auto-generated | Last modification |

**Relationships:** None (standalone entity)

**Where used:** InternshipService, InternshipRepository, DataLoader, InternshipController

### PasswordResetToken (password_reset_tokens table)

**Purpose:** Stores password reset tokens with expiry and usage tracking.

| Field | Type | Constraints | Purpose |
|-------|------|-------------|---------|
| `id` | Long | PK, auto-increment | Unique token record ID |
| `token` | String | Not null, unique, indexed | The reset token (UUID) |
| `email` | String | Not null, indexed | Email this token is for |
| `expires_at` | LocalDateTime | Not null | Token expiry time |
| `used` | boolean | Not null, default false | Whether token has been used |
| `attempt_count` | int | Not null, default 0 | Failed attempt counter |
| `created_at` | LocalDateTime | Auto-generated | Token creation time |

**Relationships:** None (standalone entity)

**Where used:** AuthService, PasswordResetTokenRepository

### EmailVerificationToken (email_verification_tokens table)

**Purpose:** Stores email verification tokens with expiry and usage tracking.

| Field | Type | Constraints | Purpose |
|-------|------|-------------|---------|
| `id` | Long | PK, auto-increment | Unique token record ID |
| `token` | String | Not null, unique, indexed | The verification token (UUID) |
| `email` | String | Not null, indexed | Email this token is for |
| `expires_at` | LocalDateTime | Not null | Token expiry time (24 hours) |
| `used` | boolean | Not null, default false | Whether token has been used |
| `created_at` | LocalDateTime | Auto-generated | Token creation time |

**Relationships:** None (standalone entity)

**Where used:** AuthService, EmailVerificationTokenRepository

---

# 7. Authentication and Authorization

## Authentication Flow

```
User Sends Credentials (email + password)
        ↓
POST /api/auth/register or /api/auth/login
        ↓
AuthController receives request
        ↓
@Valid annotation triggers DTO validation
   ├── Required fields checked
   ├── Email format validated
   ├── Password strength validated (8+ chars, upper, lower, digit, special)
   └── Password confirmation matched
        ↓
AuthService processes request
   ├── For register:
   │   ├── Check email not already registered
   │   ├── Check phone number not already registered
   │   ├── Sanitize inputs (strip HTML, encode special chars)
   │   ├── Hash password with BCrypt
   │   ├── Save user to database
   │   ├── Log registration event with IP
   │   └── Generate JWT tokens
   ├── For login:
   │   ├── AuthenticationManager verifies credentials
   │   ├── Check if account is locked (failedAttempts >= 5)
   │   ├── On success: reset failed attempts, generate JWT tokens
   │   └── On failure: increment failed attempts, potentially lock account
        ↓
JWT Tokens Generated (access + refresh)
   ├── Access token: 1 hour expiry, contains email + tokenVersion
   ├── Refresh token: 7 days expiry, contains type="refresh" + tokenVersion
   └── Signed with HMAC-SHA using secret key
        ↓
Tokens Returned to Client
        ↓
Client Stores Tokens (localStorage, etc.)
        ↓
Client Sends Future Requests With:
   Authorization: Bearer <access-token>
        ↓
JwtAuthenticationFilter (runs on every request)
   ├── Extracts token from Authorization header
   ├── Validates signature and expiry
   ├── Extracts email and tokenVersion from claims
   ├── Looks up user in database
   ├── Verifies tokenVersion matches stored version
   ├── If valid: sets SecurityContext with user authorities
   └── If invalid: request proceeds without authentication (401 if protected)
        ↓
Controller Processes Request (or returns 401)
```

## Authorization Rules

**Public Endpoints (no token required):**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /api/auth/verify-email`
- `/swagger-ui/**`, `/v3/api-docs/**`
- `/actuator/health`
- `/h2-console/**` (only when `spring.h2.console.enabled=true`)

**Protected Endpoints (token required):**
- All other endpoints require a valid JWT token

**Admin-Only Endpoints (ROLE_ADMIN required):**
- `GET /api/admin/users`
- `PATCH /api/admin/users/{userId}/role`
- `POST /api/internships` (create)
- `PUT /api/internships/{id}` (update)
- `DELETE /api/internships/{id}` (delete)

**Role-Based Access:** Users are assigned `ROLE_USER` by default. Admin endpoints require `ROLE_ADMIN`. Role changes are admin-only operations enforced server-side.

## Password Handling

- **Hashing:** BCryptPasswordEncoder (irreversible)
- **Strength rules:** Minimum 8 characters, maximum 128, must contain at least one digit, one lowercase, one uppercase, and one special character (`@#$%^&+=!`)

## Token Versioning (Session Invalidation)

When a password is changed or reset, the `tokenVersion` field on the User entity is incremented. All existing JWT tokens contain the old version number. The `JwtAuthenticationFilter` compares the token's version with the stored version — if they don't match, the token is rejected.

```
File: security/JwtTokenProvider.java
Method: generateAccessTokenFromEmail(String email, int tokenVersion)
```

```java
return Jwts.builder()
        .subject(email)
        .issuedAt(now)
        .expiration(expiryDate)
        .claim("tokenVersion", tokenVersion)
        .signWith(getSigningKey())
        .compact();
```

---

# 8. COMPLETE API ENDPOINT DOCUMENTATION

## Endpoint Summary

| # | Method | Endpoint | Purpose | Auth Required |
|---|--------|----------|---------|---------------|
| 1 | POST | `/api/auth/register` | Create new user account | No |
| 2 | POST | `/api/auth/login` | Authenticate and get tokens | No |
| 3 | POST | `/api/auth/refresh` | Get new access token | No |
| 4 | GET | `/api/auth/me` | Get current user profile | Yes |
| 5 | POST | `/api/auth/change-password` | Change password (logged in) | Yes |
| 6 | POST | `/api/auth/forgot-password` | Request password reset email | No |
| 7 | POST | `/api/auth/reset-password` | Reset password with token | No |
| 8 | GET | `/api/auth/verify-email` | Verify email with token | No |
| 9 | POST | `/api/auth/resend-verification` | Resend verification email | Yes |
| 10 | GET | `/api/internships` | List all internships | Yes |
| 11 | GET | `/api/internships/{id}` | Get internship by ID | Yes |
| 12 | POST | `/api/internships` | Create new internship | Yes |
| 13 | POST | `/api/resumes/upload` | Upload resume file | Yes |
| 14 | GET | `/api/resumes` | List user's resumes | Yes |
| 15 | GET | `/api/resumes/active` | Get active resume | Yes |
| 16 | GET | `/api/resumes/{id}` | Get resume by ID | Yes |
| 17 | GET | `/api/resumes/{id}/download` | Download resume file | Yes |
| 18 | PUT | `/api/resumes/{id}/activate` | Activate a resume | Yes |
| 19 | DELETE | `/api/resumes/{id}` | Delete a resume | Yes |
| 20 | POST | `/api/users/me/profile-picture` | Upload/replace profile picture | Yes |
| 21 | GET | `/api/users/me/profile-picture` | Retrieve profile picture | Yes |
| 22 | DELETE | `/api/users/me/profile-picture` | Delete profile picture | Yes |
| 23 | GET | `/api/admin/users` | List all users | Yes + ROLE_ADMIN |
| 24 | PATCH | `/api/admin/users/{userId}/role` | Change user role | Yes + ROLE_ADMIN |
| 25 | GET | `/api/ml/health` | Check ML service availability | Yes |
| 26 | POST | `/api/ml/analyze` | Analyze resume text | Yes |
| 27 | POST | `/api/ml/match` | Match resume to jobs | Yes |

---

### Endpoint 1: Register

**Method:** `POST`
**Route:** `/api/auth/register`
**Authentication:** Not required

**What It Does:** Creates a new user account, validates all fields, hashes the password, stores the user, and returns JWT tokens immediately so the user is logged in after registration.

**Layman Explanation:** When a new student signs up, this endpoint creates their account, saves their information safely, and gives them a login token so they can start using the platform right away.

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `fullName` | String | Yes | 2-100 characters |
| `email` | String | Yes | Valid email format |
| `phoneNumber` | String | Yes | 10-15 digits, optional `+` prefix |
| `college` | String | Yes | Any value |
| `department` | String | No | Any value |
| `graduationYear` | String | No | Any value |
| `password` | String | Yes | 8-128 chars, upper+lower+digit+special |
| `confirmPassword` | String | Yes | Must match password |

**Backend Flow:**

```
Request
   ↓
AuthController.register(@Valid @RequestBody RegisterRequest)
   ↓
Input validation (@Valid triggers annotation checks)
   ↓
AuthService.register(RegisterRequest)
   ├── Password confirmation check
   ├── Duplicate email check (UserRepository.existsByEmail)
   ├── Duplicate phone check (UserRepository.existsByPhoneNumber)
   ├── Input sanitization (InputSanitizer.sanitize)
   ├── Password hashing (BCrypt)
   ├── User saved to database
   ├── Security event logged
   ├── AuthenticationManager.authenticate (generates auth context)
   └── JWT tokens generated
   ↓
Response: 201 Created + AuthResponse (tokens + user info)
```

**Files Involved:**
- `controller/AuthController.java` → `register()`
- `service/AuthService.java` → `register()`
- `repository/UserRepository.java` → `existsByEmail()`, `existsByPhoneNumber()`
- `security/InputSanitizer.java` → `sanitize()`, `trimOnly()`
- `security/SecurityEventLogger.java` → `logRegistration()`

**Success Response (201):**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "email": "test@example.com",
      "fullName": "Test User",
      "phoneNumber": "9876543210",
      "college": "IIT Delhi",
      "department": "CS",
      "graduationYear": "2027",
      "profilePictureUrl": null,
      "enabled": true,
      "emailVerified": false,
      "createdAt": "2026-08-28T10:30:00"
    }
  }
}
```

**Error Responses:**

- `400` — "Email is already registered" / "Phone number is already registered" / "Passwords do not match" / Validation errors
- `400` — Validation failed (missing fields, weak password, invalid email)

---

### Endpoint 2: Login

**Method:** `POST`
**Route:** `/api/auth/login`
**Authentication:** Not required

**What It Does:** Verifies user credentials, checks account lockout status, and returns JWT tokens.

**Layman Explanation:** When a student logs in with their email and password, this endpoint checks if the credentials are correct and gives them a security token.

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `email` | String | Yes |
| `password` | String | Yes |

**Backend Flow:**

```
Request
   ↓
AuthController.login(@Valid @RequestBody LoginRequest)
   ↓
AuthService.login(LoginRequest)
   ├── AuthenticationManager.authenticate
   │   ├── CustomUserDetailsService.loadUserByUsername
   │   │   └── Checks account lockout status
   │   └── BCrypt password comparison
   ├── On success:
   │   ├── Reset failed login attempts
   │   ├── Generate JWT tokens with tokenVersion
   │   └── Log successful login with IP
   └── On failure:
       ├── Increment failed login attempts
       ├── Lock account if >= 5 failures (30 min lockout)
       └── Log failure with IP
   ↓
Response: 200 OK + AuthResponse
```

**Files Involved:**
- `controller/AuthController.java` → `login()`
- `service/AuthService.java` → `login()`
- `security/CustomUserDetailsService.java` → `loadUserByUsername()`
- `security/JwtTokenProvider.java` → `generateAccessTokenFromEmail()`, `generateRefreshTokenFromEmail()`
- `security/SecurityEventLogger.java` → `logLoginSuccess()`, `logLoginFailure()`

**Success Response (200):** Same structure as register response.

**Error Responses:**
- `401` — "Invalid email or password" (always same message, prevents user enumeration)

---

### Endpoint 3: Refresh Token

**Method:** `POST`
**Route:** `/api/auth/refresh`
**Authentication:** Not required (but requires valid refresh token)

**What It Does:** Takes a valid refresh token and returns new access + refresh tokens. Validates token version to ensure the token hasn't been invalidated by a password change.

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `refreshToken` | String | Yes |

**Files Involved:**
- `controller/AuthController.java` → `refreshToken()`
- `service/AuthService.java` → `refreshTokens()`
- `security/JwtTokenProvider.java` → `validateToken()`, `isRefreshToken()`, `getEmailFromToken()`, `getTokenVersionFromToken()`

---

### Endpoint 4: Get Current User

**Method:** `GET`
**Route:** `/api/auth/me`
**Authentication:** Required

**What It Does:** Returns the profile information of the currently authenticated user.

**Headers:**
```
Authorization: Bearer <access-token>
```

**Files Involved:**
- `controller/AuthController.java` → `getCurrentUser()`
- `service/AuthService.java` → `getCurrentUser()`

---

### Endpoint 5: Change Password

**Method:** `POST`
**Route:** `/api/auth/change-password`
**Authentication:** Required

**What It Does:** Changes the password for the authenticated user. Requires current password verification. Increments `tokenVersion` to invalidate all existing tokens. Deletes all pending reset tokens.

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `currentPassword` | String | Yes | Must match current password |
| `newPassword` | String | Yes | 8-128 chars, strength rules |
| `confirmNewPassword` | String | Yes | Must match newPassword |

**Backend Flow:**

```
Request
   ↓
AuthController.changePassword(@AuthenticationPrincipal UserDetails, ChangePasswordRequest)
   ↓
AuthService.changePassword(ChangePasswordRequest, email)
   ├── Verify current password with BCrypt
   ├── Check new password is different from current
   ├── Validate confirmation matches
   ├── Hash new password
   ├── Increment tokenVersion (invalidates all existing tokens)
   ├── Delete all reset tokens for this user
   └── Log password change
   ↓
Response: 200 OK "Password changed successfully"
```

**Files Involved:**
- `controller/AuthController.java` → `changePassword()`
- `service/AuthService.java` → `changePassword()`
- `repository/PasswordResetTokenRepository.java` → `deleteAllByEmail()`

---

### Endpoint 6: Forgot Password

**Method:** `POST`
**Route:** `/api/auth/forgot-password`
**Authentication:** Not required

**What It Does:** Generates a password reset token for the given email. Always returns the same success message regardless of whether the email exists (prevents user enumeration). Token is rate-limited (3 per email per hour, 10 per IP per hour).

**Layman Explanation:** When a student forgets their password, they enter their email here. The system creates a reset token and (in production) would email it to them. For now, the token is logged to the console.

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `email` | String | Yes, valid email |

**Security Features:**
- Rate limiting via `RateLimiter.java`
- User enumeration prevention (same response for existing/non-existing emails)
- Token expires after 15 minutes
- Previous tokens deleted when new one is generated
- Audit logging of all attempts

**Response (always 200):**
```json
{
  "success": true,
  "message": "If the email exists, a reset link has been sent"
}
```

---

### Endpoint 7: Reset Password

**Method:** `POST`
**Route:** `/api/auth/reset-password`
**Authentication:** Not required

**What It Does:** Resets the password using a valid, unexpired, unused reset token. Increments `tokenVersion` to invalidate all existing tokens. Resets failed login attempts and account lockout.

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `token` | String | Yes | Valid UUID reset token |
| `newPassword` | String | Yes | 8-128 chars, strength rules |
| `confirmNewPassword` | String | Yes | Must match newPassword |

**Security Features:**
- Token expiry check (15 minutes)
- Token single-use enforcement
- Brute-force protection (locks after 5 failed attempts)
- Token version increment (invalidates all existing sessions)

**Files Involved:**
- `controller/AuthController.java` → `resetPassword()`
- `service/AuthService.java` → `resetPassword()`
- `repository/PasswordResetTokenRepository.java` → `findByToken()`

---

### Endpoint 8: Verify Email

**Method:** `GET`
**Route:** `/api/auth/verify-email?token=VERIFICATION_TOKEN`
**Authentication:** Not required

**What It Does:** Verifies a user's email address using a secure token. Sets `emailVerified = true` on the user and invalidates the token.

**Query Parameters:**
- `token` — The verification token (UUID) sent via email

**Backend Flow:**
```
Request
   ↓
AuthController.verifyEmail(@RequestParam token)
   ↓
AuthService.verifyEmail(token)
   ├── Find token in database
   ├── Validate token exists, not used, not expired
   ├── Find user by token email
   ├── Set user.emailVerified = true
   ├── Mark token as used
   └── Log verification event
   ↓
Response: 200 OK "Email verified successfully"
```

**Files Involved:**
- `controller/AuthController.java` → `verifyEmail()`
- `service/AuthService.java` → `verifyEmail()`
- `repository/EmailVerificationTokenRepository.java` → `findByToken()`

---

### Endpoint 9: Resend Verification

**Method:** `POST`
**Route:** `/api/auth/resend-verification`
**Authentication:** Required

**What It Does:** Generates a new email verification token and sends a verification email. Deletes any previous unused tokens for this user. Returns error if email is already verified.

**Backend Flow:**
```
Request
   ↓
AuthController.resendVerification(@AuthenticationPrincipal UserDetails)
   ↓
AuthService.resendVerification(email)
   ├── Check if email already verified → 400 if yes
   ├── Delete previous unused tokens
   ├── Generate new verification token (24-hour expiry)
   ├── Save token to database
   ├── Send verification email via EmailService
   └── Log verification email sent
   ↓
Response: 200 OK "Verification email sent"
```

**Files Involved:**
- `controller/AuthController.java` → `resendVerification()`
- `service/AuthService.java` → `resendVerification()`
- `service/EmailService.java` → `sendVerificationEmail()`

---

### Endpoint 10: Get All Internships

**Method:** `GET`
**Route:** `/api/internships`
**Authentication:** Required

**What It Does:** Returns all internship listings from the database.

**Files Involved:**
- `controller/InternshipController.java` → `getAllInternships()`
- `service/InternshipService.java` → `getAllInternships()`
- `repository/InternshipRepository.java` → `findAll()`

**Response (200):**
```json
{
  "success": true,
  "message": "Internships retrieved",
  "data": [
    {
      "id": 1,
      "title": "Software Engineering Intern",
      "company": "Google",
      "description": "Work on large-scale distributed systems...",
      "applicationLink": "https://careers.google.com/jobs/results/123456/",
      "createdAt": "2026-08-31T10:00:00",
      "updatedAt": "2026-08-31T10:00:00"
    }
  ]
}
```

---

### Endpoint 9: Get Internship by ID

**Method:** `GET`
**Route:** `/api/internships/{id}`
**Authentication:** Required

**Files Involved:**
- `controller/InternshipController.java` → `getInternshipById()`
- `service/InternshipService.java` → `getInternshipById()`
- `repository/InternshipRepository.java` → `findById()`

---

### Endpoint 10: Create Internship

**Method:** `POST`
**Route:** `/api/internships`
**Authentication:** Required

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `title` | String | Yes | 1-200 characters |
| `company` | String | Yes | 1-200 characters |
| `description` | String | No | Max 5000 characters |
| `applicationLink` | String | Yes | Any value |

**Files Involved:**
- `controller/InternshipController.java` → `createInternship()`
- `service/InternshipService.java` → `createInternship()`
- `repository/InternshipRepository.java` → `save()`

---

### Endpoint 11: Upload Resume

**Method:** `POST`
**Route:** `/api/resumes/upload`
**Authentication:** Required
**Content-Type:** `multipart/form-data`

**Form Data:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | PDF or Word document |
| `description` | String | No | Description text |

**Validation Rules:**
- File must not be empty
- Maximum file size: 10MB
- Allowed types: `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- Maximum 10 resumes per user
- First resume is automatically set as active

**Backend Flow:**

```
Request (multipart/form-data)
   ↓
ResumeController.uploadResume(@AuthenticationPrincipal, MultipartFile, String)
   ↓
ResumeService.uploadResume(email, file, description)
   ├── Find user by email
   ├── Validate file (empty, size, type)
   ├── Check resume count limit
   ├── Generate unique filename
   ├── Store file bytes in database
   ├── Log file upload event
   └── Return ResumeResponse
   ↓
Response: 201 Created + ResumeResponse
```

**Files Involved:**
- `controller/ResumeController.java` → `uploadResume()`
- `service/ResumeService.java` → `uploadResume()`, `validateFile()`
- `repository/ResumeRepository.java` → `save()`, `countByUserId()`

---

### Endpoint 12: Get User's Resumes

**Method:** `GET`
**Route:** `/api/resumes`
**Authentication:** Required

**Files Involved:**
- `controller/ResumeController.java` → `getUserResumes()`
- `service/ResumeService.java` → `getUserResumes()`
- `repository/ResumeRepository.java` → `findByUserIdOrderByCreatedAtDesc()`

---

### Endpoint 13: Get Active Resume

**Method:** `GET`
**Route:** `/api/resumes/active`
**Authentication:** Required

**What It Does:** Returns the resume currently marked as active for this user (used for job matching).

**Files Involved:**
- `controller/ResumeController.java` → `getActiveResume()`
- `service/ResumeService.java` → `getActiveResume()`

---

### Endpoint 14: Get Resume by ID

**Method:** `GET`
**Route:** `/api/resumes/{id}`
**Authentication:** Required

**Files Involved:**
- `controller/ResumeController.java` → `getResumeById()`
- `service/ResumeService.java` → `getResumeById()`
- `repository/ResumeRepository.java` → `findByIdAndUserId()`

---

### Endpoint 15: Download Resume

**Method:** `GET`
**Route:** `/api/resumes/{id}/download`
**Authentication:** Required

**What It Does:** Returns the raw resume file as binary data with appropriate headers for download.

**Response Headers:**
```
Content-Disposition: attachment; filename="original_resume.pdf"
Content-Type: application/pdf
Content-Length: 245760
```

**Files Involved:**
- `controller/ResumeController.java` → `downloadResume()`
- `service/ResumeService.java` → `downloadResume()`

---

### Endpoint 16: Activate Resume

**Method:** `PUT`
**Route:** `/api/resumes/{id}/activate`
**Authentication:** Required

**What It Does:** Sets the specified resume as the active one. Deactivates all other resumes for the same user.

**Backend Flow:**

```
Request
   ↓
ResumeController.activateResume(@AuthenticationPrincipal, Long id)
   ↓
ResumeService.activateResume(email, resumeId)
   ├── Find user
   ├── Find resume by ID + user ID
   ├── Deactivate all user's resumes (bulk update)
   ├── Activate selected resume
   └── Log activation
   ↓
Response: 200 OK + ResumeResponse
```

**Files Involved:**
- `controller/ResumeController.java` → `activateResume()`
- `service/ResumeService.java` → `activateResume()`
- `repository/ResumeRepository.java` → `deactivateAllByUserId()`, `activateResume()`

---

### Endpoint 17: Delete Resume

**Method:** `DELETE`
**Route:** `/api/resumes/{id}`
**Authentication:** Required

**Files Involved:**
- `controller/ResumeController.java` → `deleteResume()`
- `service/ResumeService.java` → `deleteResume()`
- `repository/ResumeRepository.java` → `findByIdAndUserId()`, `delete()`

---

### Endpoint 19: Upload/Replace Profile Picture

**Method:** `POST`
**Route:** `/api/users/me/profile-picture`
**Authentication:** Required
**Content-Type:** `multipart/form-data`

**Form Data:**
- `file` — Image file (JPEG, PNG, or WebP)

**Validation:**
- Max size: 5MB
- Allowed types: `image/jpeg`, `image/png`, `image/webp`
- File must not be empty

**Backend Flow:**
```
Request
   ↓
UserController.uploadProfilePicture(@AuthenticationPrincipal, MultipartFile)
   ↓
ProfilePictureService.uploadProfilePicture(email, file)
   ├── Find user
   ├── Validate file (empty, size, type)
   ├── Delete old picture if exists
   ├── Generate unique filename
   ├── Save file to uploads/profile-pictures/
   ├── Update user.profilePictureUrl
   └── Log file upload
   ↓
Response: 200 OK
```

**Files Involved:**
- `controller/UserController.java` → `uploadProfilePicture()`
- `service/ProfilePictureService.java` → `uploadProfilePicture()`

---

### Endpoint 20: Get Profile Picture

**Method:** `GET`
**Route:** `/api/users/me/profile-picture`
**Authentication:** Required

**What It Does:** Returns the user's profile picture as binary data with the correct Content-Type header. Returns 404 if no picture exists.

**Files Involved:**
- `controller/UserController.java` → `getProfilePicture()`
- `service/ProfilePictureService.java` → `getProfilePicture()`

---

### Endpoint 21: Delete Profile Picture

**Method:** `DELETE`
**Route:** `/api/users/me/profile-picture`
**Authentication:** Required

**What It Does:** Deletes the user's profile picture file and clears the `profilePictureUrl` field.

**Files Involved:**
- `controller/UserController.java` → `deleteProfilePicture()`
- `service/ProfilePictureService.java` → `deleteProfilePicture()`

---

### Endpoint 22: Admin — List Users

**Method:** `GET`
**Route:** `/api/admin/users`
**Authentication:** Required + ROLE_ADMIN

**What It Does:** Returns a list of all users with safe fields only (no passwords, no tokens, no sensitive data).

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "user@example.com",
      "fullName": "User Name",
      "role": "ROLE_USER",
      "emailVerified": true,
      "createdAt": "2026-08-28T10:00:00"
    }
  ]
}
```

**Files Involved:**
- `controller/AdminController.java` → `listUsers()`
- `repository/UserRepository.java` → `findAll()`

---

### Endpoint 23: Admin — Change User Role

**Method:** `PATCH`
**Route:** `/api/admin/users/{userId}/role`
**Authentication:** Required + ROLE_ADMIN

**Request Body:**
- `role` — Either `ROLE_USER` or `ROLE_ADMIN`

**What It Does:** Changes a user's role. Validates that the requested role is one of the allowed values. Logs the role change.

**Files Involved:**
- `controller/AdminController.java` → `changeUserRole()`
- `repository/UserRepository.java` → `findById()`, `save()`

---

### Endpoint 24: ML Health Check

**Method:** `GET`
**Route:** `/api/ml/health`
**Authentication:** Required

**What It Does:** Checks if the external Python ML service is running and available.

**Files Involved:**
- `controller/MlController.java` → `health()`
- `service/MlService.java` → `isAvailable()`

**Response:**
```json
{
  "success": true,
  "message": "ML service is available",
  "data": { "available": true }
}
```

---

### Endpoint 19: Analyze Resume (Text)

**Method:** `POST`
**Route:** `/api/ml/analyze`
**Authentication:** Required

**What It Does:** Sends resume text to the ML service for analysis. Returns extracted profile (skills, experience, education, etc.).

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `resumeText` | String | Yes |

**Files Involved:**
- `controller/MlController.java` → `analyzeResume()`
- `service/MlService.java` → `analyzeResume()`

---

### Endpoint 20: Match Jobs

**Method:** `POST`
**Route:** `/api/ml/match`
**Authentication:** Required
**Content-Type:** `multipart/form-data`

**Form Data:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `file` | File | Yes | — | Resume file |
| `topN` | Integer | No | 10 | Number of top matches to return |

**Files Involved:**
- `controller/MlController.java` → `matchJobs()`
- `service/MlService.java` → `matchJobs()`

**Response:**
```json
{
  "success": true,
  "message": "Jobs matched successfully",
  "data": {
    "profile": { "skills": ["Java", "React"], "experience_level": "mid" },
    "totalJobs": 42,
    "matches": [
      {
        "id": 101,
        "title": "Software Developer — Infosys",
        "score": 92,
        "reason": "Strong match: Java + Spring Boot",
        "fields": { "location": "Bangalore" }
      }
    ]
  }
}
```

---

# 9. Feature-Wise Documentation

## Feature 1: User Authentication

**What it does:** Handles user registration, login, token management, and profile retrieval.

**Endpoints:**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`

**Files:**
- `controller/AuthController.java`
- `service/AuthService.java`
- `repository/UserRepository.java`
- `entity/User.java`
- `security/JwtTokenProvider.java`
- `security/JwtAuthenticationFilter.java`
- `security/CustomUserDetailsService.java`

**Key Methods:**
- `AuthService.register()` — creates user, generates tokens
- `AuthService.login()` — verifies credentials, generates tokens
- `AuthService.refreshTokens()` — validates refresh token, returns new tokens
- `JwtTokenProvider.generateAccessTokenFromEmail()` — creates JWT with email + tokenVersion
- `JwtAuthenticationFilter.doFilterInternal()` — validates token on every request

## Feature 2: Password Management

**What it does:** Allows users to change passwords (while logged in) or reset forgotten passwords (via email token).

**Endpoints:**
- `POST /api/auth/change-password` (authenticated)
- `POST /api/auth/forgot-password` (public)
- `POST /api/auth/reset-password` (public)

**Files:**
- `service/AuthService.java`
- `entity/PasswordResetToken.java`
- `repository/PasswordResetTokenRepository.java`
- `security/RateLimiter.java`

**Key Methods:**
- `AuthService.changePassword()` — verifies current, sets new, invalidates tokens
- `AuthService.forgotPassword()` — generates reset token, rate limited
- `AuthService.resetPassword()` — validates token, sets new password, invalidates tokens

## Feature 3: Resume Management

**What it does:** Upload, list, download, activate, and delete resume files. Validates file type and size.

**Endpoints:**
- `POST /api/resumes/upload`
- `GET /api/resumes`
- `GET /api/resumes/active`
- `GET /api/resumes/{id}`
- `GET /api/resumes/{id}/download`
- `PUT /api/resumes/{id}/activate`
- `DELETE /api/resumes/{id}`

**Files:**
- `controller/ResumeController.java`
- `service/ResumeService.java`
- `repository/ResumeRepository.java`
- `entity/Resume.java`

**Key Methods:**
- `ResumeService.uploadResume()` — validates file, stores in DB
- `ResumeService.activateResume()` — deactivates all, activates one
- `ResumeService.validateFile()` — checks size, type, emptiness

## Feature 4: Email Verification

**What it does:** Verifies user email addresses via secure tokens sent through email. Tokens expire after 24 hours and are single-use.

**Endpoints:**
- `GET /api/auth/verify-email?token=...` (public)
- `POST /api/auth/resend-verification` (authenticated)

**Files:**
- `controller/AuthController.java` → `verifyEmail()`, `resendVerification()`
- `service/AuthService.java` → `verifyEmail()`, `resendVerification()`
- `service/EmailService.java` → `sendVerificationEmail()`
- `entity/EmailVerificationToken.java`
- `repository/EmailVerificationTokenRepository.java`

**Status:** IMPLEMENTED AND VERIFIED

## Feature 5: Profile Picture Management

**What it does:** Allows authenticated users to upload, replace, retrieve, and delete profile pictures. Stored on filesystem.

**Endpoints:**
- `POST /api/users/me/profile-picture` (upload/replace)
- `GET /api/users/me/profile-picture` (retrieve)
- `DELETE /api/users/me/profile-picture` (delete)

**Files:**
- `controller/UserController.java`
- `service/ProfilePictureService.java`

**Validation:** JPEG/PNG/WebP only, max 5MB

**Status:** IMPLEMENTED AND VERIFIED

## Feature 6: Role-Based Access Control

**What it does:** Enforces role-based authorization. Users get `ROLE_USER` by default. Admins get `ROLE_ADMIN` which grants access to admin-only endpoints.

**Endpoints:**
- `GET /api/admin/users` (ROLE_ADMIN only)
- `PATCH /api/admin/users/{userId}/role` (ROLE_ADMIN only)

**Files:**
- `controller/AdminController.java`
- `config/SecurityConfig.java` → `.requestMatchers("/api/admin/**").hasRole("ADMIN")`
- `security/CustomUserDetailsService.java` → loads role from database
- `entity/User.java` → `role` field

**Status:** IMPLEMENTED AND VERIFIED

## Feature 7: Password Reset Email Delivery

**What it does:** Sends actual password reset emails via SMTP instead of logging tokens to console.

**Endpoints:**
- `POST /api/auth/forgot-password` — sends reset email
- `POST /api/auth/reset-password` — resets with token

**Files:**
- `service/EmailService.java` → `sendPasswordResetEmail()`
- `service/AuthService.java` → `forgotPassword()`, `resetPassword()`

**Configuration required:**
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`

**Status:** IMPLEMENTED AND VERIFIED (requires SMTP server)

## Feature 8: Internship Management

**What it does:** CRUD operations for internship listings. Sample data is seeded on first startup.

**Endpoints:**
- `GET /api/internships`
- `GET /api/internships/{id}`
- `POST /api/internships`

**Files:**
- `controller/InternshipController.java`
- `service/InternshipService.java`
- `repository/InternshipRepository.java`
- `entity/Internship.java`
- `config/DataLoader.java` (seeds sample data)

## Feature 9: ML Integration

**What it does:** Proxies requests to an external Python ML service for resume analysis and job matching.

**Endpoints:**
- `GET /api/ml/health`
- `POST /api/ml/analyze`
- `POST /api/ml/match`

**Files:**
- `controller/MlController.java`
- `service/MlService.java`

**External Service:** Python FastAPI at `http://localhost:8000`
- `GET /health` — health check
- `POST /analyze-resume` — resume text analysis
- `POST /match-jobs-file` — file-based job matching

---

# 10. Request Lifecycle Examples

## Example 1: User Registration

```
Client (Postman/Frontend)
   ↓
POST http://localhost:8080/api/auth/register
Headers: Content-Type: application/json
Body: { "fullName": "Rahul", "email": "rahul@ex.com", ... }
   ↓
Spring Security Filter Chain
   ↓
SecurityConfig permits /api/auth/register (no auth needed)
   ↓
AuthController.register(@Valid @RequestBody RegisterRequest)
   ↓
@Valid triggers: @NotBlank, @Email, @Size, @Pattern checks
   ↓ (if validation passes)
AuthService.register(RegisterRequest)
   ├── InputSanitizer.sanitize(fullName) → strips HTML, encodes chars
   ├── InputSanitizer.trimOnly(email).toLowerCase() → "rahul@ex.com"
   ├── PasswordEncoder.encode(password) → "$2a$10$..."
   ├── UserRepository.save(user) → INSERT INTO users ...
   ├── SecurityEventLogger.logRegistration(email, ip)
   ├── AuthenticationManager.authenticate(...) → Spring Security auth
   └── JwtTokenProvider.generateAccessToken(auth) → "eyJhbG..."
   ↓
AuthResponse.of(accessToken, refreshToken, expiresIn, userResponse)
   ↓
ApiResponse.success("Registration successful", authResponse)
   ↓
HTTP 201 Created + JSON response
   ↓
Client receives tokens + user info
```

## Example 2: Resume Upload

```
Client
   ↓
POST http://localhost:8080/api/resumes/upload
Headers: Authorization: Bearer <token>, Content-Type: multipart/form-data
Body: file=resume.pdf, description="My resume"
   ↓
JwtAuthenticationFilter.doFilterInternal()
   ├── Extracts token from Authorization header
   ├── Validates JWT signature + expiry
   ├── Extracts email + tokenVersion
   ├── UserRepository.findByEmail → verifies tokenVersion matches
   └── Sets SecurityContext
   ↓
ResumeController.uploadResume(@AuthenticationPrincipal UserDetails, MultipartFile, String)
   ↓
ResumeService.uploadResume(email, file, description)
   ├── UserRepository.findByEmail → finds user
   ├── validateFile(file)
   │   ├── file.isEmpty() check
   │   ├── file.getSize() > 10MB check
   │   └── ALLOWED_TYPES.contains(contentType) check
   ├── ResumeRepository.countByUserId → max 10 resumes check
   ├── Resume.builder() → creates entity
   ├── ResumeRepository.save(resume) → INSERT INTO resumes ...
   └── SecurityEventLogger.logFileUpload(...)
   ↓
ResumeResponse.builder() → maps entity to DTO
   ↓
ApiResponse.success("Resume uploaded successfully", response)
   ↓
HTTP 201 Created + JSON response
```

---

# 11. Security Deep Dive

## SecurityConfig.java

**File:** `config/SecurityConfig.java`
**Purpose:** Configures the entire Spring Security filter chain.

**Key Configuration:**
- **CSRF:** Disabled (stateless JWT API, no session cookies)
- **CORS:** Whitelisted origins: `localhost:3000`, `localhost:5173`; whitelisted headers: `Authorization`, `Content-Type`, `X-Requested-With`, `Accept`, `Origin`, `Cache-Control`
- **Sessions:** Stateless (no HTTP sessions; JWT tokens used instead)
- **Security Headers:** HSTS (1 year, includeSubDomains, preload), Content-Security-Policy, Referrer-Policy, Permissions-Policy
- **H2 Console:** Conditionally enabled (only when `spring.h2.console.enabled=true`)

**Public Endpoints:**
```java
.requestMatchers(
    "/api/auth/register",
    "/api/auth/login",
    "/api/auth/refresh",
    "/api/auth/forgot-password",
    "/api/auth/reset-password",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/actuator/health"
).permitAll()
```

## JwtAuthenticationFilter.java

**File:** `security/JwtAuthenticationFilter.java`
**Purpose:** Runs on every HTTP request. Extracts JWT from the `Authorization: Bearer <token>` header, validates it, and sets the security context.

**When it runs:** Before every controller method.

**Key behavior:**
1. Extracts token from `Authorization` header
2. Validates signature and expiry via `JwtTokenProvider`
3. Extracts `email` and `tokenVersion` from JWT claims
4. Looks up user in database
5. Compares `tokenVersion` in token with stored version
6. If match: creates `UsernamePasswordAuthenticationToken` and sets it in `SecurityContextHolder`
7. If no match or invalid: request proceeds without authentication

## JwtTokenProvider.java

**File:** `security/JwtTokenProvider.java`
**Purpose:** Creates and validates JWT tokens.

**Key Methods:**
- `generateAccessTokenFromEmail(email, tokenVersion)` — creates access token (1 hour expiry)
- `generateRefreshTokenFromEmail(email, tokenVersion)` — creates refresh token (7 days expiry, `type=refresh` claim)
- `validateToken(token)` — checks signature, expiry, format
- `getEmailFromToken(token)` — extracts subject (email)
- `getTokenVersionFromToken(token)` — extracts `tokenVersion` claim
- `isRefreshToken(token)` — checks if `type` claim equals `"refresh"`

**Token Claims:**
```json
{
  "sub": "user@email.com",
  "iat": 1724832000,
  "exp": 1724868000,
  "tokenVersion": 0,
  "type": "refresh"  // only on refresh tokens
}
```

## Password Hashing

**Mechanism:** BCryptPasswordEncoder

```java
// In SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Usage in AuthService:**
```java
// Hashing (registration, password change)
user.setPassword(passwordEncoder.encode(request.getPassword()));

// Verification (login, password change)
passwordEncoder.matches(request.getPassword(), user.getPassword());
```

## Input Sanitization

**File:** `security/InputSanitizer.java`
**Purpose:** Prevents stored XSS by stripping HTML tags and encoding special characters.

**Two methods:**
- `sanitize(input)` — removes HTML tags, encodes `& < > " '`, trims whitespace
- `trimOnly(input)` — only trims whitespace (for emails, phone numbers)

**Applied to:** User registration inputs (fullName, email, phoneNumber, college, department, graduationYear)

## Rate Limiting

**File:** `security/RateLimiter.java`
**Purpose:** In-memory rate limiter for password reset requests.

**Limits:**
- 3 requests per email per hour
- 10 requests per IP per hour

**Storage:** `ConcurrentHashMap` with `AtomicInteger` counters

**Applied to:** `POST /api/auth/forgot-password`

## Account Lockout

**Implementation in:** `service/AuthService.java`

**Rules:**
- After 5 failed login attempts → account locked for 30 minutes
- On successful login → failed attempts reset to 0
- Lockout check in `CustomUserDetailsService.loadUserByUsername()`

**Fields on User entity:**
- `failedLoginAttempts` (int, default 0)
- `accountLockedUntil` (LocalDateTime, nullable)

## Role-Based Access Control (RBAC)

**Implementation in:** `config/SecurityConfig.java`, `security/CustomUserDetailsService.java`, `entity/User.java`

**How it works:**
1. User entity has a `role` field (default: `ROLE_USER`)
2. `CustomUserDetailsService` loads the role from the database and creates `SimpleGrantedAuthority(role)`
3. `SecurityConfig` enforces admin access: `.requestMatchers("/api/admin/**").hasRole("ADMIN")`
4. Public registration always assigns `ROLE_USER` server-side (cannot be self-assigned)

**Admin-only endpoints:**
- `GET /api/admin/users` — list all users
- `PATCH /api/admin/users/{userId}/role` — change user role

**Authorization enforcement:**
- `ROLE_USER` → 403 Forbidden on `/api/admin/**`
- `ROLE_ADMIN` → Allowed on `/api/admin/**`

## Security Event Logging

**File:** `security/SecurityEventLogger.java`
**Purpose:** Centralized audit trail for security-relevant events.

**Events logged:**
- `LOGIN_SUCCESS` — successful login
- `LOGIN_FAILURE` — failed login attempt
- `ACCOUNT_LOCKED` — account locked due to too many failures
- `ACCOUNT_UNLOCKED` — account unlocked
- `REGISTRATION` — new user registered
- `TOKEN_REFRESH` — tokens refreshed
- `FILE_UPLOAD` — resume uploaded
- `UNAUTHORIZED_ACCESS` — unauthorized access attempt
- `USER_ENUMERATION_ATTEMPT` — potential user enumeration
- `PASSWORD_CHANGE` — password changed
- `PASSWORD_RESET_REQUEST` — reset token requested
- `PASSWORD_RESET` — password reset completed
- `PASSWORD_RESET_RATE_LIMITED` — rate limited

**Format:** `[SECURITY] EVENT_TYPE | time=... | user=... | ip=...`

---

# 12. Validation and Error Handling

## Request Validation

Validation is performed using Jakarta Bean Validation annotations on DTO classes. When a controller method receives a `@Valid @RequestBody`, Spring automatically validates the input.

**Common annotations used:**
- `@NotBlank` — field must not be null, empty, or whitespace
- `@Email` — must be valid email format
- `@Size(min, max)` — string length constraints
- `@Pattern(regexp)` — regex pattern matching

## Global Exception Handler

**File:** `exception/GlobalExceptionHandler.java`
**Purpose:** Catches all exceptions and returns consistent JSON error responses.

| Exception | HTTP Status | Response Message |
|-----------|-------------|-----------------|
| `MethodArgumentNotValidException` | 400 Bad Request | "Validation failed" + field errors |
| `IllegalArgumentException` | 400 Bad Request | Exception message |
| `BadCredentialsException` | 401 Unauthorized | "Invalid email or password" |
| `MaxUploadSizeExceededException` | 400 Bad Request | "File size exceeds maximum limit of 10MB" |
| `RuntimeException` | 500 Internal Server Error | Exception message |
| `Exception` | 500 Internal Server Error | "An unexpected error occurred" |

## Standard Response Format

**File:** `dto/ApiResponse.java`
**Purpose:** All API responses use a consistent wrapper structure.

```json
{
  "success": true/false,
  "message": "Description of what happened",
  "data": { ... }   // only present on success when data exists
}
```

---

# 13. Important Dependencies

| Dependency | Purpose | Where It Used |
|------------|---------|---------------|
| `spring-boot-starter-web` | REST API framework, HTTP handling | All controllers, MlService |
| `spring-boot-starter-security` | Authentication, authorization, security filters | SecurityConfig, all auth code |
| `spring-boot-starter-data-jpa` | Database ORM, repository pattern | All repositories, entities |
| `spring-boot-starter-validation` | Bean validation (@Valid, @NotBlank, etc.) | All DTOs, controllers |
| `h2` | In-memory database | Database configuration |
| `jjwt-api` (0.12.5) | JWT token creation and parsing | JwtTokenProvider |
| `jjwt-impl` (0.12.5) | JWT implementation | JwtTokenProvider |
| `jjwt-jackson` (0.12.5) | JWT JSON serialization | JwtTokenProvider |
| `lombok` | Reduces boilerplate code | All Java classes |
| `spring-boot-starter-mail` | SMTP email sending | EmailService |
| `spring-boot-starter-test` | Testing framework, MockMvc | AuthControllerTest, ResumeControllerTest |

---

# 14. Configuration and Environment Variables

## application.properties

```properties
# Server
server.port=8080
server.tomcat.max-http-form-post-size=10MB

# Database
spring.datasource.url=jdbc:h2:mem:internshipdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# H2 Console (disabled by default)
spring.h2.console.enabled=false
spring.h2.console.path=/h2-console

# JWT
jwt.secret=<hidden>    # Base64-encoded HMAC-SHA secret
jwt.expiration=86400000          # Access token: 24 hours
jwt.refresh-expiration=604800000 # Refresh token: 7 days
jwt.token-prefix=Bearer

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# ML Service
ml.service.url=http://localhost:8000

# Profile Picture Upload
app.upload.dir=uploads/profile-pictures
app.upload.max-size=5242880
app.upload.allowed-types=image/jpeg,image/png,image/webp

# Email Configuration
spring.mail.host=${MAIL_HOST:localhost}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS:true}

# Application URLs
app.frontend.url=${FRONTEND_URL:http://localhost:3000}
app.mail.from=${MAIL_FROM:noreply@internshipplatform.com}

# HTTPS (commented out for production)
# server.ssl.enabled=true
# server.ssl.key-store=classpath:keystore.p12
# server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
```

## application-dev.properties

```properties
# Development profile overrides
spring.h2.console.enabled=true
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Note:** Never expose the JWT secret, database credentials, or API keys. Use environment variables or secret management in production.

---

# 15. If the Evaluator Asks...

## How does authentication work?

**Quick Answer:** Users register or login to get JWT tokens. Every protected request includes the token in the `Authorization: Bearer` header. A filter on every request validates the token and identifies the user.

**Technical Explanation:** The backend uses stateless JWT authentication. On login, `JwtTokenProvider` creates a signed token containing the user's email and a token version number. On subsequent requests, `JwtAuthenticationFilter` extracts and validates the token, checks the token version matches the database, and sets the authentication context.

**Where in the Code:**
- Token generation: `security/JwtTokenProvider.java` → `generateAccessTokenFromEmail()`
- Token validation: `security/JwtTokenProvider.java` → `validateToken()`
- Filter: `security/JwtAuthenticationFilter.java` → `doFilterInternal()`

## Where are passwords stored?

**Quick Answer:** Passwords are never stored in plain text. They are hashed using BCrypt before being saved to the database. The original password cannot be recovered.

**Where in the Code:**
- `service/AuthService.java` → `passwordEncoder.encode(request.getPassword())`
- `config/SecurityConfig.java` → `new BCryptPasswordEncoder()`

## How does the frontend communicate with the backend?

**Quick Answer:** The frontend sends HTTP requests (GET, POST, PUT, DELETE) to `http://localhost:8080/api/...`. Protected endpoints require an `Authorization: Bearer <token>` header. The backend returns JSON responses wrapped in a standard `ApiResponse` format.

## How are private APIs protected?

**Quick Answer:** Spring Security's filter chain checks every request. If a request hits a protected endpoint without a valid JWT token, it returns a 401 "Authentication required" response. The JWT filter runs before any controller method.

**Where in the Code:** `config/SecurityConfig.java` → `authorizeHttpRequests()`

## Why does the backend use a service layer?

**Quick Answer:** The service layer separates business logic from HTTP handling. Controllers handle request/response format; services handle rules, validation, and coordination. This makes code easier to test, maintain, and reuse.

## Why are DTOs used instead of returning entities directly?

**Quick Answer:** DTOs control exactly what data is sent to the client. They prevent exposing sensitive fields (like passwords), prevent circular references in JSON, and decouple the API contract from the database schema.

## How does password reset work?

**Quick Answer:** User requests a reset → a UUID token is generated with 15-minute expiry → token is stored in database → (in production) emailed to user → user submits token + new password → token is validated and password is updated → all existing sessions are invalidated.

**Where in the Code:** `service/AuthService.java` → `forgotPassword()`, `resetPassword()`

---

# 16. Crucial Code Map

| Feature | File Path | Class / Method | Why It Matters |
|---------|-----------|----------------|---------------|
| User Registration | `service/AuthService.java` | `register()` | Creates new accounts with hashed passwords |
| User Login | `service/AuthService.java` | `login()` | Verifies credentials, handles lockout |
| Token Generation | `security/JwtTokenProvider.java` | `generateAccessTokenFromEmail()` | Creates JWT with email + tokenVersion |
| Token Validation | `security/JwtTokenProvider.java` | `validateToken()` | Checks JWT signature and expiry |
| Token Filter | `security/JwtAuthenticationFilter.java` | `doFilterInternal()` | Runs on every request, validates JWT |
| Password Hashing | `config/SecurityConfig.java` | `passwordEncoder()` | BCrypt encoder bean |
| Account Lockout | `service/AuthService.java` | `login()` | Locks after 5 failed attempts |
| Token Versioning | `service/AuthService.java` | `changePassword()` | Increments tokenVersion to invalidate tokens |
| Password Reset | `service/AuthService.java` | `forgotPassword()` | Generates time-limited reset token |
| Rate Limiting | `security/RateLimiter.java` | `isAllowed()` | Limits password reset requests |
| Input Sanitization | `security/InputSanitizer.java` | `sanitize()` | Strips HTML to prevent XSS |
| Security Logging | `security/SecurityEventLogger.java` | `logLoginSuccess()` etc. | Audit trail for security events |
| Resume Upload | `service/ResumeService.java` | `uploadResume()` | Validates and stores resume files |
| Resume Activation | `service/ResumeService.java` | `activateResume()` | Sets one resume as active |
| File Validation | `service/ResumeService.java` | `validateFile()` | Checks size and type |
| ML Proxy | `service/MlService.java` | `analyzeResume()` | Forwards to Python ML service |
| Exception Handling | `exception/GlobalExceptionHandler.java` | `handleValidationExceptions()` | Catches all errors, returns JSON |
| Security Config | `config/SecurityConfig.java` | `securityFilterChain()` | Defines all security rules |
| CORS Config | `config/SecurityConfig.java` | `corsConfigurationSource()` | Whitelists origins and headers |
| Data Seeding | `config/DataLoader.java` | `run()` | Seeds 8 sample internships |
| Email Verification | `service/AuthService.java` | `verifyEmail()` | Verifies email with token |
| Email Sending | `service/EmailService.java` | `sendVerificationEmail()` | Sends SMTP emails |
| Profile Picture Upload | `service/ProfilePictureService.java` | `uploadProfilePicture()` | Stores profile images |
| Admin User List | `controller/AdminController.java` | `listUsers()` | Lists all users (admin only) |
| Admin Role Change | `controller/AdminController.java` | `changeUserRole()` | Changes user role |
| RBAC Enforcement | `config/SecurityConfig.java` | `hasRole("ADMIN")` | Blocks non-admin access |

---

# 17. Feature → File → Code Mapping

```
FEATURE: User Registration

1. controller/AuthController.java
   → Receives HTTP request
   → AuthController.register()

2. service/AuthService.java
   → Business logic
   → AuthService.register()
   → InputSanitizer.sanitize(), passwordEncoder.encode()

3. repository/UserRepository.java
   → Database save
   → existsByEmail(), existsByPhoneNumber()

4. entity/User.java
   → Table definition
   → User.builder()...build()

5. security/JwtTokenProvider.java
   → Token generation
   → generateAccessToken(), generateRefreshToken()

FEATURE: Resume Upload

1. controller/ResumeController.java
   → Receives multipart request
   → ResumeController.uploadResume()

2. service/ResumeService.java
   → File validation and storage
   → ResumeService.uploadResume(), validateFile()

3. repository/ResumeRepository.java
   → Database save
   → save(), countByUserId()

4. entity/Resume.java
   → Table definition (includes @Lob byte[] fileData)

FEATURE: Password Reset

1. controller/AuthController.java
   → Forgot + Reset endpoints
   → forgotPassword(), resetPassword()

2. service/AuthService.java
   → Token generation + validation
   → forgotPassword(), resetPassword()

3. entity/PasswordResetToken.java
   → Token table (with expiry, used flag, attempt count)

4. repository/PasswordResetTokenRepository.java
   → Token CRUD
   → findByToken(), deleteAllByEmail()

5. security/RateLimiter.java
   → Rate limiting
   → isAllowed()

FEATURE: ML Integration

1. controller/MlController.java
   → ML endpoints
   → health(), analyzeResume(), matchJobs()

2. service/MlService.java
   → HTTP calls to Python service
   → isAvailable(), analyzeResume(), matchJobs()
```

---

# 18. API Testing Guide

## Using Postman

The repository includes a Postman collection at `postman/Internship_Platform_API.postman_collection.json`.

**Setup:**
1. Import the collection into Postman
2. Run the "Login" request first — tokens are auto-saved via collection variables
3. All other endpoints automatically use the saved token

## Using cURL

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "phoneNumber": "9876543210",
    "college": "IIT Delhi",
    "password": "Pass@1234",
    "confirmPassword": "Pass@1234"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "Pass@1234"}'
```

### Upload Resume
```bash
curl -X POST http://localhost:8080/api/resumes/upload \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -F "file=@./my_resume.pdf" \
  -F "description=My resume"
```

### Get Internships
```bash
curl -X GET http://localhost:8080/api/internships \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### Change Password
```bash
curl -X POST http://localhost:8080/api/auth/change-password \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Pass@1234",
    "newPassword": "NewPass@1234",
    "confirmNewPassword": "NewPass@1234"
  }'
```

### Forgot Password
```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

---

# 19. Common Questions and Troubleshooting

## Authentication Failures (401)

**Cause:** Token is missing, expired, or invalid.

**Backend handling:**
- `JwtAuthenticationEntryPoint` returns: `{"success": false, "message": "Authentication required"}`
- `GlobalExceptionHandler` catches `BadCredentialsException` and returns: `{"success": false, "message": "Invalid email or password"}`

**Relevant files:**
- `security/JwtAuthenticationEntryPoint.java`
- `exception/GlobalExceptionHandler.java`

## Account Locked

**Cause:** Too many failed login attempts (>= 5).

**Backend handling:** `CustomUserDetailsService.loadUserByUsername()` checks `accountLockedUntil` and returns locked status. Spring Security throws `LockedException`.

**Relevant files:**
- `security/CustomUserDetailsService.java`
- `service/AuthService.java` (login method)

## Token Version Mismatch

**Cause:** Password was changed or reset after the token was issued.

**Backend handling:** `JwtAuthenticationFilter` compares token's `tokenVersion` with stored version. Mismatch → request proceeds without authentication → 401 if endpoint requires auth.

**Relevant files:**
- `security/JwtAuthenticationFilter.java`
- `security/JwtTokenProvider.java` (`getTokenVersionFromToken()`)

## CORS Errors

**Cause:** Frontend is not running on `localhost:3000` or `localhost:5173`.

**Backend handling:** `SecurityConfig.corsConfigurationSource()` whitelists only these two origins.

**Relevant file:** `config/SecurityConfig.java`

## File Upload Failures

**Causes:**
- File exceeds 10MB limit
- File type is not PDF or Word
- User already has 10 resumes

**Backend handling:** `ResumeService.validateFile()` checks all conditions. `GlobalExceptionHandler` catches `MaxUploadSizeExceededException`.

**Relevant files:**
- `service/ResumeService.java`
- `exception/GlobalExceptionHandler.java`

---

# 20. Final Backend Summary

```
Client Sends Request
        ↓
Spring Security Filter Chain
   ├── JwtAuthenticationFilter validates JWT (if present)
   ├── Token version verified against database
   └── Authentication context set if valid
        ↓
SecurityConfig checks endpoint access rules
   ├── Public endpoints → proceed without auth
   └── Protected endpoints → require valid authentication
        ↓
Controller Receives Request
   ├── @Valid annotation triggers input validation
   └── @AuthenticationPrincipal identifies current user
        ↓
Validation Is Applied (DTO annotations)
   ├── Required fields checked
   ├── Email format validated
   ├── Password strength validated
   └── Custom business rules checked
        ↓
Service Performs Business Logic
   ├── Calls Repository for database operations
   ├── Applies business rules and constraints
   ├── Calls SecurityEventLogger for audit
   └── Manages transactions
        ↓
Repository Communicates With Database
   ├── Auto-generated CRUD operations
   └── Custom JPQL queries
        ↓
Database Returns Results
        ↓
Result Travels Back Through Layers
   ├── Service maps entity to DTO
   ├── Controller wraps in ApiResponse
   └── GlobalExceptionHandler catches any errors
        ↓
Client Receives JSON Response
```

**All 27 API endpoints are verified against the actual codebase.**

**Backend functionality was not modified during documentation creation.**

**No secrets or credentials are exposed in this document.**

---

*Last updated: September 2, 2026*
*All 27 endpoints verified. 29/29 automated tests passing.*
*Based on actual codebase implementation — not assumptions or plans.*

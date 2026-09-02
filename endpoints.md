#  API Endpoints Reference — Internship Platform SIH

> Yeh document saare backend endpoints ka detailed guide hai. 

---

## 📌 General Info

| Cheez | Detail |
|---|---|
| **Base URL** | `http://localhost:8080` |
| **Auth Type** | JWT Bearer Token |
| **Response Format** | Sabhi responses ek standard `ApiResponse` wrapper mein aate hain |
| **CORS** | `localhost:3000` aur `localhost:5173` se requests allowed hain |

### Standard Response Format

```json
{
  "success": true,
  "message": "Kaam ho gaya bhai",
  "data": { ... }
}
```

Error ka format:
```json
{
  "success": false,
  "message": "Kuch gadbad ho gayi"
}
```

### 🔑 Authentication Kaise Kaam Karta Hai

1. Pehle **register** ya **login** karo
2. Login ke baad `accessToken` aur `refreshToken` milega
3. Baaki sab endpoints mein `Authorization` header mein token bhejo:
   ```
   Authorization: Bearer <your-access-token>
   ```
4. Token expire ho gaya? — **refresh endpoint** se naya token lo
5. Password ka rule: min 8 chars, ek digit, ek lowercase, ek uppercase, ek special char (`@#$%^&+=!`)

---

## 1. 🔐 Auth Endpoints

Base path: `/api/auth`

Yeh endpoints **public** hain — inhein login/register ki zaroorat nahi.

---

### 1.1 Register (Naya User Banana)

```
POST /api/auth/register
```

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `fullName` | String | ✅ | 2-100 characters |
| `email` | String | ✅ | Valid email hona chahiye |
| `phoneNumber` | String | ✅ | 10-15 digits, `+` optional |
| `college` | String | ✅ | — |
| `department` | String | ❌ | — |
| `graduationYear` | String | ❌ | — |
| `password` | String | ✅ | 8-128 chars, uppercase+lowercase+digit+special char |
| `confirmPassword` | String | ✅ | password se match hona chahiye |

**Example Request:**
```json
{
  "fullName": "Rahul Sharma",
  "email": "rahul@example.com",
  "phoneNumber": "+919876543210",
  "college": "IIT Delhi",
  "department": "Computer Science",
  "graduationYear": "2025",
  "password": "Rahul@123",
  "confirmPassword": "Rahul@123"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": {
      "id": 1,
      "email": "rahul@example.com",
      "fullName": "Rahul Sharma",
      "phoneNumber": "+919876543210",
      "college": "IIT Delhi",
      "department": "Computer Science",
      "graduationYear": "2025",
      "enabled": true,
      "emailVerified": false
    }
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Email already registered"
}
```

---

### 1.2 Login

```
POST /api/auth/login
```

**Request Body:**

| Field | Type | Required |
|---|---|---|
| `email` | String | ✅ |
| `password` | String | ✅ |

**Example Request:**
```json
{
  "email": "rahul@example.com",
  "password": "Rahul@123"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": {
      "id": 1,
      "email": "rahul@example.com",
      "fullName": "Rahul Sharma"
    }
  }
}
```

---

### 1.3 Token Refresh

```
POST /api/auth/refresh
```

Jab access token expire ho jaye, is endpoint se naya token lo. Refresh token zyada time tak valid rehta hai.

**Request Body:**

| Field | Type | Required |
|---|---|---|
| `refreshToken` | String | ✅ |

**Example Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9... (naya wala)",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9... (naya wala)",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": { ... }
  }
}
```

---

### 1.4 Current User Ki Info

```
GET /api/auth/me
```

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User profile retrieved",
  "data": {
    "id": 1,
    "email": "rahul@example.com",
    "fullName": "Rahul Sharma",
    "phoneNumber": "+919876543210",
    "college": "IIT Delhi",
    "department": "Computer Science",
    "graduationYear": "2025",
    "profilePictureUrl": null,
    "enabled": true,
    "emailVerified": false,
    "createdAt": "2026-08-28T10:30:00"
  }
}
```

**Error (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Authentication required"
}
```

---

### 1.5 Change Password

```
POST /api/auth/change-password
```

Logged-in user apna password change kar sakta hai. Current password verify hota hai, aur saare purane tokens invalidate ho jaate hain.

**Headers:**
```
Authorization: Bearer <access-token>
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `currentPassword` | String | ✅ | Current password match karna chahiye |
| `newPassword` | String | ✅ | 8-128 chars, uppercase+lowercase+digit+special char |
| `confirmNewPassword` | String | ✅ | newPassword se match hona chahiye |

**Example Request:**
```json
{
  "currentPassword": "Rahul@123",
  "newPassword": "Rahul@456",
  "confirmNewPassword": "Rahul@456"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `401` — "Invalid email or password" (current password galat hai)
- `400` — "Passwords do not match" (new aur confirm match nahi kar rahe)
- `400` — "New password must be different from current password"
- `400` — Validation failed (kamzor password)

**Note:** Password change ke baad saare purane JWT tokens invalidate ho jaate hain — user ko dobara login karna padega.

---

### 1.6 Forgot Password

```
POST /api/auth/forgot-password
```

Password bhool gaye? Is endpoint pe email bhejo — system ek reset token generate karega. **Hamesha same response aata hai** chahe email exist kare ya nahi (user enumeration prevent karta hai).

**Headers:**
```
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required |
|---|---|---|
| `email` | String | ✅, valid email |

**Example Request:**
```json
{
  "email": "rahul@example.com"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "If the email exists, a reset link has been sent"
}
```

**Security Features:**
- Rate limiting: 3 requests per email per hour, 10 per IP per hour
- Token expires in 15 minutes
- Previous tokens deleted when new one is generated
- Response same hota hai email exist kare ya nahi

**Note:** Token email se bheja jaata hai (SMTP configuration required).

---

### 1.7 Reset Password

```
POST /api/auth/reset-password
```

Reset token ke saath naya password set karo. Token valid hona chahiye, expire nahi hona chahiye, aur use nahi hona chahiye.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `token` | String | ✅ | Valid UUID reset token |
| `newPassword` | String | ✅ | 8-128 chars, strength rules |
| `confirmNewPassword` | String | ✅ | newPassword se match |

**Example Request:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NewRahul@123",
  "confirmNewPassword": "NewRahul@123"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

**Error Responses:**
- `400` — "Invalid or expired reset token"
- `400` — "Passwords do not match"
- `400` — "Reset token has been locked due to too many failed attempts" (5 galat attempts ke baad)

**Security Features:**
- Token single-use hai — use ke baad delete ho jaata hai
- 5 galat attempts ke baad token lock ho jaata hai
- Password reset ke baad saare purane tokens invalidate hote hain
- Failed login attempts aur account lockout reset ho jaata hai

---

### 1.8 Verify Email

```
GET /api/auth/verify-email?token=VERIFICATION_TOKEN
```

Register ke baad email verify karo. Token email pe bheja jaata hai.

**Query Parameters:**

| Param | Type | Required |
|---|---|---|
| `token` | String | ✅ | UUID verification token |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Email verified successfully"
}
```

**Error Responses:**
- `400` — "Invalid or expired verification token"
- `400` — "Invalid verification token"

**Security Features:**
- Token 24 hours mein expire hota hai
- Token single-use hai
- Password reset token se verification nahi ho sakta

---

### 1.9 Resend Verification Email

```
POST /api/auth/resend-verification
```

Logged-in user verification email dobara bhej sakta hai.

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Verification email sent"
}
```

**Error Response:**
- `400` — "Email is already verified"
- `401` — "Authentication required"

---

## 2. 📄 Resume Endpoints

Base path: `/api/resumes`

Yeh sabhi endpoints **authenticated** hain — JWT token zaroori hai.

---

### 2.1 Resume Upload Karna

```
POST /api/resumes/upload
```

**Headers:**
```
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

**Form Data:**

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | File | ✅ | Resume ka PDF/DOC file |
| `description` | String | ❌ | Resume ke baare mein kuch likh do |

**curl Example:**
```bash
curl -X POST http://localhost:8080/api/resumes/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@rahul_resume.pdf" \
  -F "description=Frontend developer resume"
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Resume uploaded successfully",
  "data": {
    "id": 1,
    "fileName": "a1b2c3d4-rahul_resume.pdf",
    "originalFileName": "rahul_resume.pdf",
    "contentType": "application/pdf",
    "fileSize": 245760,
    "description": "Frontend developer resume",
    "active": false,
    "createdAt": "2026-08-28T10:35:00",
    "updatedAt": "2026-08-28T10:35:00"
  }
}
```

---

### 2.2 Apne Saare Resumes Dekho

```
GET /api/resumes
```

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Resumes retrieved",
  "data": [
    {
      "id": 1,
      "fileName": "a1b2c3d4-rahul_resume.pdf",
      "originalFileName": "rahul_resume.pdf",
      "contentType": "application/pdf",
      "fileSize": 245760,
      "description": "Frontend developer resume",
      "active": true,
      "createdAt": "2026-08-28T10:35:00",
      "updatedAt": "2026-08-28T10:40:00"
    },
    {
      "id": 2,
      "fileName": "e5f6g7h8-rahul_resume_v2.pdf",
      "originalFileName": "rahul_resume_v2.pdf",
      "contentType": "application/pdf",
      "fileSize": 307200,
      "description": "Updated version",
      "active": false,
      "createdAt": "2026-08-29T14:20:00",
      "updatedAt": "2026-08-29T14:20:00"
    }
  ]
}
```

---

### 2.3 Active Resume Dekho

```
GET /api/resumes/active
```

Jo resume currently "active" hai (job matching ke liye use hota hai), woh milega.

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Active resume retrieved",
  "data": {
    "id": 1,
    "fileName": "a1b2c3d4-rahul_resume.pdf",
    "originalFileName": "rahul_resume.pdf",
    "contentType": "application/pdf",
    "fileSize": 245760,
    "description": "Frontend developer resume",
    "active": true,
    "createdAt": "2026-08-28T10:35:00",
    "updatedAt": "2026-08-28T10:40:00"
  }
}
```

---

### 2.4 Resume By ID Dekho

```
GET /api/resumes/{id}
```

| Param | Type | Description |
|---|---|---|
| `id` | Long | Resume ka unique ID |

**Example:** `GET /api/resumes/1`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Resume retrieved",
  "data": {
    "id": 1,
    "fileName": "a1b2c3d4-rahul_resume.pdf",
    "originalFileName": "rahul_resume.pdf",
    "contentType": "application/pdf",
    "fileSize": 245760,
    "description": "Frontend developer resume",
    "active": true,
    "createdAt": "2026-08-28T10:35:00",
    "updatedAt": "2026-08-28T10:40:00"
  }
}
```

---

### 2.5 Resume Download Karo

```
GET /api/resumes/{id}/download
```

Yeh endpoint raw file return karta hai (binary data), JSON nahi.

**Headers:**
```
Authorization: Bearer <access-token>
```

**curl Example:**
```bash
curl -X GET http://localhost:8080/api/resumes/1/download \
  -H "Authorization: Bearer <token>" \
  --output resume.pdf
```

**Response Headers:**
```
Content-Disposition: attachment; filename="rahul_resume.pdf"
Content-Type: application/pdf
Content-Length: 245760
```

---

### 2.6 Resume Activate Karo

```
PUT /api/resumes/{id}/activate
```

Yeh resume ko "active" bana dega. Baaki sab resumes automatically inactive ho jayenge.

**Headers:**
```
Authorization: Bearer <access-token>
```

**Example:** `PUT /api/resumes/2/activate`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Resume activated",
  "data": {
    "id": 2,
    "fileName": "e5f6g7h8-rahul_resume_v2.pdf",
    "originalFileName": "rahul_resume_v2.pdf",
    "active": true,
    ...
  }
}
```

---

### 2.7 Resume Delete Karo

```
DELETE /api/resumes/{id}
```

**Headers:**
```
Authorization: Bearer <access-token>
```

**Example:** `DELETE /api/resumes/1`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Resume deleted successfully",
  "data": null
}
```

---

## 3. 🤖 ML (Machine Learning) Endpoints

Base path: `/api/ml`

Yeh endpoints ML service ke saath baat karte hain — resume analyze aur job matching ke liye. Sabhi **authenticated** hain.

---

### 3.1 ML Service Health Check

```
GET /api/ml/health
```

Check karo ki ML service chal raha hai ya nahi.

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "ML service is available",
  "data": {
    "available": true
  }
}
```

```json
{
  "success": true,
  "message": "ML service is unavailable",
  "data": {
    "available": false
  }
}
```

---

### 3.2 Resume Analyze Karo (Text Based)

```
POST /api/ml/analyze
```

Resume ka text bhejo aur ML model se analysis lo — skills, experience level, education, etc.

**Headers:**
```
Authorization: Bearer <access-token>
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Description |
|---|---|---|---|
| `resumeText` | String | ✅ | Resume ka pura text content |

**Example Request:**
```json
{
  "resumeText": "Rahul Sharma - B.Tech CSE from IIT Delhi. Skills: Java, React, Spring Boot, Python. 2 years experience at TCS as Software Developer."
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Resume analyzed successfully",
  "data": {
    "skills": ["Java", "React", "Spring Boot", "Python"],
    "experience_level": "mid",
    "education": "B.Tech CSE",
    "college": "IIT Delhi"
  }
}
```

**Error (400 Bad Request):**
```json
{
  "success": false,
  "message": "resumeText is required"
}
```

---

### 3.3 Job Matching (File Upload Based)

```
POST /api/ml/match
```

Resume file upload karo aur ML model tumhe best matching jobs batayega.

**Headers:**
```
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

**Form Data:**

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `file` | File | ✅ | — | Resume ka file (PDF/DOC) |
| `topN` | Integer | ❌ | `10` | Kitni top jobs dikhani hain |

**curl Example:**
```bash
curl -X POST http://localhost:8080/api/ml/match \
  -H "Authorization: Bearer <token>" \
  -F "file=@rahul_resume.pdf" \
  -F "topN=5"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Jobs matched successfully",
  "data": {
    "profile": {
      "skills": ["Java", "React", "Spring Boot"],
      "experience_level": "mid"
    },
    "totalJobs": 42,
    "matches": [
      {
        "id": 101,
        "title": "Software Developer — Infosys",
        "score": 92,
        "reason": "Strong match: Java + Spring Boot experience aligns with role requirements",
        "fields": {
          "location": "Bangalore",
          "stipend": "₹25,000/month",
          "duration": "6 months"
        }
      },
      {
        "id": 205,
        "title": "Full Stack Intern — Wipro",
        "score": 87,
        "reason": "Good match: React frontend + Java backend skills match",
        "fields": {
          "location": "Hyderabad",
          "stipend": "₹20,000/month",
          "duration": "3 months"
        }
      }
    ]
  }
}
```

---

## 4. 💼 Internship Endpoints

Base path: `/api/internships`

Yeh sabhi endpoints **authenticated** hain — JWT token zaroori hai.

---

### 4.1 Saari Internships Dekho

```
GET /api/internships
```

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
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
    },
    {
      "id": 2,
      "title": "Frontend Developer Intern",
      "company": "Microsoft",
      "description": "Build beautiful and performant web applications...",
      "applicationLink": "https://careers.microsoft.com/global/en/apply/789012/",
      "createdAt": "2026-08-31T10:00:00",
      "updatedAt": "2026-08-31T10:00:00"
    }
  ]
}
```

---

### 4.2 Internship By ID Dekho

```
GET /api/internships/{id}
```

| Param | Type | Description |
|---|---|---|
| `id` | Long | Internship ka unique ID |

**Example:** `GET /api/internships/1`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Internship retrieved",
  "data": {
    "id": 1,
    "title": "Software Engineering Intern",
    "company": "Google",
    "description": "Work on large-scale distributed systems...",
    "applicationLink": "https://careers.google.com/jobs/results/123456/",
    "createdAt": "2026-08-31T10:00:00",
    "updatedAt": "2026-08-31T10:00:00"
  }
}
```

---

### 4.3 Naya Internship Banao

```
POST /api/internships
```

**Headers:**
```
Authorization: Bearer <access-token>
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `title` | String | ✅ | 1-200 characters |
| `company` | String | ✅ | 1-200 characters |
| `description` | String | ❌ | Max 5000 characters |
| `applicationLink` | String | ✅ | Valid URL |

**Example Request:**
```json
{
  "title": "Backend Developer Intern",
  "company": "Infosys",
  "description": "Work on enterprise Java applications using Spring Boot and microservices.",
  "applicationLink": "https://www.infosys.com/careers/apply/123"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Internship created",
  "data": {
    "id": 9,
    "title": "Backend Developer Intern",
    "company": "Infosys",
    "description": "Work on enterprise Java applications using Spring Boot and microservices.",
    "applicationLink": "https://www.infosys.com/careers/apply/123",
    "createdAt": "2026-08-31T12:00:00",
    "updatedAt": "2026-08-31T12:00:00"
  }
}
```

---

## 5. 👤 Profile Picture Endpoints

Base path: `/api/users`

Yeh sabhi endpoints **authenticated** hain.

---

### 5.1 Upload/Replace Profile Picture

```
POST /api/users/me/profile-picture
```

**Headers:**
```
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

**Form Data:**

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | File | ✅ | JPEG, PNG, or WebP image (max 5MB) |

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile picture uploaded successfully",
  "data": {
    "profilePictureUrl": "/api/users/me/profile-picture"
  }
}
```

**Error Responses:**
- `400` — "Only JPEG, PNG, and WebP images are allowed"
- `400` — "File size exceeds maximum limit of 5MB"

---

### 5.2 Get Profile Picture

```
GET /api/users/me/profile-picture
```

**Headers:**
```
Authorization: Bearer <access-token>
```

**Response:** Raw image data with correct Content-Type header.

**Error:** `404` — No profile picture found

---

### 5.3 Delete Profile Picture

```
DELETE /api/users/me/profile-picture
```

**Headers:**
```
Authorization: Bearer <access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile picture deleted successfully"
}
```

---

## 6. 🔒 Admin Endpoints

Base path: `/api/admin`

Yeh sabhi endpoints **ROLE_ADMIN** role require karte hain.

---

### 6.1 List All Users

```
GET /api/admin/users
```

**Headers:**
```
Authorization: Bearer <admin-access-token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Users retrieved",
  "data": [
    {
      "id": 1,
      "email": "user@example.com",
      "fullName": "User Name",
      "enabled": true,
      "emailVerified": true,
      "createdAt": "2026-08-28T10:30:00"
    }
  ]
}
```

**Error:** `403` — ROLE_ADMIN required

---

### 6.2 Change User Role

```
PATCH /api/admin/users/{userId}/role
```

**Headers:**
```
Authorization: Bearer <admin-access-token>
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Allowed Values |
|---|---|---|---|
| `role` | String | ✅ | `ROLE_USER`, `ROLE_ADMIN` |

**Example Request:**
```json
{
  "role": "ROLE_ADMIN"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User role updated successfully"
}
```

**Error Responses:**
- `400` — "Role is required"
- `400` — "Invalid role. Allowed roles: ROLE_USER, ROLE_ADMIN"
- `403` — ROLE_ADMIN required

---

## 7. 🧪 Quick Testing Guide (cURL se)

### Step 1: Register karo
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "phoneNumber": "+919999999999",
    "college": "DTU",
    "password": "Test@1234",
    "confirmPassword": "Test@1234"
  }'
```

### Step 2: Login karo aur token lo
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "Test@1234"}'
```

Response se `accessToken` nikalo.

### Step 3: Resume upload karo
```bash
curl -X POST http://localhost:8080/api/resumes/upload \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -F "file=@./my_resume.pdf"
```

### Step 4: Saari internships dekho
```bash
curl -X GET http://localhost:8080/api/internships \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### Step 5: Naya internship banao
```bash
curl -X POST http://localhost:8080/api/internships \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Backend Developer Intern",
    "company": "Infosys",
    "description": "Work on enterprise Java applications.",
    "applicationLink": "https://www.infosys.com/careers/apply/123"
  }'
```

### Step 6: Job matching karwao
```bash
curl -X POST http://localhost:8080/api/ml/match \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -F "file=@./my_resume.pdf" \
  -F "topN=5"
```

---

## 6. ⚠️ Common Errors

| HTTP Status | Matlab | Kab Aata Hai |
|---|---|---|
| `400` | Bad Request | Invalid data bheja, validation fail |
| `401` | Unauthorized | Token nahi hai ya galat hai |
| `403` | Forbidden | Token hai lekin access nahi hai |
| `404` | Not Found | Endpoint ya resource exist nahi karta |
| `500` | Server Error | Backend pe kuch crash ho gaya |

---

## 7. 📝 Notes for Teammates

- **JWT Token ki expiry:** Access token ~1 ghante mein expire hota hai, refresh token zyada time tak valid rehta hai
- **Resume activate:** Sirf ek hi resume active ho sakta hai ek time pe — jab naya activate karte ho, purana wala automatically inactive ho jata hai
- **ML endpoints:** ML service alag server pe chalta hai — pehle `/health` se check karo ki woh available hai ya nahi
- **File uploads:** Sirf PDF aur DOC files accepted hain (backend pe validation lagao agar zaroorat ho)
- **CORS:** Frontend `localhost:3000` ya `localhost:5173` pe hona chahiye request karne ke liye
- **H2 Console:** `http://localhost:8080/h2-console` pe jaake database directly dekh sakte ho (dev mode mein)

---

*Last updated: 31 August 2026*

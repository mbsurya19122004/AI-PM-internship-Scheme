# WeatherGPT API Endpoints

Base URL: `http://localhost:8080`

All responses follow the envelope format:
```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

---

## Phase 1 — Weather

### GET /api/weather/current

Get real-time weather conditions for a location.

**Query Parameters:**
- `location` (required) — Human-readable location name (e.g. `{{location}}`)

**Example:**
```bash
curl "http://localhost:8080/api/weather/current?location={{location}}"
```

**Response:**
```json
{
  "success": true,
  "message": "Current weather retrieved",
  "data": {
    "location": { "name": "{{location}}", "latitude": 28.6519, "longitude": 77.2315, "country": "India" },
    "temperature": 32.5,
    "apparentTemperature": 36.2,
    "humidity": 68,
    "windSpeed": 14.2,
    "weatherCode": 3,
    "weatherDescription": "Overcast",
    "observedAt": "2026-09-05T14:30",
    "timezone": "Asia/Kolkata",
    "provider": "Open-Meteo"
  }
}
```

---

### GET /api/weather/forecast

Get a multi-day weather forecast.

**Query Parameters:**
- `location` (required) — Human-readable location name
- `days` (optional, default 7, max 16) — Number of forecast days

**Example:**
```bash
curl "http://localhost:8080/api/weather/forecast?location={{location}}&days=7"
```

**Response:**
```json
{
  "success": true,
  "message": "Weather forecast retrieved",
  "data": {
    "location": { "name": "{{location}}", "latitude": 19.0728, "longitude": 72.8826, "country": "India" },
    "timezone": "Asia/Kolkata",
    "days": [
      {
        "date": "2026-09-05",
        "weatherDescription": "Moderate rain",
        "tempMax": 31.2,
        "tempMin": 26.8,
        "precipitationProbabilityMax": 85,
        "precipitationSum": 12.4,
        "windSpeedMax": 22.0
      }
    ],
    "provider": "Open-Meteo"
  }
}
```

---

## Phase 2 — Natural-Language Chat

### POST /api/chat/query

Ask a natural-language weather question.

**Request Body:**
```json
{ "message": "Will it rain tomorrow in {{location}}?" }
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/chat/query" \
  -H "Content-Type: application/json" \
  -d '{"message": "Will it rain tomorrow in {{location}}?"}'
```

**Response:**
```json
{
  "success": true,
  "message": "Query processed",
  "data": {
    "answer": "Yes. Rain is likely tomorrow in {{location}} with a 80% chance of precipitation. Consider carrying an umbrella.",
    "intent": "RAIN_QUERY",
    "timeReference": "TOMORROW",
    "location": { "name": "{{location}}", "latitude": 28.6519, "longitude": 77.2315 },
    "advisories": ["Consider carrying an umbrella."]
  }
}
```

**Supported intents:** CURRENT_WEATHER, FORECAST, RAIN_QUERY, TEMPERATURE_QUERY, WIND_QUERY, HUMIDITY_QUERY, GENERAL_WEATHER, UNSUPPORTED

**Supported time references:** NOW, TODAY, TOMORROW, NEXT_DAY, THIS_WEEK, THIS_WEEKEND, UNSUPPORTED

**Supported aspects:** TEMPERATURE, PRECIPITATION, WIND, HUMIDITY, GENERAL

**Interpreter input coverage:**
- Location extraction: preposition phrases (`in Delhi`, `at Mumbai`, `for Chennai`) and bare city names from dictionary
- Time references: now, today, tonight, tomorrow, next day, this week, this weekend, historical (rejected)
- Weather aspects: rain/precipitation, temperature, wind, humidity
- Input sanitization: HTML stripped before parsing
- Missing location → clarification response
- Unknown location → 404
- Non-weather queries → UNSUPPORTED, no fabricated answer

**Note:** The `{{location}}` variable is a placeholder. Set it to any city the interpreter or geocoding provider can resolve (e.g. Delhi, Mumbai, Bengaluru, Chennai, Kolkata, New York, London, Tokyo).

---

## Phase 3 — Extreme Weather Alerts

### GET /api/alerts

Get active extreme weather alerts and advisories for a location.

**Query Parameters:**
- `location` (required) — Human-readable location name

**Example:**
```bash
curl "http://localhost:8080/api/alerts?location={{location}}"
```

**Response (no official provider configured):**
```json
{
  "success": true,
  "message": "Alerts retrieved",
  "data": {
    "location": "{{location}}",
    "latitude": 28.6519,
    "longitude": 77.2315,
    "alerts": [],
    "totalAlerts": 0,
    "officialProviderActive": false,
    "providerStatus": "Official extreme weather alert integration is pending. No verified government alert provider is currently configured. Check official sources such as mausam.imd.gov.in for current warnings."
  }
}
```

**Alert object structure:**
```json
{
  "id": "...",
  "title": "Heavy Rain Advisory",
  "description": "...",
  "informationClass": "AUTOMATED_ADVISORY",
  "alertType": "HEAVY_RAIN",
  "severity": "MODERATE",
  "source": "...",
  "official": false,
  "issuedAt": "2026-09-05T10:00:00Z",
  "effectiveFrom": "2026-09-05T12:00:00Z",
  "effectiveUntil": "2026-09-06T00:00:00Z",
  "affectedLocations": ["{{location}}"]
}
```

**informationClass values:**
- `OFFICIAL_WARNING` — Verified government source (IMD, NDMA). Not currently active.
- `AUTOMATED_ADVISORY` — System-generated from weather thresholds. Not an official warning.
- `OBSERVATION` — Factual observation. Not a warning.

**Consumers must always display informationClass to users.**

---

## Authentication

### POST /api/auth/register

Register a new user account.

Request body fields: `fullName`, `email`, `phoneNumber`, `password`, `confirmPassword`.

```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "phoneNumber": "9876543210",
  "password": "Secure@1234",
  "confirmPassword": "Secure@1234"
}
```

### POST /api/auth/login

Request body fields: `email`, `password`.

```json
{ "email": "jane@example.com", "password": "Secure@1234" }
```

### POST /api/auth/refresh

Refresh the access token using a valid refresh token.

Request body fields: `refreshToken`.

```json
{ "refreshToken": "<token>" }
```

### GET /api/auth/me

Get the currently authenticated user's profile.

Requires `Authorization: Bearer <token>` header.

### POST /api/auth/change-password

Change the authenticated user's password.

Requires authentication.

Request body fields: `currentPassword`, `newPassword`, `confirmNewPassword`.

```json
{
  "currentPassword": "Secure@1234",
  "newPassword": "NewSecure@5678",
  "confirmNewPassword": "NewSecure@5678"
}
```

### POST /api/auth/forgot-password

Request a password reset link for the given email.

Request body fields: `email`.

Always returns success to prevent user enumeration.

```json
{ "email": "jane@example.com" }
```

### POST /api/auth/reset-password

Reset the password using a valid reset token.

Request body fields: `token`, `newPassword`, `confirmNewPassword`.

```json
{
  "token": "<reset-token>",
  "newPassword": "NewSecure@5678",
  "confirmNewPassword": "NewSecure@5678"
}
```

### GET /api/auth/verify-email?token={token}

Verify the user's email using a valid verification token.

### POST /api/auth/resend-verification

Resend the verification email to the authenticated user.

Requires authentication.

---

## Admin (ROLE_ADMIN only)

### GET /api/admin/users

List all registered users.

### PATCH /api/admin/users/{userId}/role

Update a user's role.

Path parameter: `userId` — the ID of the user to update.

Request body fields: `role`.

```json
{ "role": "ROLE_ADMIN" }
```

Allowed roles: `ROLE_USER`, `ROLE_ADMIN`

---

## Error Responses

All errors follow:
```json
{ "success": false, "message": "Error description" }
```

| HTTP Status | Meaning                    |
| ----------- | -------------------------- |
| 400         | Validation / bad request   |
| 401         | Authentication required    |
| 403         | Forbidden / access denied  |
| 404         | Resource not found         |
| 503         | Weather provider unavailable |

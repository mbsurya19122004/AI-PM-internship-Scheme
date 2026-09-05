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
- `location` (required) — Human-readable location name (e.g. "Delhi", "Mumbai")

**Example:**
```bash
curl "http://localhost:8080/api/weather/current?location=Delhi"
```

**Response:**
```json
{
  "success": true,
  "message": "Current weather retrieved",
  "data": {
    "location": { "name": "Delhi", "latitude": 28.6519, "longitude": 77.2315, "country": "India" },
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
curl "http://localhost:8080/api/weather/forecast?location=Mumbai&days=7"
```

**Response:**
```json
{
  "success": true,
  "message": "Weather forecast retrieved",
  "data": {
    "location": { "name": "Mumbai", "latitude": 19.0728, "longitude": 72.8826, "country": "India" },
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
{ "message": "Will it rain tomorrow in Delhi?" }
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/chat/query" \
  -H "Content-Type: application/json" \
  -d '{"message": "Will it rain tomorrow in Delhi?"}'
```

**Response:**
```json
{
  "success": true,
  "message": "Query processed",
  "data": {
    "answer": "Yes. Rain is likely tomorrow in Delhi with a 80% chance of precipitation. Consider carrying an umbrella.",
    "intent": "RAIN_QUERY",
    "timeReference": "TOMORROW",
    "location": { "name": "Delhi", "latitude": 28.6519, "longitude": 77.2315 },
    "advisories": ["Consider carrying an umbrella."]
  }
}
```

**Supported intents:** CURRENT_WEATHER, FORECAST, RAIN_QUERY, TEMPERATURE_QUERY, WIND_QUERY, HUMIDITY_QUERY, GENERAL_WEATHER, UNSUPPORTED

**Supported time references:** NOW, TODAY, TOMORROW, NEXT_DAY, THIS_WEEK, THIS_WEEKEND

---

## Phase 3 — Extreme Weather Alerts

### GET /api/alerts

Get active extreme weather alerts and advisories for a location.

**Query Parameters:**
- `location` (required) — Human-readable location name

**Example:**
```bash
curl "http://localhost:8080/api/alerts?location=Delhi"
```

**Response (no official provider configured):**
```json
{
  "success": true,
  "message": "Alerts retrieved",
  "data": {
    "location": "Delhi",
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
  "affectedLocations": ["Delhi"]
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

```json
{ "email": "jane@example.com", "password": "Secure@1234" }
```

### POST /api/auth/refresh

```json
{ "refreshToken": "<token>" }
```

### GET /api/auth/me

Requires `Authorization: Bearer <token>` header.

### POST /api/auth/change-password

Requires authentication.

```json
{
  "currentPassword": "Secure@1234",
  "newPassword": "NewSecure@5678",
  "confirmNewPassword": "NewSecure@5678"
}
```

### POST /api/auth/forgot-password

```json
{ "email": "jane@example.com" }
```

Always returns success to prevent user enumeration.

### POST /api/auth/reset-password

```json
{
  "token": "<reset-token>",
  "newPassword": "NewSecure@5678",
  "confirmNewPassword": "NewSecure@5678"
}
```

### GET /api/auth/verify-email?token={token}

### POST /api/auth/resend-verification

Requires authentication.

---

## Admin (ROLE_ADMIN only)

### GET /api/admin/users

List all registered users.

### PATCH /api/admin/users/{userId}/role

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

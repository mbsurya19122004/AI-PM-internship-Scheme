# WeatherGPT

An intelligent conversational weather intelligence and disaster decision-support platform aligned with the **Ministry of Earth Sciences (MoES) / India Meteorological Department (IMD)** mission and the **Disaster Management** theme.

WeatherGPT makes meteorological information easier to access and understand through natural-language interactions, real-time weather data, and early warning architecture.

---

## Organization

| Field        | Value                                     |
| ------------ | ----------------------------------------- |
| Organization | Ministry of Earth Sciences (MoES)         |
| Department   | India Meteorological Department (IMD)     |
| Category     | Software                                  |
| Theme        | Disaster Management                       |

---

## IMPLEMENTED

### Phase 1 — Real-Time Weather Intelligence

- Real-time weather conditions by location
- Multi-day weather forecasts (up to 16 days)
- Location geocoding (human-readable name → coordinates)
- Open-Meteo API integration (free, no API key required)
- Weather provider abstraction (pluggable, provider-independent)
- Normalized weather DTOs (raw provider payloads never exposed)
- WMO weather code normalization with human-readable descriptions

**Endpoints:**

```
GET /api/weather/current?location={location}
GET /api/weather/forecast?location={location}&days={1-16}
```

### Phase 2 — Natural-Language Weather Queries

- Deterministic natural-language weather query interpreter
- Supported intents: CURRENT\_WEATHER, FORECAST, RAIN\_QUERY, TEMPERATURE\_QUERY, WIND\_QUERY, HUMIDITY\_QUERY, GENERAL\_WEATHER, UNSUPPORTED
- Supported time references: NOW, TODAY, TOMORROW, NEXT\_DAY, THIS\_WEEK, THIS\_WEEKEND
- Conversational response generation grounded in real provider data
- Simple data-driven automated advisories (clearly labelled, never presented as official warnings)
- Query interpreter interface supporting future LLM-based implementation

**Endpoint:**

```
POST /api/chat/query
Body: { "message": "Will it rain tomorrow in Delhi?" }
```

### Phase 3 — Extreme Weather Alert Foundation

- `WeatherAlertProvider` interface (pluggable, supports multiple future providers)
- Normalized `WeatherAlertDto` with all standard alert fields
- `AlertType` enum (RAIN, HEAVY\_RAIN, THUNDERSTORM, CYCLONE, FLOOD, LANDSLIDE, etc.)
- `AlertSeverity` enum (LOW, MODERATE, SEVERE, EXTREME, UNKNOWN)
- `AlertInformationClass` enum — **mandatory classification**: OFFICIAL\_WARNING, AUTOMATED\_ADVISORY, OBSERVATION
- `NoOpAlertProvider` — truthful placeholder that returns no alerts and correctly identifies itself as non-official
- `AlertService` — provider orchestration with classification enforcement
- Safety guard: non-official providers cannot produce OFFICIAL\_WARNING alerts
- Provider failure handling with graceful degradation
- Location-based alert query endpoint

**Endpoint:**

```
GET /api/alerts?location={location}
```

**Response always includes:**
- `officialProviderActive` — whether a verified official source is configured
- `providerStatus` — human-readable provider status message
- `informationClass` on each alert — OFFICIAL\_WARNING / AUTOMATED\_ADVISORY / OBSERVATION

### Authentication and Security (Supporting Infrastructure)

- User registration and login
- JWT access and refresh tokens
- Token versioning (invalidates all tokens on password change)
- Password reset (email token, 15-minute expiry)
- Email verification
- Account lockout after 5 failed login attempts
- Rate limiting on password reset requests
- RBAC (ROLE\_USER, ROLE\_ADMIN)
- Admin user management and role assignment
- Security event audit logging
- Input sanitization (XSS prevention)

---

## PLANNED (NOT YET IMPLEMENTED)

- Official IMD / NDMA alert provider integration
- Conversation context and persistence
- LLM-based query understanding (optional enhancement, deterministic remains active)
- Multilingual support (Indian languages)
- Voice interaction (speech-to-text / text-to-speech)
- Historical weather and climate analytics
- Weather risk assessment engine (Phase 4)
- Sector-specific decision support: agriculture, aviation, marine, disaster preparedness
- NWP / GFS / WRF numerical weather prediction model integration
- Real-time event ingestion (WebSocket / MQTT)

---

## Architecture

```
WeatherController    ChatController     AlertController
        ↓                   ↓                  ↓
WeatherService      WeatherQueryService    AlertService
        ↓              ↓          ↓            ↓
GeocodingProvider  QueryInterpreter  ResponseGenerator  WeatherAlertProvider
        ↓               ↓                           ↓
OpenMeteoGeocodingProvider  DeterministicInterpreter  NoOpAlertProvider (placeholder)
        ↓
  OpenMeteoWeatherProvider
```

### Key Architectural Rules

- Controllers are thin — no business logic, no provider-specific parsing
- Services orchestrate business logic
- External APIs are isolated behind provider interfaces
- Raw provider payloads are never exposed to API clients
- Query interpretation is separate from weather data retrieval
- Weather responses are grounded in actual provider data
- Automated advisories are never presented as official government warnings
- No alert is fabricated — if no official provider is configured, the endpoint returns an empty list with a truthful provider status

---

## Tech Stack

| Component          | Technology                      |
| ------------------ | ------------------------------- |
| Backend            | Java 17, Spring Boot 3.2        |
| Database           | H2 (in-memory, dev); swap to PostgreSQL for production |
| Weather API        | Open-Meteo (free, no API key)   |
| Geocoding API      | Open-Meteo Geocoding (free)     |
| Authentication     | JWT (JJWT 0.12)                 |
| Build              | Maven                           |

---

## Running Locally

```bash
cd backend
export JWT_SECRET=<base64-encoded-secret-min-256-bits>
mvn spring-boot:run
```

**Test the API:**

```bash
# Current weather
curl "http://localhost:8080/api/weather/current?location=Delhi"

# 7-day forecast
curl "http://localhost:8080/api/weather/forecast?location=Mumbai&days=7"

# Natural-language query
curl -X POST "http://localhost:8080/api/chat/query" \
  -H "Content-Type: application/json" \
  -d '{"message": "Will it rain tomorrow in Chennai?"}'

# Extreme weather alerts
curl "http://localhost:8080/api/alerts?location=Delhi"
```

---

## Tests

```bash
cd backend
mvn clean test
```

Current status: **108 tests, 0 failures, 0 errors**

---

## Important: Alert Information Classification

Every alert or advisory returned by the API includes an `informationClass` field. Consumers **must** present this to users:

| Class                 | Meaning                                                                 |
| --------------------- | ----------------------------------------------------------------------- |
| `OFFICIAL_WARNING`    | Originates from a verified government source (e.g. IMD, NDMA)          |
| `AUTOMATED_ADVISORY`  | System-generated from weather data thresholds. Not a government warning |
| `OBSERVATION`         | Factual statement from observed conditions. Not a warning               |

**WeatherGPT never fabricates official government warnings.**

Until a real official alert provider is integrated, `GET /api/alerts` returns an empty alert list with `officialProviderActive: false` and a `providerStatus` message directing users to check `mausam.imd.gov.in`.

---

## Future Roadmap

1. Official extreme weather alert provider integration (IMD / CAP feeds)
2. Disaster-oriented location-based alert intelligence
3. Conversation persistence and context
4. Multilingual support (Hindi, Bengali, Tamil, Telugu, and other Indian languages)
5. Voice interaction
6. Historical climate analytics
7. NWP/GFS/WRF integration
8. Sector-specific decision support (agriculture, aviation, marine, disaster preparedness)
9. Scalable real-time ingestion (MQTT / WebSocket)

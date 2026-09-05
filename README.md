# WeatherGPT

A conversational weather intelligence backend built with Java 17 and Spring Boot 3.2. WeatherGPT is aligned with the **Ministry of Earth Sciences (MoES) / India Meteorological Department (IMD)** mission and the **Disaster Management** theme.

[![Build](https://img.shields.io/badge/build-Maven-informational?logo=apache-maven)](https://maven.apache.org/)
[![Java](https://img.shields.io/badge/java-17-blue?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.2-black?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/tests-JUnit-orange?logo=junit5)](https://junit.org/)
[![License](https://img.shields.io/badge/license-internal-lightgrey)](https://choosealicense.com/)

> Badges: replace the artifact URLs with your repo/CI/coverage links once they exist.

## Mission framing: MoES / IMD

WeatherGPT is built with the **Ministry of Earth Sciences (MoES)** and the **India Meteorological Department (IMD)** mission in view: make meteorological information more accessible, more understandable, and more actionable for the people and systems that need it.

The project is also aligned with the **Disaster Management** theme. It is not an official warning authority, but it is designed to support early-awareness workflows by turning weather data into plain-language answers, location-based alerts, and clearly labelled advisories that can feed dashboards, monitoring tools, and decision-support processes.

Where official government warnings exist, the system should surface them through a verified source. Where they do not yet exist in the system, WeatherGPT should say so plainly rather than inventing a warning.

## Disaster-management use cases

- **Early awareness, not final authority.** Residents, coordinators, and frontline teams can use the chat and alert endpoints to get a quick natural-language read on current conditions and likely weather changes for a place they care about.
- **Situational awareness before and during an event.** Multi-day forecasts and current conditions support planning for rain, heat, wind, and other conditions that commonly matter in disaster-management workflows.
- **Clear separation between official and automated information.** The `informationClass` field on alerts and advisories is meant to make it harder for automated content to be mistaken for a government warning.
- **Graceful absence of official data.** When no official alert provider is configured, the alerts endpoint returns an empty list and tells consumers where to check instead. That is intentional: the system prefers a truthful empty response over a fabricated warning.

A short way to think about the product: WeatherGPT helps people ask weather questions in their own language and get grounded answers fast, while keeping official warnings and automated advisories visually and semantically distinct.

<!-- Quick-start -->

```bash
# 1. Start the backend
cd backend
export JWT_SECRET=<base64-secret-at-least-256-bits>
mvn spring-boot:run

# 2. Try it from another terminal
curl -s "http://localhost:8080/api/weather/current?location=Delhi" | jq .
curl -s "http://localhost:8080/api/weather/forecast?location=Mumbai&days=7" | jq .
curl -s -X POST "http://localhost:8080/api/chat/query" -H "Content-Type: application/json" -d '{"message":"Will it rain tomorrow in Chennai?"}' | jq .
curl -s "http://localhost:8080/api/alerts?location=Delhi" | jq .
```

_Tip: if you want to reuse one city, replace `Delhi` / `Mumbai` / `Chennai` in all four commands._

It turns natural-language weather questions, real-time conditions, multi-day forecasts, and extreme-weather alerts into structured, reproducible API responses — without fabricating official government warnings.

---

## What it does

- **Real-time weather** for a human-readable location name
- **Multi-day forecasts** (up to 16 days)
- **Natural-language weather queries** with a deterministic interpreter
- **Extreme-weather alert foundation** with mandatory information classification (`OFFICIAL_WARNING` / `AUTOMATED_ADVISORY` / `OBSERVATION`)
- **Authentication, authorization, and account lifecycle** — registration, login, JWT refresh, password reset, email verification, lockout, and RBAC

---

## API at a glance

Base URL: `http://localhost:8080`

All responses use the same envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

### Weather

```bash
# Current conditions
curl "http://localhost:8080/api/weather/current?location=Delhi"

# Multi-day forecast
curl "http://localhost:8080/api/weather/forecast?location=Mumbai&days=7"
```

### Natural-language chat

```bash
curl -X POST "http://localhost:8080/api/chat/query" \
  -H "Content-Type: application/json" \
  -d '{"message": "Will it rain tomorrow in Chennai?"}'
```

### Extreme-weather alerts

```bash
curl "http://localhost:8080/api/alerts?location=Delhi"
```

Until an official alert provider is wired in, `/api/alerts` returns an empty alert list with a truthful `providerStatus` and `officialProviderActive: false`. It does not invent warnings.

### Authentication

```bash
# Register
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{ "fullName": "Jane Doe", "email": "jane@example.com", "phoneNumber": "9876543210", "password": "Secure@1234", "confirmPassword": "Secure@1234" }'

# Login
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{ "email": "jane@example.com", "password": "Secure@1234" }'

# Refresh token
curl -X POST "http://localhost:8080/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "<token>" }'

# Current user
curl "http://localhost:8080/api/auth/me" \
  -H "Authorization: Bearer <token>"
```

Password reset, email verification, and admin user-management endpoints also exist. See the project’s endpoint reference for the full list.

---

## Architecture

```
WeatherController      ChatController       AlertController
     ↓                      ↓                     ↓
WeatherService      WeatherQueryService      AlertService
     ↓                    ↓        ↓              ↓
GeocodingProvider   QueryInterpreter  ResponseGenerator   WeatherAlertProvider
     ↓                   ↓                          ↓
OpenMeteoGeocodingProvider  DeterministicInterpreter   NoOpAlertProvider (placeholder)
     ↓
OpenMeteoWeatherProvider
```

A few hard rules shape the code:

- Controllers are thin. No business logic, no provider-specific parsing in request handlers.
- External APIs are isolated behind provider interfaces.
- Raw provider payloads are never exposed to API clients.
- Query interpretation is separated from weather data retrieval.
- Responses are grounded in real provider data.
- Automated advisories are never presented as official government warnings.
- If no official provider is configured, the alerts endpoint returns an empty list and tells the consumer where to look instead.

---

## Data sources

| Concern | Source |
| --- | --- |
| Current weather + forecast | Open-Meteo |
| Geocoding | Open-Meteo Geocoding |
| Alerts (today) | Placeholder `NoOpAlertProvider` |

Open-Meteo is free and does not require an API key, which keeps local development simple. The provider layer is pluggable, so other sources can be added later without changing controllers or business logic.

---

## Alert classification

Every alert or advisory includes an `informationClass` field. Consumers should present this to end users.

| Class | Meaning |
| --- | --- |
| `OFFICIAL_WARNING` | Originates from a verified government source such as IMD or NDMA |
| `AUTOMATED_ADVISORY` | System-generated from weather thresholds. Not a government warning |
| `OBSERVATION` | Factual statement from observed conditions. Not a warning |

WeatherGPT does not fabricate official warnings. With only the placeholder provider active, alert responses are intentionally empty and transparent about that fact.

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Language / framework | Java 17, Spring Boot 3.2 |
| Build | Maven |
| Database (dev) | H2 in-memory |
| Database (production) | Swap to PostgreSQL |
| Authentication | JWT (JJWT 0.12) |
| Weather API | Open-Meteo |
| Geocoding API | Open-Meteo Geocoding |

---

## Running locally

```bash
cd backend
export JWT_SECRET=<base64-encoded-secret, at least 256 bits>
mvn spring-boot:run
```

Default local settings come from `backend/src/main/resources/application.properties`. The app runs on port `8080`. Out of the box, authentication, email, and SMTP are configured to work in a development posture, so the backend can start without external mail infrastructure.

If you want, you can override the weather and geocoding base URLs through environment variables:

```bash
export WEATHER_API_BASE_URL=https://api.open-meteo.com/v1
export GEOCODING_API_BASE_URL=https://geocoding-api.open-meteo.com/v1
```

---

## Testing

```bash
cd backend
mvn clean test
```

The project currently has a substantial Spring Boot test suite covering weather flow, chat query interpretation, alerts, controllers, token handling, and provider behavior.

---

## Roadmap

**Implemented**

- Real-time weather and multi-day forecasts
- Deterministic natural-language weather query interpretation
- Conversational weather responses grounded in live provider data
- Alert classification framework and placeholder provider with safety guards
- JWT-based authentication with refresh tokens, password reset, email verification, account lockout, and RBAC
- Admin user and role management

**Planned**

- Official IMD / NDMA alert provider integration
- Conversation persistence and context
- Optional LLM-based query understanding alongside the deterministic interpreter
- Multilingual support for Indian languages
- Voice interaction
- Historical weather and climate analytics
- Weather risk assessment and sector-specific decision support
- Real-time event ingestion via WebSocket / MQTT
- NWP / GFS / WRF model integration

---

## Important caveat

WeatherGPT is a decision-support tool, not an official warning authority. Until a verified government alert source is integrated, treat automated advisories as informational and route official warnings through the appropriate government channels.

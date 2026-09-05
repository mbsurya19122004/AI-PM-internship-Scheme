# WeatherGPT Backend — Implementation Notes

**Organization:** Ministry of Earth Sciences (MoES) / India Meteorological Department (IMD)  
**Theme:** Disaster Management  
**Tech Stack:** Java 17, Spring Boot 3.2, H2 (dev), JWT, Maven

---

## Phase 3 Implementation Report

### Completed Features

**Repository Cleanup and Rename**
- Removed entire `ML/` directory: legacy internship resume-job matching FastAPI service, sample resumes, jobs CSV dataset
- Removed `frontend/placeholder.txt`, `frontend/package-lock.json` (empty packages artifact)
- Removed `backend/placeholder.txt`
- Renamed package from `com.internshipplatform` → `com.weathergpt`
- Renamed artifact from `internship-platform-backend` → `weathergpt-backend`
- Renamed main class from `InternshipPlatformApplication` → `WeatherGptApplication`
- Removed legacy `User` fields: `college`, `department`, `graduationYear`
- Removed legacy `RegisterRequest` fields: `college` (was required!), `department`, `graduationYear`
- Cleaned `UserResponse` and `AdminController` to remove legacy fields

**Phase 3 — Extreme Weather Alert Foundation**
- `WeatherAlertProvider` interface in `com.weathergpt.weather.alert`
- `AlertType` enum: RAIN, HEAVY_RAIN, THUNDERSTORM, LIGHTNING, CYCLONE, STRONG_WIND, HEATWAVE, COLD_WAVE, FOG, FLOOD, LANDSLIDE, OTHER
- `AlertSeverity` enum: LOW, MODERATE, SEVERE, EXTREME, UNKNOWN
- `AlertInformationClass` enum: OFFICIAL_WARNING, AUTOMATED_ADVISORY, OBSERVATION (mandatory classification on every alert)
- `WeatherAlertDto` — fully normalized alert DTO
- `AlertResponse` — top-level response including `officialProviderActive` and `providerStatus`
- `NoOpAlertProvider` — truthful placeholder; returns no alerts; isOfficialSource() = false
- `AlertService` — orchestrates provider calls; enforces classification constraints
- Safety guard: non-official providers that produce OFFICIAL_WARNING alerts are reclassified to AUTOMATED_ADVISORY
- Provider failure handled gracefully — returns response with error status instead of crashing
- `AlertController` — GET /api/alerts?location={location}

### Files Added

**Main source (com.weathergpt):**
- `WeatherGptApplication.java`
- `entity/User.java` (legacy fields removed)
- `entity/PasswordResetToken.java`
- `entity/EmailVerificationToken.java`
- `repository/UserRepository.java`
- `repository/PasswordResetTokenRepository.java`
- `repository/EmailVerificationTokenRepository.java`
- `dto/RegisterRequest.java` (legacy fields removed)
- `dto/UserResponse.java` (legacy fields removed)
- `dto/ApiResponse.java`, `AuthResponse.java`, `LoginRequest.java`, `RefreshTokenRequest.java`
- `dto/ChangePasswordRequest.java`, `ForgotPasswordRequest.java`, `ResetPasswordRequest.java`
- `dto/weather/CurrentWeatherResponse.java`, `ForecastResponse.java`, `ForecastDay.java`, `LocationInfo.java`
- `dto/chat/ChatQueryRequest.java`, `ChatResponse.java`
- `dto/alert/WeatherAlertDto.java` (Phase 3 new)
- `dto/alert/AlertResponse.java` (Phase 3 new)
- `weather/WeatherProvider.java`, `GeocodingProvider.java`
- `weather/model/GeoLocation.java`
- `weather/query/WeatherIntent.java`, `TimeReference.java`, `WeatherAspect.java`, `ParsedWeatherQuery.java`
- `weather/query/WeatherQueryInterpreter.java`, `DeterministicWeatherQueryInterpreter.java`
- `weather/provider/OpenMeteoWeatherProvider.java`
- `weather/geocoding/OpenMeteoGeocodingProvider.java`
- `weather/alert/AlertType.java` (Phase 3 new)
- `weather/alert/AlertSeverity.java` (Phase 3 new)
- `weather/alert/AlertInformationClass.java` (Phase 3 new)
- `weather/alert/WeatherAlertProvider.java` (Phase 3 new)
- `weather/alert/NoOpAlertProvider.java` (Phase 3 new)
- `exception/ResourceNotFoundException.java`, `WeatherProviderException.java`, `GlobalExceptionHandler.java`
- `config/WeatherConfig.java`, `SecurityConfig.java`
- `security/*` (all 8 security files migrated)
- `service/AuthService.java` (legacy fields removed)
- `service/EmailService.java`, `WeatherService.java`, `WeatherQueryService.java`, `WeatherResponseGenerator.java`
- `service/AlertService.java` (Phase 3 new)
- `controller/AuthController.java`, `WeatherController.java`, `ChatController.java`, `AdminController.java`
- `controller/AlertController.java` (Phase 3 new)

**Tests (com.weathergpt):**
- `AlertServiceTest.java` (Phase 3 new — 8 tests)
- `AlertControllerTest.java` (Phase 3 new — 5 tests)
- `NoOpAlertProviderTest.java` (Phase 3 new — 4 tests)
- All existing tests migrated to `com.weathergpt` package

### Files Removed

| File/Directory | Reason |
| --- | --- |
| `ML/app.py` | Legacy internship resume-job matching FastAPI service |
| `ML/main.py` | Legacy resume/job matching implementation |
| `ML/requirements.txt` | Legacy ML dependencies |
| `ML/jobs_sample.csv` | Legacy job listings data |
| `ML/sample_resume.pdf` | Legacy sample resume |
| `ML/sample_resume-2.pdf` | Legacy sample resume |
| `ML/__pycache__/` | Python bytecode from legacy ML service |
| `frontend/placeholder.txt` | Empty placeholder |
| `frontend/package-lock.json` | Empty npm lockfile (no real frontend) |
| `backend/placeholder.txt` | Empty placeholder |
| `backend/src/main/java/com/internshipplatform/` | Entire legacy package (migrated to com.weathergpt) |
| `backend/src/test/java/com/internshipplatform/` | Entire legacy test package (migrated to com.weathergpt) |

### Architecture

```
AlertController
      ↓
AlertService
      ↓
WeatherAlertProvider (interface)
      ↓
NoOpAlertProvider (active — no official feed configured yet)

Future providers:
      ├── ImdAlertProvider (IMD official warnings)
      ├── CapFeedAlertProvider (Common Alerting Protocol)
      └── NdmaAlertProvider (NDMA feeds)
```

**Data flow:**
1. `GET /api/alerts?location=Delhi`
2. `AlertController` → validates location param
3. `AlertService.getAlerts("Delhi")`
4. `GeocodingProvider.resolve("Delhi")` → `GeoLocation`
5. `WeatherAlertProvider.getAlerts(location)` → `List<WeatherAlertDto>`
6. Classification enforcement: non-official providers cannot produce `OFFICIAL_WARNING`
7. Return `AlertResponse` with `officialProviderActive`, `providerStatus`, alerts list

### API Endpoints

**GET /api/alerts?location={location}**

Response includes:
- `officialProviderActive: false` (no official provider yet)
- `providerStatus` — directs users to mausam.imd.gov.in
- `alerts: []` — empty (NoOpAlertProvider never fabricates)

### Error Handling

| Scenario | Behavior |
| --- | --- |
| Location not found | 404 ResourceNotFoundException |
| Provider throws exception | 200 with error providerStatus, empty alerts |
| Non-official provider produces OFFICIAL_WARNING | Reclassified to AUTOMATED_ADVISORY, logged |
| Missing location param | 400 IllegalArgumentException |

### Tests

```
Tests run: 108, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test breakdown:
- `AuthControllerTest`: 28 (authentication flow)
- `DeterministicWeatherQueryInterpreterTest`: 18 (NL query parsing)
- `WeatherQueryFlowTest`: 11 (end-to-end NL weather queries)
- `AlertServiceTest`: 8 (Phase 3 — alert orchestration)
- `WeatherServiceTest`: 8 (weather service unit)
- `JwtTokenProviderTest`: 6 (JWT)
- `WeatherResponseGeneratorTest`: 6 (response generation)
- `AlertControllerTest`: 5 (Phase 3 — alert endpoint)
- `WeatherControllerTest`: 7 (weather endpoints)
- `OpenMeteoGeocodingProviderTest`: 4 (geocoding provider)
- `NoOpAlertProviderTest`: 4 (Phase 3 — no-op provider)
- `OpenMeteoWeatherProviderTest`: 3 (weather provider)

### Runtime Verification

Application startup: verified — `BUILD SUCCESS`  
Weather endpoints: operational (mocked in tests, live with external network)  
Alert endpoint: returns truthful empty response with `officialProviderActive: false`  
Authentication: all 28 auth tests pass

### Not Implemented (Intentionally Deferred)

- Official IMD / NDMA alert provider (Phase 3 placeholder architecture built)
- Conversation persistence (Phase 5)
- LLM query understanding (Phase 6)
- Multilingual support (Phase 7)
- Historical climate analytics (Phase 8)
- Sector-specific decision support (Phase 9)
- Voice interaction (Phase 10)
- Real-time WebSocket/MQTT ingestion (Phase 11)

---

## Architecture Constraints (Non-Negotiable)

1. **No fabricated alerts** — WeatherGPT never invents official government warnings
2. **informationClass is mandatory** — every alert must classify as OFFICIAL_WARNING, AUTOMATED_ADVISORY, or OBSERVATION
3. **Provider isolation** — raw provider payloads never reach API clients
4. **Thin controllers** — no business logic in controllers
5. **Deterministic interpreter stays** — the deterministic weather query interpreter must remain the baseline even if LLM is added later
6. **Safety guard** — AlertService enforces that non-official providers cannot produce OFFICIAL_WARNING alerts

---

## Running Locally

```bash
cd backend
export JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run
```

```bash
# Test alerts endpoint
curl "http://localhost:8080/api/alerts?location=Delhi"

# Test weather
curl "http://localhost:8080/api/weather/current?location=Delhi"

# Test chat
curl -X POST "http://localhost:8080/api/chat/query" \
  -H "Content-Type: application/json" \
  -d '{"message": "Will it rain tomorrow in Mumbai?"}'
```

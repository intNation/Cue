# Sprint 3: V3 — Context Expansion (Real Signals)

## Objective
Replace the `MockContextEngine` with `ContextEngineImpl`, a production-ready component that collects real Android system signals while strictly adhering to the "Privacy First" mandate.

## 1. Signal Implementation Strategy

### A. Phone Usage (UsageStatsManager)
- **Signal**: Screen time in the 2 hours preceding the capture.
- **Permission**: `android.permission.PACKAGE_USAGE_STATS` (Note: User must manually enable this in System Settings).
- **Normalization**: 
    - `High`: > 60 minutes
    - `Medium`: 20 - 60 minutes
    - `Low`: < 20 minutes

### B. Connectivity (ConnectivityManager)
- **Signal**: Current active network type.
- **Permission**: `android.permission.ACCESS_NETWORK_STATE`.
- **Values**: `WiFi`, `Cellular`, `None`.

### C. Movement (Activity Recognition)
- **Signal**: Current physical activity (Walking, Still, In Vehicle).
- **Permission**: `android.permission.ACTIVITY_RECOGNITION`.

### D. Weather (Open-Meteo API)
- **Signal**: Current weather condition based on coarse location.
- **Permission**: `android.permission.ACCESS_COARSE_LOCATION`.
- **API**: Open-Meteo (Doesn't require API keys or user accounts).

## 2. Implementation Steps

### Phase 1: Signal Providers
Create specialized providers for each signal to keep `ContextEngineImpl` clean and testable:
- `PhoneUsageProvider.kt`
- `ConnectivityProvider.kt`
- `WeatherProvider.kt` (using Ktor or simple HttpURLConnection)

### Phase 2: ContextEngine Implementation
- Implement `ContextEngineImpl.kt` by orchestrating the providers.
- Handle permission checks gracefully (returning "Unknown" or safe defaults if denied).

### Phase 3: UI & UX
- Update `PermissionsScreen` to guide users to System Settings for "Usage Access" if they want Phone Usage insights.
- Ensure the app functions correctly if these real signals are unavailable.

## 3. Security & Privacy
- **Coarse Data**: Store only the normalized values (e.g., "High"), never the exact minute counts or precise locations.
- **Local Only**: All raw API responses are processed locally and discarded; only the final `ContextSnapshot` is persisted.

## Verification
- Verify that "High phone usage" is correctly detected after using other apps for 5+ minutes.
- Verify that switching to Airplane Mode correctly captures "None" connectivity.
- Verify that denying a permission results in an "Unknown" or "Low confidence" snapshot rather than a crash.

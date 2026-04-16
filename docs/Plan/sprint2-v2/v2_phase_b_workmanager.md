# Sprint 2: Phase B — Background Context (WorkManager)

## Objective
Implement background context polling to capture "Ghost Snapshots" at the start of scheduled study sessions, enabling the detection of silent study failures.

## Implementation Steps

### 1. Infrastructure
- Update `libs.versions.toml` to include `androidx.work:work-runtime-ktx`.
- Add `androidx-work-runtime-ktx` dependency to `app/build.gradle.kts`.

### 2. Context Polling Worker
- **`ContextPollingWorker.kt`**:
    - Extends `CoroutineWorker`.
    - In `doWork()`:
        - Capture context using `ContextEngine`.
        - Store the snapshot in `ContextSnapShotRepository` with a null `sessionId`.
        - Returns `Result.success()`.

### 3. Schedule Management
- **`ScheduleManager.kt`**:
    - Responsible for calculating the next study start time based on the user's `WeeklySchedule`.
    - Uses `WorkManager` to schedule a `OneTimeWorkRequest` for `ContextPollingWorker`.
    - Logic to reschedule the next capture after one completes.

### 4. Integration
- Trigger `ScheduleManager.updateSchedule()` after onboarding completion.
- Trigger `ScheduleManager.updateSchedule()` on app launch to ensure polling is active.

## Verification
- Use `adb shell dumpsys jobscheduler` to verify WorkManager tasks are scheduled.
- Inspect the database to verify snapshots with `sessionId = NULL` are created at the correct times.

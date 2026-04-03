# Sprint 2: V2 — First Insight Engine & Silent Failure Detection

## Objective
Implement a rule-based engine that detects both explicit (check-in) and silent (missed schedule) study failures, using WorkManager to capture contextual "Ghost Snapshots" at scheduled start times.

## 1. Data Foundation Updates
### `ContextSnapshot` Evolution
- Make `sessionId` nullable (allow snapshots without a session).
- Add `timestamp: Long` to track exactly when signals were collected.
- **Table**: `ContextSnapshot` (Schema update).

### `Insight` Entity
- `id`: Long (PK)
- `message`: String
- `type`: String
- `timestamp`: Long
- **Table**: `Insight` (New).

## 2. Ghost Snapshot Mechanism (WorkManager)
To answer "Why" for missed sessions, we must collect context even if the app isn't opened.
- **`ContextPollingWorker`**: A background task that captures signals.
- **`ScheduleManager`**: Logic to schedule the worker based on the user's `WeeklySchedule` (Step 2 of onboarding).
- **Execution**: At every "Start Time" defined in the schedule, the app silently captures context.

## 3. The Initiation Failure Detector (Logic)
A day is marked as a "Failure" if:
1. **Explicit**: User manually checked in as "No".
2. **Silent**: A scheduled study window has passed, but NO `StudySession` was recorded and NO check-in was made.

### V2 Rule Logic:
- **Correlate Failures**: Match failure events with the snapshot (Session Snapshot or Ghost Snapshot) for that day/time.
- **Rules**:
  - If Failure + PhoneUsage == "High" -> *Insight: "High phone use before study correlates with missed sessions."*
  - If Failure + Sleep < 6 -> *Insight: "Initiation is 40% harder after poor sleep."*

## 4. Implementation Steps

### Phase A: Infrastructure
1. Add `WorkManager` dependency.
2. Update `ContextSnapshotEntity` (null sessionId + timestamp).
3. Create `Insight` Entity and Dao.

### Phase B: Background Context
1. Implement `ContextPollingWorker`.
2. Implement `ScheduleManager` to hook into onboarding/boot.

### Phase C: Insight Generation
1. Implement `GenerateInsightsUseCase`:
  - Fetch Schedule vs Sessions.
  - Fetch Snapshots (Ghost + Real).
  - Apply Rules and Save `Insight`.

### Phase D: UI
1. Create `InsightsScreen.kt` (Text cards).

## Verification
- Verify `WorkManager` triggers Ghost Snapshots at the correct time.
- Verify a "Silent Failure" is detected if a scheduled time passes without action.
- Verify the `GenerateInsightsUseCase` produces an insight based on a Ghost Snapshot.

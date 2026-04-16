# Architecture Design: Project Cue

## 1. Overview
Project Cue follows a specialized **Clean Architecture** approach. To effectively detect failure patterns through contextual awareness, the system is divided into five core components. This separation ensures that complex signal collection and background analysis do not interfere with the user experience or core business logic.

## 2. Core Components

### 2.1 Presentation Layer (UI Layer)
- **Framework**: Jetpack Compose.
- **Pattern**: MVVM (Model-View-ViewModel).
- **Responsibility**: Rendering the UI, managing UI state, and handling user interactions.
- **Dependency**: Communicates only with the **Domain Layer**.

### 2.2 Domain Layer (Business Logic)
- **Responsibility**: Contains the core business rules, models, and Use Cases.
- **Independence**: This layer is pure Kotlin and has no dependencies on Android frameworks.
- **Key Use Cases**: `StartSessionUseCase`, `EndSessionUseCase`, `GetHistoricalSessionsUseCase`.

### 2.3 Data Layer
- **Responsibility**: Acts as the single source of truth. It coordinates data between local storage and the Context Engine.
- **Components**: Repositories and Room Database.

### 2.4 Context Engine (V1: Mocked)
- **Responsibility**: Responsible for **Signal Collection** and **Data Normalization**.
- **V1 Implementation**: In V1, this component returns **mocked/static values** (dummy data) for phone usage, weather, and connectivity to establish the data pipeline without sensor complexity.
- **Future (V2+)**: Will interface with Android System APIs (UsageStats, Sleep API, etc.).

### 2.5 Background System (V1: Inactive)
- **Responsibility**: Manages non-UI, asynchronous tasks.
- **V1 Implementation**: **Disabled**. No WorkManager or background polling is implemented in V1.
- **Future (V2+)**: Will handle periodic signal polling and batch analysis.

## 3. Data Models (V1)

### 3.1 StudySession
- `id`: Long (Primary Key)
- `startTime`: Long (Unix Timestamp)
- `endTime`: Long? (Unix Timestamp)
- `status`: SessionStatus (ACTIVE, ENDED)
- `endType`: EndType? (MANUAL, AUTO)

### 3.2 ContextSnapshot
- `id`: Long (Primary Key)
- `sessionId`: Long (Foreign Key to StudySession)
- `phoneUsage`: String (Dummy: "High", "Medium", "Low")
- `connectivity`: String (Dummy: "WiFi", "Cellular", "None")
- `weather`: String (Dummy: "Sunny", "Rainy", "Cloudy")
- `sleep`: Int? (Dummy: "8". "6" , "4") 
- `confidenceScore`: Float (0.0 – 1.0)

### 3.3 DailyCheckIn
- `id`: Long (Primary Key)
- `timestamp`: Long (Unix Timestamp)
- `didStudy`: Boolean (Binary Tap: Yes/No)

## 4. Session Lifecycle & Safety (V1)

- **START**: Triggered by user. Creates a `StudySession` with `ACTIVE` status and takes a `ContextSnapshot`.
- **END (MANUAL)**: Triggered by user. Updates `endTime` and sets status to `ENDED`, `endType` to `MANUAL`.
- **AUTO-END (SAFETY)**: If a session exceeds **12 hours** (Max Duration), the system shall automatically mark it as `ENDED` with `endType` to `AUTO` upon next app launch to prevent "infinite" sessions.
- **RECOVERY**: If the app crashes or is killed during an `ACTIVE` session, the system shall restore the `ACTIVE` state upon restart.

## 5. Data Flow (V1)
The system follows a strict unidirectional flow to maintain Clean Architecture:
**UI** → **ViewModel** → **Domain (UseCase)** → **Data (Repository)** → **Context Engine** → **Local DB (Room)**

## 6. Tech Stack (V1)
- **Language**: Kotlin
- **Dependency Injection**: Simple **Constructor Injection** (Hilt is excluded from V1 to reduce overhead).
- **Concurrency**: Coroutines & Flow
- **Persistence**: Room
- **UI**: Jetpack Compose

## 7. V1 Scope: MVP Release
The goal for V1 is a stable "Session-to-Storage" pipeline.

### 7.1 Included in V1
- Manual Start/End session control.
- Room database for local persistence of sessions and snapshots.
- Mocked Context Engine returning dummy signal data.
- Basic Daily Check-in (Binary Tap).
- Auto-end safety logic (Max duration cap).

### 7.2 Excluded from V1
- **NO** Hilt (DI Framework).
- **NO** WorkManager (Background Jobs).
- **NO** Real Sensor APIs (Location, Sleep, UsageStats).
- **NO** Insight/Correlation Engine.

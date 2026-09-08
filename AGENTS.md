# Cue: Codex Project Handoff

## Read This First

Cue is a native Android application for students who understand what they need
to study but sometimes struggle to begin. It does not coach, motivate, score,
or prescribe behaviour. Its purpose is to help a student notice explainable
contextual patterns associated with delayed or missed study sessions.

Before making product or implementation decisions, read these documents:

1. `docs/Problem Statement/Problem Statement.md`
2. `docs/Solution/Solution.md`
3. `docs/Requirements/Requirements.md`
4. `docs/Product Vision/Product Vision.md`
5. `docs/Product/product.md`
6. `docs/architecture/architecture.md`
7. `docs/architecture/working-boundary.md`
8. `docs/Plan/sprint5-v5/v5-pattern-awareness-ux.md`

The V5 plan is the active development checkpoint.

## Product Guardrails

- Cue is an awareness tool, not a habit tracker, streak app, productivity
  dashboard, motivational app, or advisor.
- Insight copy is observational and non-judgmental. Do not use guilt-inducing
  language such as "failure", "bad", or commands telling the user what to do.
- Preserve the separation in `docs/architecture/working-boundary.md`:
  detection, ranking, confidence, and aggregation are deterministic domain
  concerns; calm framing and user-facing wording belong in presentation.
- Privacy is part of the product contract. Persist normalized signals, not raw
  usage data, raw weather responses, or precise location. Permission denial or
  unavailable system data must become `UNKNOWN`/unavailable, never a fabricated
  healthy signal.

## Architecture and Current Capabilities

- Kotlin, Jetpack Compose, MVVM, Room, coroutines/Flow, WorkManager, and manual
  constructor injection. There is no Hilt.
- Persistence is local Room storage: users, schedules, study locations, study
  sessions, daily check-ins, context snapshots, and insights.
- Starting a study session stores a context snapshot. A WorkManager worker also
  stores a scheduled, session-less "ghost snapshot" to identify missed planned
  sessions.
- `ContextEngineImpl` composes phone-usage, connectivity, study-location, and
  weather providers. Sleep is modeled but its real API integration is deferred.
- The rule engine is in `GenerateInsightsUseCase`. It correlates explicit and
  silent failures with nearby snapshots, filters sessions/noisy data, uses
  time-of-day buckets, calculates confidence, prioritizes candidates, preserves
  insight history, and suppresses duplicates for three days.

## Active Work: V5 Pattern Awareness

V5 is a presentation/interpretation sprint built on V4's stored insight
history. It must not expand the detection rule engine unless a concrete blocker
requires it.

V5 should turn a flat insight list into three sections:

1. At most one credible "On days like today..." hint.
2. One to three recurring pattern summaries, including strength and recurrence.
3. A lightweight newest-first timeline showing newly detected patterns and later
   reinforcements.

Completed or mostly implemented V5 work:

- Domain output models:
  - `InsightPatternSummary`
  - `TodayLikeInsightHint`
  - `InsightTimeLineEntry`
- Domain use cases:
  - `GenerateInsightSummaryUseCase`
  - `GenerateTodayLikeHintUseCase`
  - `GenerateInsightTimelineUseCase`
- Presentation models and initial mapper work under
  `presentation/insights/model` and `presentation/insights/mappers`.

The next intended task is V5 phase 5: reconstruct `InsightsUiState`,
`InsightsViewModel`, and `InsightsScreen` around the new sectioned models. The
old UI still loads `List<Insight>`, collapses it to latest-per-type, and renders
flat cards; replace that behaviour only as part of the V5 reconstruction.

## Current Worktree: Treat as User Work

At this handoff the worktree deliberately contains uncommitted changes. Do not
discard, reset, checkout over, or casually reformat them.

- `.idea/misc.xml`: IDE metadata change; unrelated.
- `docs/setup-android-dev.ps1`: new Windows bootstrap script; preserve it.
- `presentation/insights/mappers/InsightStrenghtMapper.kt`: package/import
  correction in progress.
- `presentation/insights/mappers/PatternAwarenessMapper.kt`: mapper work in
  progress. It currently references missing functions (`toInsightStrength()` and
  `eventType.to()`), so reconcile this deliberately before expecting a build to
  pass. Prefer one clear naming convention and explicit enum mapping.

Always begin implementation work with `git status --short` and inspect the
relevant diff. Preserve unrelated modifications.

## Build and Toolchain

The checked-in build requires:

- JDK 17 (Temurin on the previous machine)
- Gradle wrapper 9.2.1
- Android Gradle Plugin 9.0.1
- compileSdk/targetSdk 36.1; minSdk 30
- Android SDK platforms 36 and 36.1; build-tools 36.0.0 and 36.1.0

For a new Windows desktop, run the repository bootstrap script:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\docs\setup-android-dev.ps1
```

It installs Git, JDK 17, Android Studio, the required Android SDK/emulator
packages, configures user-level environment variables, writes `local.properties`
for the current machine, and runs `testDebugUnitTest assembleDebug` unless
`-SkipVerification` is provided. If starting with only the script, use:

```powershell
.\setup-android-dev.ps1 -ProjectRoot "$HOME\Studio\Android\Cue" -CloneIfMissing
```

Do not commit `local.properties`, `.gradle`, build outputs, or IDE workspace
files. They are machine-specific or generated.

## Verification and Working Style

- Prefer `rg` for repository searches.
- Use `apply_patch` for edits.
- Run focused unit tests first, then `./gradlew.bat testDebugUnitTest` and
  `./gradlew.bat assembleDebug` after meaningful Android changes.
- Do not claim a build is healthy without running it. A previous attempt on the
  old machine could not download the Gradle distribution because its sandboxed
  network denied the connection; that was an environment limitation, not a
  verified project result.
- Keep new domain logic testable and free of Compose/Android dependencies where
  practical. Add tests at the aggregation and ViewModel boundaries for V5.
- Do not add dashboards, charts, streaks, productivity scores, or gamification
  while implementing the V5 experience.

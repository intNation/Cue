# Cue - V5 Pattern Awareness UX (Implementation Plan)

## Objective
Improve how insights are experienced by moving from a flat list of messages to a lightweight pattern-awareness system. V5 should make Cue feel intuitive, contextual, and reflective without turning the product into a dashboard.

This sprint should build on the V4 refinement layer that already exists:
- confidence scoring
- time-of-day buckets
- prioritized top insights
- multi-signal rules
- insight history
- deduplication

V5 is not a new detection sprint. It is primarily a presentation and interpretation sprint built on top of stored `Insight` history.

---

# Current Gap

The current insight pipeline already produces refined and prioritized insights, but the UX still presents them as a flat weekly list. The main limitation is not pattern detection quality anymore, but how those patterns are surfaced to the user.

Current state:
- `GenerateInsightsUseCase` already generates confidence-scored, time-aware insights
- `Insight` history already exists in storage
- `InsightsViewModel` still collapses data into a latest-per-type list
- `InsightsScreen` still renders simple message cards

V5 should close this gap by transforming history into:
- pattern summaries
- "On days like today..." hints
- a lightweight insight timeline

---

# Phase 1 - Output Model Definition

## Goal
Define UI-facing models that represent pattern awareness clearly before touching the screen.

## Tasks
1. Add presentation/domain-facing models for:
   - `PatternSummary`
   - `TodayLikeHint`
   - `InsightTimelineItem`
   - `InsightStrength`
2. Ensure these models describe how the UX should read, not how database rows are stored.
3. Keep this layer separate from Compose UI code.

## Notes
- `InsightStrength` should map from `confidenceScore` into labels such as `Emerging`, `Moderate`, and `Strong`.
- These models should become the contract between aggregation logic and the screen.

---

# Phase 2 - Pattern Aggregation Use Case

## Goal
Transform stored `Insight` history into summarized recurring patterns.

## Tasks
1. Create a new aggregation-focused use case on top of `InsightRepository`.
2. Group repeated insight messages over a recent time window.
3. Track for each grouped pattern:
   - recurrence count
   - latest detection timestamp
   - average or representative confidence
   - derived strength label
4. Select only the strongest 1 to 3 pattern summaries for display.

## Recommended Window
- Start with the last 7 to 14 days for timeline and summary calculations.

## Important Constraint
- Reuse V4 insight outputs.
- Do not expand the rule engine unless a real blocker appears.

---

# Phase 3 - "On Days Like Today" Hint Logic

## Goal
Provide a contextual hint that connects the user's current conditions to known historical patterns.

## Tasks
1. Create a dedicated use case or a focused branch inside the aggregation layer for `TodayLikeHint`.
2. Pull the latest relevant context snapshot.
3. Match today's available signals against known strong or moderate patterns.
4. Return at most one high-signal hint for the screen header.

## Output Style
Example:
"On days like today, high phone usage usually makes it harder to start your afternoon study sessions."

## Constraints
- Keep the message observational, not prescriptive.
- If there is no credible match, show no hint.

---

# Phase 4 - Timeline Logic

## Goal
Expose recent insight history in a form that feels lightweight and understandable.

## Tasks
1. Convert recent `Insight` records into timeline items.
2. Group or label entries by day or week.
3. Preserve ordering from newest to oldest.
4. Distinguish between:
   - newly detected patterns
   - reinforced patterns

## UX Direction
- Avoid charts.
- Prefer a vertically stacked, readable sequence of events.
- Use date labels and light visual connectors where useful.

---

# Phase 5 - ViewModel and UI State Restructure

## Goal
Move from a raw insight list to a section-based screen state.

## Tasks
1. Replace the current flat `insights: List<Insight>`-centric screen contract with sectioned UI state.
2. Expand `InsightsUiState` to include:
   - `todayHint`
   - `patternSummaries`
   - `timeline`
   - `isLoading`
   - `isEmpty`
3. Remove the current latest-per-type collapse from the presentation layer.
4. Let history remain visible, since history is now part of the product value.

---

# Phase 6 - Pattern Awareness Screen

## Goal
Implement the V5 experience in three clear sections.

## Screen Structure
1. Top section:
   - `TodayHintCard`
   - only visible when a credible match exists
2. Middle section:
   - 2 to 3 `PatternSummary` cards
   - show strength, recurrence, and a subtle visual reliability cue
3. Bottom section:
   - lightweight timeline of recent detections and reinforcements

## UI Guidance
- Keep the visual language calm and understated.
- Avoid dashboard density.
- Do not introduce productivity scores, streaks, or gamification.

---

# Phase 7 - Subtle Visualization Language

## Goal
Make patterns easier to scan without relying on charts or metrics-heavy UI.

## Components
- confidence chip or label
- small recurrence indicator such as dots or short bars
- time-of-day label when relevant
- timeline dividers and icon anchors

## Constraint
- Visual cues must support comprehension, not dominate the screen.

---

# Phase 8 - Repository Support and Data Access

## Goal
Add repository/query support only where the aggregation layer becomes awkward or inefficient.

## Tasks
1. Review whether existing repository methods are enough.
2. If needed, add focused accessors such as:
   - recent insights by user ordered descending
   - insights between timestamps
   - latest N insights
3. Keep repository additions minimal and tied to V5 presentation needs.

---

# Phase 9 - Testing Strategy

## Goal
Protect the new interpretation layer with tests at the aggregation boundary.

## Required Tests
- repeated insight messages are grouped into a single summary
- confidence correctly maps to strength labels
- the best available "today-like" hint is selected
- timeline items are ordered correctly
- empty-state behavior works
- V4 history and deduplication still produce sensible V5 summaries

## Focus
- Prioritize use case and ViewModel tests over screenshot-heavy testing first.

---

# Implementation Order

1. Define V5 output models:
   `PatternSummary`, `TodayLikeHint`, `InsightTimelineItem`, `InsightStrength`
2. Build the pattern aggregation use case on top of stored `Insight` history
3. Add the "On days like today..." hint logic
4. Reshape `InsightsUiState` and `InsightsViewModel` around sections
5. Implement pattern summary cards
6. Implement the timeline section
7. Add subtle visual indicators for strength and recurrence
8. Add repository support only if aggregation becomes awkward
9. Add unit tests for aggregation, hint selection, and timeline ordering

---

# Suggested Sprint Breakdown

## Week 1
- finalize the V5 UX contract
- define output models
- implement aggregation use case
- implement hint derivation logic
- write unit tests for summaries, hint selection, and timeline mapping

## Week 2
- restructure `InsightsViewModel`
- implement the new screen sections
- connect real data to the UI
- refine copy and visual hierarchy
- validate loading, empty, and history-heavy states

---

# Success Criteria

- The Insights screen clearly distinguishes current patterns from recent history.
- Users see 1 to 3 recurring pattern summaries rather than raw message spam.
- Users receive a contextual "On days like today..." hint only when there is a credible match.
- Pattern strength is communicated subtly and clearly.
- Recent insight history is visible through a lightweight timeline.
- No dashboards, streaks, productivity scoring, or gamification are introduced.
- Tests cover the new aggregation and presentation logic.

---

# Final Note

V5 is about moving from:

"Here is a list of things we found"

to:

"Here is how your patterns have been showing up recently, and here is what seems relevant today."

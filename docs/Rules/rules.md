## Rules that define the insights
---
## Phone Usage
### Metrics that can be derivable.
- total usage duration before session
- time since last phone use
- intensity (continuous vs fragmented)
- session start delay
### groups
1. `Delay-related`
2. `Intensity-related`
3. `Consistency-related`

## Rules
1. High Pre-Study Usage → Delayed Start
"You tend to start studying later on days with `high phone use` beforehand."

2. Low Usage → Easier Start
“You start more easily when `phone usage is low` before studying.” 

3. Short Gap Between Phone & Study
"Starting `immediately after phone` use makes it harder to get into studying."

5. Habit Loop Detection
You often use your phone heavily before starting to study"

# 🌦️ Weather-Based Insight Rules (Cue V2)

## 🎯 Goal
Use **simple, explainable rules** to detect how weather conditions correlate with difficulty in starting study sessions.

---

## 📊 Available Weather Signals (Keep Minimal)

- temperature (°C)
- weatherCondition (CLEAR, CLOUDY, RAIN, STORM)
- humidity (%) *(optional)*
- windSpeed *(optional, ignore in V2)*

---

## ⚠️ Rule Design Constraints (V2)

- Use **broad categories**, not precise values
- Require **minimum repetition (≥ 3 occurrences)**
- Avoid overfitting
- Keep insights **human-readable**

---

# 🧠 RULE SET

---

## 🔹 Rule W1 — High Temperature → Reduced Start Likelihood

### Condition
IF:
- temperature ≥ 30°C
- AND study session was delayed or skipped
- occurs ≥ 3 times

### Insight
"You tend to struggle starting study sessions on hotter days."

---

## 🔹 Rule W2 — Mild Temperature → Easier Start

### Condition
IF:
- temperature between 18°C and 25°C
- AND session starts occur earlier or consistently
- occurs ≥ 3 times

### Insight
"You start studying more easily during mild weather conditions."
---

## 🔹 Rule W3 — Rainy Weather → Increased Resistance
### Condition
IF:
- weatherCondition == RAIN
- AND delayed start or no session recorded
- occurs ≥ 3 times

### Insight
"Rainy weather is often linked to difficulty in starting your study sessions."
---

## 🔹 Rule W4 — Clear Weather → Positive Start Pattern

### Condition
IF:
- weatherCondition == CLEAR
- AND sessions start on time or earlier
- occurs ≥ 3 times

### Insight
"You tend to start studying more easily on clear weather days."
---

## 🔹 Rule W5 — Extreme Weather → Disruption Pattern

### Condition
IF:
- weatherCondition == STORM
- AND session skipped or significantly delayed
- occurs ≥ 2 times *(lower threshold due to rarity)*

### Insight
"Extreme weather conditions tend to disrupt your ability to start studying."
---

## 🔹 Rule W6 — Weather Variability → Inconsistent Behavior

### Condition
IF:
- behavior differs significantly across weather types
- (e.g., good starts on CLEAR, poor starts on RAIN)
- occurs across ≥ 2–3 weather categories

### Insight
"Your study start behavior varies depending on weather conditions."

---

# 🧱 Supporting Definitions

## "Delayed Start"
- Start time significantly later than user's typical start time
  
##   "No start"
- No session recorded for the day

---

#  Recommended V2 Scope (Keep It Small)

Implement ONLY:

1. W1 — High Temperature → Harder Start
2. W3 — Rain → Harder Start
3. W4 — Clear Weather → Easier Start
---

# Key Insight
Weather is a **weak signal individually**, but becomes useful when:

- repeated over time
- combined with other signals (e.g., phone usage)
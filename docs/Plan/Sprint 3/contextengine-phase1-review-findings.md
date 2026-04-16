
     1. High: /D:/Personal Projects/Cue/app/src/main/java/com/
        cue/context/provider/PhoneUsageProvider.kt:31 treats
        missing PACKAGE_USAGE_STATS access as "Low", which will
        silently misclassify denied permission as healthy
        behavior. queryUsageStats() commonly returns an empty
        list when usage access is not granted, so this path will
        generate false insights and undermine the privacy-first
        contract that denied permissions should degrade to
        "Unknown" or low confidence, not fabricated data.
     2. High: /D:/Personal Projects/Cue/app/src/main/java/com/
        cue/context/provider/LocationProvider.kt:18 suppresses
        permission checks and then iterates all enabled
        providers, returning precise latitude/longitude from
        whichever last-known fix is most accurate. That conflicts
        with the Sprint 3 requirement to use coarse location
        only, and it creates both a privacy risk and a crash risk
        if this method is ever called without location permission
        being granted.
     3. Medium: /D:/Personal Projects/Cue/app/src/main/java/com/
        cue/context/provider/PhoneUsageProvider.kt:35 sums
        totalTimeInForeground across all results returned by
        queryUsageStats(...). Those rows are bucketed stats, not
        guaranteed to align cleanly to the last two hours, and
        summing them can overcount or blur the requested window.
        For a “2 hours preceding capture” signal, this is not
        precise enough for rule generation and will skew
        normalization thresholds.
     4. Medium: /D:/Personal Projects/Cue/app/src/main/java/com/
        cue/context/provider/WeatherProvider.kt:19 sends raw lat/
        lon directly to the remote API. Even if you only persist
        normalized weather locally, this still leaks precise
        location to a third party during collection, which
        violates the stated coarse-data/privacy-first direction
        cue/context/provider/WeatherProvider.kt:20 uses
        URL(...).readText() with no explicit timeouts or response
        validation. In a worker/context engine path this can hang
        longer than expected, hurt battery, and make background
        collection less reliable under poor connectivity.
     6. Low: /D:/Personal Projects/Cue/app/src/main/java/com/cue/
        context/provider/ConnectivityProvider.kt:26 maps Ethernet
        to "WiFi". That may be harmless on phones, but it is
        still a semantic mismatch that will pollute any future
        connectivity-based insight logic. "Other" or "Unknown"
        would be safer if the domain model does not explicitly
        support Ethernet..  The main issue pattern is silent misclassification under
     denied permissions and over-collection of location
     precision. Before wiring these into ContextEngineImpl, I
     would tighten the contract so every provider returns
     explicit “unknown/unavailable” states when access is
     missing, and ensure weather lookup never transmits precise
     coordinates.. Why this matters:

     - The context engine can distinguish “real low usage” from
       “we were not allowed to inspect usage.”
     - You can lower confidence cleanly instead of inventing
       data.
     - It supports privacy and debugging without storing raw
       values.. Context engine contract

     ContextEngineImpl should consume those provider results and
     output only normalized snapshot fields:

     interface ContextEngine {
         suspend fun captureSnapshot(sessionId: Long?):
     ContextSnapshot
     }. Inside ContextSnapshot, keep only storage-safe values:

     - phoneUsage: String
     - connectivity: String
     - weather: String
     - sleep: Int?
     - studyLocationStatus: String? if you add it
     - confidenceScore: Float
     - timestamp: Long

     No raw minutes, no coordinates, no raw API payloads..Provider responsibilities

     PhoneUsageProvider

     - Check usage access system setting explicitly.
     - If disabled: Unavailable(SYSTEM_SETTING_DISABLED).
     - If enabled but no usable data:
       Unavailable(DATA_NOT_AVAILABLE).
     - Only return normalized LOW/MEDIUM/HIGH.

     ConnectivityProvider

     - Return current transport classification only.
     - Never throw.
     - Prefer UNKNOWN over pretending disconnected state if the
       system response is ambiguous.

     LocationProvider

     - Own permission checks.
     - Use coarse permission only.
     - Immediately reduce precision.
     - For study-location matching, compare the current coarse
       fix to saved study places and return only
       AT_USUAL_LOCATION or AWAY_FROM_USUAL_LOCATION.

     WeatherProvider

     - Accept only CoarseLocation.
     - Use timeouts.
     - Return normalized weather only.
     - Never persist or expose response bodies.

     System-setting checks

     You need one for usage access. Runtime checks alone are not
     enough.

     Practical rule:

     - PACKAGE_USAGE_STATS requires a system-setting/app-ops
       check.
     - ACCESS_COARSE_LOCATION and ACCESS_NETWORK_STATE are
       standard permission/service availability checks. i have added the contracts in
   @com/cue/context/contracts/. please read through them. after that, do these. 1 Refactor
   PhoneUsageProvider to use explicit usage-access
        detection.2.  Refactor LocationProvider into getWeatherLocation() and
        getStudyLocationSignal(...).3.Update WeatherProvider to accept CoarseLocation.4.
   Then wire ContextEngineImpl
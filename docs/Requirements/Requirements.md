
# Requirements Specification:  Cue
This document outlines the functional and non-functional requirements for the Cue mobile application, following a structured approach suitable for professional software development lifecycle (SDLC) standards.

## Product requirements(High Level)
The application must provide students with insights that explains the underlying factors that explain why starting to study may feel compared to other days.

## Functional Requirements

1. User & Onboarding
    - **FR-01** - The system shall allow the student to log in using their google account or email
    - **FR-02** - The system shall cleary describe what data is being collected, and why.
    - **FR-03** - The system shall let the student to optionally opt-in to data collection permission such as location, calendar, phone usage, etc.
    - **FR-05** - The system shall ask the student their usual places of study, their usual study time, and what count as study success for them.
    - **FR-06**: The system shall allow users to view and modify their study profile and preferences at any time via the Settings module.

2. Session & Logging
    - **FR-01** - The system shall allow the student to manually log the start and end of their study sessions.
    - **FR-02** - The system shall collect the timestamp of the start and end of every logged study sessions
    - **FR-03** - The system shall semi auto-detect the end of study sessions.

3. Contextual Signal collection
   -  **FR-01**  - With permissions accepted, The system must contextually collect signals before and during study sessions
   -  **FR-02**  - The system shall detect sleep duration using the available signal
   -  **FR-03**  - The system shall detect whether condition using approximate location
   -  **FR-04**  - The system shall detect internet connectivity during the start of study sessions
   -  **FR-05**  - The System shall detect movement at coarse level
   -  **FR-05**  - The system shall detect phone usage activity prior study sessions.
   -  **FR-06**  - With calendar permissions allowed, the system shall detect calendar activities and schedule

4. User check in and binary taps
   -  **FR-01** - The system shall allow the user to manually perform study session feedback once a day with minimal effort binary taps

5. Insights and Insights presentation
   -  **FR-01** - The system shall present the user with insights once every week
   -  **FR-02** - For every insight generated, the system must present the user with a confidence score.
   -  **FR-03** - Insights shall be presented as objective observations (e.g., "Sessions started after 20:00 correlate with higher phone usage") rather than recommendations

6. Privacy and security
   -  **FR-01** - The system shall allow the user to delete Historical data at any time
   -  **FR-02** - The system shall allow the user to opt-out to any of the data collected
   -  **FR-03** - The system shall collect signals at coarse level
   -  **FR-04** - The system shall comply with the Protection of Personal Information Act (POPIA) regarding the storage and processing of South African user data


## Non-Functional Requirements
-  **NFR-01** - The system shall function gracefully with limited functionality if specific data permissions (e.g., Location) are denied by the user.
-  **NFR-02** - The system shall not affect device performance, battery life, or system resources in any way
-  **NFR-03** - in the event of "Noisy Data" or insufficient signals, the system shall provide a status indicator explaining why an insight could not be generated.
-  **NFR-04** - The uptime for the system must be >99%
-  **NFR-05** - The data architecture shall support up to 10,000 concurrent users without significant degradation in response time (< 200ms for API calls)
-  **NFR-06** - The core "Check-in" interaction must be completable by the user in under 5 seconds.
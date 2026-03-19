## Requirements

### Product requirements(High Level)
- The application must provide students with insights that explains the underlying factors that explain why starting to study may feel compared to other days.
- 


### Functional Requirements

1. User & Onboarding
    - **FR-01** - The system shall allow the student to log in using their google account or email
    - **FR-02** - The system shall cleary describe what data is being collected, and why.
    - **FR-03** - The system shall let the student to optionally opt-in to data collection permission such as location, calendar, phone usage, etc.
    - **FR-05** - The system shall ask the student their usual places of study, their usual study time, and what count as study success for them.

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
   -  **FR-03** - The system shall present the insights not as recommendations, but as observations

6. Privaty and security
   -  **FR-01** - The system shall allow the user to delete Historical data at any time
   -  **FR-01** - The system shall allow the user to opt-out to any of the data collected
   -  **FR-01** - The system shall collect signals at coarse level


### Non-Functional Requirements
-  **NFR-01** - The system shall be able to perform seamlessly even when some data collection is denied
-  **FR-01** - The system shall not affect device performance, battery life, or system resources in any way
-  **FR-01** - in case there are no enough  signals or data is noisy, the system shall gracefully mention the case.
-  **FR-01** - The uptime for the system must be >99%
-  **FR-01** - The system must scale to accommodate a growing user base of students.
-  
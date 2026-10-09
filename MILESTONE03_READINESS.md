# Milestone 03 readiness working notes — WE_167

**Status:** source and prototype inspection plus debug compilation only. These notes are not the consolidated report, do not assert participant testing, and should be checked and rewritten by the group in its own words before submission. The assignment guide requires a working app, a traceability matrix, functional execution evidence, usability sessions with at least five real or proxy participants, and a consolidated report of at most 35 pages including the cover (references and appendix excluded). Expected PDF filename: `IT3060HCI2026_Milestone03_GroupWE_167.pdf`.

## Compliance audit

| Requirement | Current evidence | Gap/status | Necessary action |
| --- | --- | --- | --- |
| Four assigned implementations | `feature/review`, `feature/courses`, `feature/timetable`, `feature/staff` and shared routes are present | **Partial**: source connected; final manual journeys not recorded | Demonstrate each member's screens and save evidence |
| Working, installable app | `:app:assembleDebug` produces a debug APK | **Partial** until installed and exercised on final revision | Install on a device and record revision/device/result |
| At least two CRUD operations per assigned interface | Course selection C/R/U/D, saved drafts C/R/U/D, timetable selection R/U/D, staff notes C/R/U/D and case status R/U; review/correction has selection R/U/D and confirmation creates/reads registration | **Ambiguous** for onboarding, read-only Success and immutable registration confirmation; see mapping below | Ask course coordinator whether the rule applies per screen or per member feature; do not add fake CRUD to read-only screens |
| Persistent operations and integrated journeys | `CourseSyncRepository` and Room transactions; active draft ID shared by navigation | **Partial**: operations are implemented; latest revision needs manual execution evidence | Run functional cases below with both student and staff flows |
| Prototype structure and intent | Rendered pages 2–6, 8–48 and 49–74 are readable; key pages compared below | **Partial**: differences documented; some depend on actual device layout | Capture app screenshots beside reference screens and justify differences |
| Functional tests and traceability | Unit/instrumentation source exists for validator, repository and timetable | **Missing final execution evidence** for all core actions and CRUD | Execute case log below; fill actual result/status/device/revision; include logs in appendix |
| Five participant usability test | Plan and blank template below | **Missing**: no recorded sessions in repository | Recruit at least five real/proxy users, obtain appropriate consent, run tasks, record results and issues |
| Consolidated report | Earlier draft content was supplied in conversation, not as a checked-in report | **Missing final evidence** | Update stale timetable claims, add genuine results, screenshots, schedule, contributions, links; stay within page limit |
| Build/run instructions and source | `README.md`, Gradle wrapper, source repository | **Partial** until another member checks setup from a clean machine | Follow README, install APK and record any setup corrections |

## CRUD by member and interface

CRUD refers to actual Room-backed data changes, not navigation, validation or a success label.

| Member / interface | Create | Read | Update | Delete | Assessment |
| --- | --- | --- | --- | --- | --- |
| 1 / Review and correction | `addSelection` for empty plan | `reopenDraft`, `validateDraft`, catalogue reads | `changeSelection`, `replaceSelection` | `removeSelection` | Multiple genuine operations; Review alone is mainly read/validation, while Correction performs mutations |
| 1 / Confirmation and Success | `confirmRegistration` creates registration and selection snapshot | `registrationSelections`, `registrations` | Intentionally unsupported after confirmation | Intentionally unsupported | Immutable registration protects final state; coordinator should clarify per-screen interpretation |
| 1 / Onboarding | SharedPreferences stores completion | SharedPreferences reads completion | Replay changes completion flow state | None | Navigation/preference flow, not an academic CRUD interface; do not count it as two CRUD operations |
| 2 / Course selection | `addSelection` | `courses`, `groups`, `remainingSeats`, `reopenDraft` | `changeSelection` | `removeSelection` | Meets two or more operations |
| 2 / Saved Drafts | `saveDraft` (named copy or new plan) | `drafts`, `reopenDraft` | `renameDraft` and edits to reopened plan | `deleteDraft` for unconfirmed plans | Meets two or more operations |
| 3 / Editable timetable and clash correction | `addSelection` via Courses route | `reopenDraft`, `validateDraft`, group/seat reads | `changeSelection` | `removeSelection` | Read/update/delete available in timetable; confirmed timetable is deliberately read only |
| 4 / Staff guidance notes | `addGuidanceNote` | `notes` | `updateGuidanceNote` | `deleteGuidanceNote` | Meets two or more operations |
| 4 / Staff case and registration view | Case creation is seeded only | `cases`, `students`, `registrations`, `registrationSelections`, `caseHistory` | `updateCaseStatus` records history | No case deletion | Read/update available; academic records remain read only |

## Prototype fidelity and deviations

Compared with rendered `docs/design reference/prototype.pdf` pages 11, 31, 35, 40, 43, 68, 70 and 71, plus PDF text extraction for other pages. This is design inspection, not a device measurement.

| Reference screen | Implemented difference | Reason | User impact |
| --- | --- | --- | --- |
| Review and confirmation (pp. 11–14) | Module counts replace prototype credit totals; app uses repository issue types and persisted results | Course model has no credit values, so a total would be invented | Accurate count but less academic detail |
| Course details / drafts (pp. 31–39) | Native Compose lists/dialogs replace several separate prototype screens; “Save a copy” describes current repository semantics | Keeps persistent editing and copy creation clear in a small app | Fewer steps; layout needs final device comparison |
| Timetable and clash resolution (pp. 40–48) | Implemented weekly grid plus readable class list; group correction is available within timetable | List helps on narrow screens and with dense overlapping sessions | Same main goal with additional fallback |
| Staff cases/guidance (pp. 68–74) | Compact Cases/Students tabs and dialogs replace dedicated case, note and history pages | Scope and shared persistent repository favour fewer destinations | More information on one scroll; test discoverability with users |
| All example screens | Prototype uses CS301-series codes and credits; app seeds CS101/MA101/UX101 etc. and fictional student Asha Perera | Repository contract uses a smaller canonical demonstration dataset | Screenshots will differ; explain this in report |
| Staff role access (pp. 67–74) | Role selector opens Staff without real authorisation | Offline assignment demonstration; no credentials or backend in scope | Do not present it as secure staff access |

## Traceability and functional cases

The prototype page references below identify readable PDF pages; FR IDs follow the available Milestone 01/02 requirement summary. Existing automated test files were inspected, but no current-revision execution log was found. Every manual case is **Not run** until a tester enters actual evidence. Each run must record commit/revision, device/API, date, tester and a screenshot/log reference.

| Requirement | Prototype screen | Implemented interface | Functional cases | Existing source coverage |
| --- | --- | --- | --- | --- |
| FR1 Detect/flag clashes | pp. 40, 46 | Timetable, Review, validator | FT03, FT18 | `PlanValidatorTest`, `TimetableScreenTest` |
| FR2 Weekly timetable | pp. 43–48 | `WeeklyTimetableScreen` | FT04 | `TimetableLayoutTest`, `TimetableScreenTest` |
| FR3 Validate before registration | pp. 11–27 | Review, Correction, Confirm, Success | FT01, FT02, FT05–FT07, FT17 | `PlanValidatorTest`, `RepositoryInstrumentedTest` |
| FR4 Prerequisites/seats | pp. 29–31, 61 | Courses, Review | FT05, FT06, FT08 | `PlanValidatorTest` |
| FR5 Immediate clash notice | p. 46 | `ClashWarning`, timetable refresh | FT03 | Source-level only; timing needs manual observation |
| FR6 Save/reopen draft | pp. 34–39 | Courses, Drafts | FT09–FT12, FT16 | `TimetableRepositoryTest` covers persisted selection changes, not all draft controls |
| FR7 Advisor/student review | pp. 51–66, 69–70 | Staff case/student view | FT13 | No current UI execution evidence |
| FR8 Guidance/status/history | pp. 71–74 | Staff Workspace | FT14, FT15 | No current automated coverage found |

**Manual functional log template:** fill the final two columns after execution. The expected result is a check, not a claimed outcome.

| ID / requirement | Preconditions | Steps | Expected result | Actual result / evidence | Status |
| --- | --- | --- | --- | --- | --- |
| FT01 / FR3 | Fresh editable valid draft | Open Review; confirm | One registration and frozen selections saved | — | Not run |
| FT02 / FR3 | Empty draft | Open Review; attempt confirmation | Blocking issue and recovery action; no registration | — | Not run |
| FT03 / FR1, FR5 | Editable plan with nonoverlap groups | Add overlapping group; inspect warning; change group | Clash appears promptly and clears after saved correction | — | Not run |
| FT04 / FR2 | Editable and confirmed examples | Open each timetable | Draft reads current selections; confirmed reads snapshot | — | Not run |
| FT05 / FR3, FR4 | Draft with unmet prerequisite | Review plan | Prerequisite named; confirmation blocked | — | Not run |
| FT06 / FR3, FR4 | Full-group sample | Inspect seats and Review; choose valid alternative | Full group identified; correction revalidates | — | Not run |
| FT07 / FR3 | Already-confirmed draft | Reopen Review/confirmation | Existing saved registration shown; no duplicate | — | Not run |
| FT08 / FR4 | Seeded catalogue | Open course details and compare to source data | Prerequisite and seat values match Room sample | — | Not run |
| FT09 / FR6 | Editable plan | Save a named copy; open it in Drafts | Copy persists; original stays active until explicitly opened | — | Not run |
| FT10 / FR6 | Saved editable draft | Reopen; edit selection; leave and return | Same draft ID and edited selection persist | — | Not run |
| FT11 / FR6 | Editable named draft | Rename; reopen list | New name persists | — | Not run |
| FT12 / FR6 | Disposable audit draft | Delete with confirmation; revisit list | Draft removed; active context remains valid | — | Not run |
| FT13 / FR7 | Seeded case and student | Open case, student registration | Correct name, case, status and saved registration shown read only | — | Not run |
| FT14 / FR8 | Disposable case note | Create, read, edit, delete note | Each change persists; academic records unchanged | — | Not run |
| FT15 / FR8 | Seeded case | Change status; reopen history | New status and timestamped event persist | — | Not run |
| FT16 / FR6 | Saved disposable draft/note | Close and relaunch app | Records remain available | — | Not run |
| FT17 / FR3 | Confirmed registration plus another editable plan | Edit the other plan; revisit saved registration | Confirmed snapshot unchanged | — | Not run |
| FT18 / FR1 | Adjacent classes 09:00–10:00 and 10:00–11:00 | Select both; inspect Review/timetable | No clash for shared endpoint | — | Not run |
| FT19 / FR6 | No selection or unavailable record | Open empty/missing-data states; retry an actual load failure if encountered | Honest empty/error state and recovery; no false success | — | Not run |

## Working-app usability study (group to conduct)

Recruit **at least five** real or proxy users, including student proxies and a staff/advisor proxy where practical. Explain that records are fictional, obtain appropriate consent for notes/screenshots, and anonymise participants as U01–U05. Give each person a task goal without naming the control. Use a disposable audit draft and disposable note to avoid overwriting shared examples; do not require actual registration for every participant. Record device, build revision, observer, start/end time and whether assistance was given.

| Task | Goal | Features covered |
| --- | --- | --- |
| UT01 | Complete or skip onboarding and find a course with suitable seats/prerequisite | Members 1, 2 |
| UT02 | Save a copy, reopen it and change a selection | Member 2 |
| UT03 | Identify a timetable clash and resolve it | Member 3 |
| UT04 | Review a valid/invalid plan and explain what confirmation will do; confirm only on a designated test record | Member 1 |
| UT05 | Review a student case, write/edit guidance and change status | Member 4 |

**Blank participant results sheet (copy once per person and task):**

| Participant code | Real/proxy role | Consent/date | Build/device | Task | Completed independently? | Time | Errors | Assistance | Feedback quote or summary | Issue ID/evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| — | — | — | — | — | — | — | — | — | — | — |

After the sessions, tally independent completion and time for each task, group observed problems by severity, record a fix or justified remaining limitation, then retest fixes. Keep Milestone 02 prototype feedback separate from Milestone 03 working-app results. Do not fill this table from memory or generate invented participants, timings or quotes.

## Evidence and submission actions

1. Install the final APK on an API 24+ device and capture actual working-app screenshots for each member plus key empty, invalid, confirmed and staff-history states. Record revision/device/API.
2. Execute FT01–FT19 on the final build and fill actual results, pass/fail and defect/retest records. Add case logs to the appendix; source tests are not execution evidence.
3. Conduct and document at least five working-app usability sessions using the blank sheet above, then analyse observed issues and fixes.
4. Have each member confirm workload and contribution evidence and prepare a live demonstration of their own interfaces, two genuine CRUD actions where applicable, technology choices and test results. Ask the coordinator how the per-interface CRUD rule applies to onboarding and immutable confirmation/success.
5. Update the consolidated report using actual screenshots, the final repository commit/link, APK location/link, recording links, actual schedule/Gantt data, references and explicit prototype deviations. Remove stale “timetable pending” statements from the earlier draft. Keep the body within 35 pages including cover; references and appendix are excluded. Review and rewrite AI-assisted draft text in the group’s own words as required by the guide.

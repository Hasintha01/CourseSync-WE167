# Member 3 — timetable and clash resolution

Implemented scope: FR1 (clash detection), FR2 (graphical weekly timetable), FR5 (immediate selection feedback). The attached milestone and guide documents supplied requirements and design references; existing project contracts determined integration APIs.

## Implementation

- `feature/timetable/WeeklyTimetableScreen.kt`: active draft loading, weekly grid, conflict evidence, group comparison, persisted replacement and removal, empty and resolved states, and registered timetable.
- `feature/timetable/TimetableLayout.kt`: independent day layout and overlap lanes. Adjacent sessions share a lane. Connected overlap clusters reserve enough lanes to keep every block visible.
- `feature/timetable/ClashWarning.kt`: immediate feedback after Courses updates selections; uses the shared `PlanValidator`.
- Navigation passes the shared repository and active draft into Timetable. Courses receives one timetable callback and the warning component. Other members' feature flows and database schema are preserved.
- Classes are read from Room. Confirmed timetables read `registrationSelections`, with no edit controls. Replacement and removal call the shared transactional repository and reload validation.
- Adding classes uses the existing Courses interface and shared `addSelection` operation. Saving a timetable change edits the active draft; it does not register the student.

## Prototype deviations

| Prototype reference | Implemented change | Reason |
| --- | --- | --- |
| Milestone 02 Figure 11 weekly timetable | Horizontally scrollable day grid and separate overlapping lanes | Keep blocks legible on a phone and prevent one conflicting class covering another. Include weekend columns when needed. |
| Figure 11 clash state; usability ISS-01 | Direct “Resolve clash” actions immediately below conflict evidence, also in Courses | Make recovery discoverable and display new conflicts without waiting for registration review. |
| Figure 11 corrected state | State calculated from stored selections; confirmation before removal; automatic draft save | Functional CRUD and protection against accidental removal. |
| Prototype module codes and credits | Existing shared sample catalog and accurate module count | Maintain the team dataset; shared Course model does not contain credits. |
| Figure 11 navigation | Courses, Timetable and Saved drafts links scoped to timetable | Integrate with existing navigation without replacing other members' screens. |

## Demonstration / manual checks

1. Student → Saved Drafts → reopen `Empty plan` → Home → Timetable: empty guidance and Browse modules.
2. Reopen `Valid plan` → Timetable: Monday CS101 09:00–10:00 and MA101 10:00–11:00, no clash.
3. Reopen `Overlapping classes` → Timetable: red CS101 and MA101 blocks; Monday overlap 09:30–10:00.
4. Resolve MA101 → choose group A: updated Monday 10:00–11:00 block, resolved feedback, no clash. Restart and reopen the draft to verify persistence.
5. In Valid plan, use Courses → MA101 → Change Group → B: immediate clash warning. Resolve clash opens Timetable for the same draft.
6. Remove a class → Keep class leaves it unchanged. Confirm removal refreshes both grid and stored draft. Remove all selections for empty state.
7. In Timetable use Add a module → Courses, add Academic Writing. Reopen Timetable: Friday 09:00–10:00.
8. Open Confirmed example → Timetable: Network Basics from saved registration, no editing controls. A new successful registration's View my timetable also opens its saved classes.
9. Review registration from a draft timetable verifies the same shared issues. A clash-free timetable with unmet prerequisites or unavailable seats still directs the student to registration review.

## Automated checks

`TimetableLayoutTest` covers adjacent blocks, visible overlapping lanes, chained overlap, independent days and empty data. `TimetableRepositoryTest` covers resolving a clash, reopening the on-disk database, removing all classes, adding a class, reading the registered snapshot and rejecting edits to a confirmed draft. Existing validator and repository tests remain in place.

Run with the project's Gradle wrapper: `:app:assembleDebug :app:testDebugUnitTest`; with an emulator or phone: `:app:connectedDebugAndroidTest`.

## Verified results — 9 October 2026

- Debug APK build: passed (`app/build/outputs/apk/debug/app-debug.apk`).
- Unit tests: 7 passed, 0 failures (including 4 timetable layout tests).
- Android emulator tests, Pixel 6 API 36: 6 passed, 0 failures or errors, including 3 timetable UI tests and the on-disk persistence test.
- `git diff --check`: passed.
- `TimetableScreenTest` verifies the clash-to-resolved interaction, empty guidance and confirmed snapshot controls. These automated checks do not substitute for the assignment’s participant usability testing.

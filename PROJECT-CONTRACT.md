# CourseSync shared contract (IT3060 WE_167)

The existing role selector and `CourseSyncApp` routes remain the navigation entry points. Onboarding, review, courses, drafts, and staff screens are implemented. The timetable route implements Member 3's weekly grid and clash resolution. No login or remote service is used. Demo identity is `S1` (Asha Perera).

## Ownership and routes

| Member | Package | Routes | Work |
| --- | --- | --- | --- |
| 1 | `feature.onboarding`, `feature.review` | `Onboarding`, `Review`, `Correction`, `Confirm`, `Success` | First-launch onboarding, review, correction, confirmation, success |
| 2 | `feature.courses` | `Courses`, `Drafts` | Course selection and saved drafts |
| 3 | `feature.timetable` | `Timetable` | Timetable and clash resolution |
| 4 | `feature.staff` | `Staff` | Cases, notes, status, history |

`Roles` and `StudentHome` are shared navigation routes. `CourseSyncApp` retains both Student and Staff entry points. Onboarding completion and the active draft ID are stored in `coursesync_ui` SharedPreferences. Onboarding appears on first launch; Replay onboarding is available from role selection. Courses, Drafts, and Review use the same active draft ID, defaulting to `D-VALID`; reopening a saved draft makes it active. The review draft menu exposes all repeatable scenarios. `shared.model`, `shared.data`, and `shared.validation` are jointly owned.

## Sample data and repeatable scenarios

`SampleData` is the single documented seed source. Room inserts it only in the database creation callback. It never overwrites edits at startup. Student `S1` has no completed courses. Course `CS201` requires `CS101`. Groups use day 1–7 (Monday–Sunday) and minutes after midnight. `CS101-A` is Monday 09:00–10:00; `MA101-A` is Monday 10:00–11:00; `MA101-B` is Monday 09:30–10:30; `UX101-A` has zero seats. Other groups are Tuesday through Friday. There is one open case with one note and one initial history event. One confirmed `REG-1` for `D-SEEDED` holds `NW101-A`, demonstrating a persisted registration and seat count.

| Draft | Selections | Expected result |
| --- | --- | --- |
| `D-VALID` | `CS101-A`, `MA101-A` | Valid; adjacent classes |
| `D-OVERLAP` | `CS101-A`, `MA101-B` | Time overlap |
| `D-PREREQ` | `CS201-A` | Missing `CS101` prerequisite |
| `D-FULL` | `UX101-A` | Full group |
| `D-EMPTY` | None | Empty selection |
| `D-SEEDED` | `NW101-A` | Already confirmed as `REG-1` |

## Repository contract

Create `CourseSyncRepository(CourseSyncDatabase.get(context))` at the app entry point and call its suspend methods from a coroutine. Reads: `students`, `courses`, `groups`, `drafts`, `reopenDraft`, `registrations`, `cases`, `notes`, `caseHistory`. Draft edits: `saveDraft`, `renameDraft`, `deleteDraft`, `addSelection`, `removeSelection`, `changeSelection`, `replaceSelection` (atomic old-course replacement). Registration: `validateDraft`, `confirmRegistration`, `registrationSelections` (the frozen saved snapshot). Staff workflow: `addGuidanceNote`, `updateGuidanceNote`, `deleteGuidanceNote`, `updateCaseStatus`. Invalid IDs and edits to confirmed drafts throw `IllegalArgumentException`; keep UI input handling around those calls. Academic catalog and completed courses have no repository mutation methods.

`confirmRegistration` returns `Confirmed`, `Invalid` with structured issues, or `AlreadyConfirmed`. It validates and writes the registration and frozen selection snapshot in one Room transaction. A unique `draftId` index is an additional duplicate guard. Confirmed drafts cannot be edited or deleted. Group capacity counts confirmed registration selections.

## Validation

`PlanValidator.validate` returns `ValidationResult` with `ValidationIssue(type, courseIds, groupIds)`. It checks empty selection, group-to-course consistency, completed prerequisites, capacity, and pairwise same-day time overlap. Intervals are half open: `[startMinute, endMinute)`, so 09:00–10:00 and 10:00–11:00 are adjacent and valid. Full groups satisfy occupied seats `>= capacity`. No academic data may be changed through the staff repository methods.

Database version is 1. Future schema changes require a Room migration; destructive fallback is intentionally absent so saved data survives app restarts and schema upgrades must be handled explicitly.

## Member 1 integration

Review reads the selected draft, current catalog, groups, and structured validator issues on entry. Returning from `Correction` reloads review and validation. `Confirm` displays the draft and calls `confirmRegistration` again; the button is disabled while the call runs. `Success` reads the persisted `RegistrationSelection` snapshot by registration ID, including on `AlreadyConfirmed`. The prototype shows credit totals, but the shared `Course` model has no credits field, so Member 1 displays accurate module counts only.

`feature.review.CorrectionScreen` receives the affected draft, course, group, and issue type from review. It can change a group, remove a prerequisite-blocked module, replace a full class, or add a module to an empty draft using repository methods. Course selection and group editing are also available in the implemented Courses screen. Review reloads validation on return. `View my timetable` opens Member 3's working timetable for the active draft. Confirmed drafts display the frozen registration snapshot with editing disabled.

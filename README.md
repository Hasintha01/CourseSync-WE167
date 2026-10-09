# CourseSync (WE_167)

CourseSync is an offline Android demonstration for IT3060 Milestone 03. It lets a student select course groups, keep editable drafts, inspect a weekly timetable, resolve clashes, validate a plan, and confirm a registration. A Staff Workspace shows sample cases and registrations and supports guidance notes and case status history.

All students, courses, seats and cases in this build are **fictional sample data**, not live university records. The role selector is a demonstration entry point, not authentication or access control. There is no server, Firebase integration or university API.

## Requirements and setup

- Android Studio with Android SDK Platform 37 and a compatible Android Gradle Plugin 9.4.1 installation.
- The repository's Gradle wrapper (Gradle 9.6) and a JDK supported by that Android Gradle Plugin. Android Studio's bundled JDK was used for the command line build on the project machine.
- An emulator or Android device running API 24 or later to install the debug APK.

Open this directory in Android Studio, allow Gradle sync to finish, select the `app` run configuration and a device, then Run. For a command line build from the repository root:

```bash
./gradlew :app:assembleDebug
```

The installable debug build is at `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected device without clearing existing app data:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`adb install -r` replaces the application package while preserving its data. The app uses a Room database named `coursesync.db`; fictional scenarios are seeded when the database is first created. Reinstalling with data preservation will **not** restore edited scenarios. Do not clear app data if you need to retain drafts, registrations or staff notes.

## Finding the demonstration flows

On first launch, finish or skip onboarding, then choose Student Workspace or Staff Workspace. Onboarding can be replayed from role selection. Student Workspace links to Courses, Saved Drafts, Timetable and Registration Review. The Review draft chooser includes valid, overlap, prerequisite, full group, empty and already-confirmed examples. Courses automatically saves changes to the active editable draft; **Save a copy** creates another named draft and leaves the active one unchanged until opened from Drafts. Review validates the active draft before confirmation. Confirmation stores a read-only registration snapshot, and Success and confirmed timetable read that snapshot. Staff Workspace lets you inspect the fictional student/case, edit guidance notes and change a case status; academic data and student selections are read only there.

## Architecture and scope

Kotlin and Jetpack Compose implement the Android screens. `navigation/CourseSyncApp.kt` holds the route and active draft context. `feature/` contains the four member flows; `ui/theme/` contains shared visual components. `shared/data/CourseSyncRepository.kt` is the single path for persisted actions and Room reads. `shared/validation/PlanValidator.kt` checks empty plans, prerequisites, full groups and overlapping sessions. Room stores drafts, selections, registrations, immutable registration selections, notes and case history locally. SharedPreferences stores onboarding completion and the active draft ID. This local stack supports an installable assignment demonstration without operating a backend or handling real credentials.

The prototype PDF is an untracked design reference under `docs/design reference/`. The current app uses the fictional Room catalogue documented in [PROJECT-CONTRACT.md](PROJECT-CONTRACT.md), which differs from the prototype's sample course codes and credit labels. See [MILESTONE03_READINESS.md](MILESTONE03_READINESS.md) for prototype deviations, requirement traceability, functional case templates and the usability plan. That document is a preparation aid; the group must record actual execution evidence and write its final report in its own words.

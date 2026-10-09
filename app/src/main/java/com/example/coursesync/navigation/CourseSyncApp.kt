package com.example.coursesync.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.feature.courses.DraftsScreen
import com.example.coursesync.feature.courses.StudentCoursesScreen
import com.example.coursesync.feature.onboarding.OnboardingScreen
import com.example.coursesync.feature.review.ConfirmationScreen
import com.example.coursesync.feature.review.CorrectionScreen
import com.example.coursesync.feature.review.RegistrationReviewScreen
import com.example.coursesync.feature.review.SuccessScreen
import com.example.coursesync.feature.staff.StaffWorkspaceScreen
import com.example.coursesync.feature.timetable.WeeklyTimetableScreen
import com.example.coursesync.shared.data.CourseSyncDatabase
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.validation.IssueType
import com.example.coursesync.ui.home.RoleSelectionScreen
import com.example.coursesync.ui.home.StudentHomeScreen
import com.example.coursesync.ui.theme.CourseSyncLogo
import com.example.coursesync.ui.theme.CourseSyncWordmark

private enum class Route(val title: String) {
    Onboarding("Welcome"), Roles("CourseSync"), StudentHome("Student Workspace"),
    Courses("Student Courses"), Drafts("Drafts"), Timetable("Weekly Timetable"),
    Review("Registration Review"), Correction("Correct Selection"),
    Confirm("Confirm Registration"), Success("Registration Complete"), Staff("Staff Workspace")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSyncApp() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("coursesync_ui", Context.MODE_PRIVATE) }
    val repository = remember(context) { CourseSyncRepository(CourseSyncDatabase.get(context)) }
    var routeName by rememberSaveable {
        mutableStateOf(if (preferences.getBoolean("onboarding_complete", false)) Route.Roles.name else Route.Onboarding.name)
    }
    var draftId by rememberSaveable { mutableStateOf(preferences.getString("active_draft", "D-VALID") ?: "D-VALID") }
    var correctionType by rememberSaveable { mutableStateOf(IssueType.EMPTY_SELECTION.name) }
    var affectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var affectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var registrationId by rememberSaveable { mutableStateOf("") }
    var timetableReturnRoute by rememberSaveable { mutableStateOf(Route.StudentHome.name) }
    var coursesReturnRoute by rememberSaveable { mutableStateOf(Route.StudentHome.name) }
    var draftsReturnRoute by rememberSaveable { mutableStateOf(Route.StudentHome.name) }
    var staffBackRequest by remember { mutableIntStateOf(0) }
    val route = Route.entries.firstOrNull { it.name == routeName } ?: Route.Roles
    val openTimetable: (Route) -> Unit = { from ->
        timetableReturnRoute = from.name
        routeName = Route.Timetable.name
    }
    val goBack = {
        routeName = when (route) {
            Route.Correction, Route.Confirm -> Route.Review.name
            Route.Courses -> coursesReturnRoute
            Route.Drafts -> draftsReturnRoute
            Route.Timetable -> timetableReturnRoute
            Route.Review, Route.Success -> Route.StudentHome.name
            Route.StudentHome, Route.Staff -> Route.Roles.name
            Route.Onboarding, Route.Roles -> Route.Roles.name
        }
    }
    BackHandler(enabled = route != Route.Roles && route != Route.Onboarding && route != Route.Staff, onBack = goBack)

    when (route) {
        Route.Onboarding -> OnboardingScreen(onComplete = {
            preferences.edit().putBoolean("onboarding_complete", true).apply()
            routeName = Route.Roles.name
        })
        Route.Review -> RegistrationReviewScreen(repository, draftId,
            onDraftChange = { draftId = it; preferences.edit().putString("active_draft", it).apply() },
            onBack = goBack,
            onCorrect = { issue, courseId, groupId ->
                correctionType = issue.type.name
                affectedCourseId = courseId
                affectedGroupId = groupId
                routeName = Route.Correction.name
            },
            onConfirm = { routeName = Route.Confirm.name },
            onSuccess = { registrationId = it; routeName = Route.Success.name },
            onBrowse = {
                correctionType = IssueType.EMPTY_SELECTION.name
                affectedCourseId = null
                affectedGroupId = null
                routeName = Route.Correction.name
            })
        Route.Correction -> CorrectionScreen(repository, draftId, IssueType.valueOf(correctionType),
            affectedCourseId, affectedGroupId, onBack = goBack, onApplied = { routeName = Route.Review.name })
        Route.Confirm -> ConfirmationScreen(repository, draftId, onBack = goBack,
            onInvalid = { routeName = Route.Review.name },
            onSuccess = { registrationId = it; routeName = Route.Success.name })
        Route.Success -> SuccessScreen(repository, registrationId, onBack = goBack,
            onTimetable = { openTimetable(Route.Success) })
        else -> Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            TopAppBar(title = {
                if (route == Route.Roles) CourseSyncWordmark(22.sp) else Text(route.title)
            }, navigationIcon = {
                if (route == Route.Staff) TextButton(onClick = { staffBackRequest++ }) { Text("Back") }
                else if (route != Route.Roles) TextButton(onClick = goBack) { Text("Back") }
            }, actions = {
                Box(Modifier.padding(end = 16.dp)) { CourseSyncLogo(32.dp, decorative = true) }
            })
        }) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (route) {
                Route.Roles -> RoleSelectionScreen(
                    onStudent = { routeName = Route.StudentHome.name },
                    onStaff = { staffBackRequest = 0; routeName = Route.Staff.name },
                    onReplayOnboarding = { routeName = Route.Onboarding.name },
                    modifier = contentModifier
                )
                Route.StudentHome -> StudentHomeScreen(
                    onCourses = { coursesReturnRoute = Route.StudentHome.name; routeName = Route.Courses.name },
                    onDrafts = { draftsReturnRoute = Route.StudentHome.name; routeName = Route.Drafts.name },
                    onTimetable = { openTimetable(Route.StudentHome) },
                    onReview = { routeName = Route.Review.name },
                    modifier = contentModifier
                )
                Route.Courses -> StudentCoursesScreen(contentModifier, draftId, onDraftChange = {
                    draftId = it
                    preferences.edit().putString("active_draft", it).apply()
                }, onTimetable = { openTimetable(Route.Courses) }, onDrafts = {
                    draftsReturnRoute = Route.Courses.name
                    routeName = Route.Drafts.name
                })
                Route.Drafts -> DraftsScreen(contentModifier, draftId, onDraftChange = {
                    draftId = it
                    preferences.edit().putString("active_draft", it).apply()
                }, onEditCourses = {
                    coursesReturnRoute = Route.Drafts.name
                    routeName = Route.Courses.name
                }, onViewTimetable = { openTimetable(Route.Drafts) })
                Route.Timetable -> WeeklyTimetableScreen(repository, draftId,
                    onBrowse = { coursesReturnRoute = Route.Timetable.name; routeName = Route.Courses.name },
                    onReview = { routeName = Route.Review.name },
                    onDrafts = { draftsReturnRoute = Route.Timetable.name; routeName = Route.Drafts.name },
                    modifier = contentModifier)
                Route.Staff -> StaffWorkspaceScreen(contentModifier, onExit = goBack, backRequest = staffBackRequest)
                else -> Unit
            }
        }
    }
}

package com.example.coursesync.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val route = Route.entries.firstOrNull { it.name == routeName } ?: Route.Roles
    val goBack = {
        routeName = when (route) {
            Route.Correction, Route.Confirm -> Route.Review.name
            Route.Courses, Route.Drafts, Route.Timetable, Route.Review, Route.Success -> Route.StudentHome.name
            Route.StudentHome, Route.Staff -> Route.Roles.name
            Route.Onboarding, Route.Roles -> Route.Roles.name
        }
    }
    BackHandler(enabled = route != Route.Roles && route != Route.Onboarding, onBack = goBack)

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
            onTimetable = { routeName = Route.Timetable.name })
        else -> Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            TopAppBar(title = { Text(route.title) }, navigationIcon = {
                if (route != Route.Roles) TextButton(onClick = goBack) { Text("Back") }
            })
        }) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (route) {
                Route.Roles -> RoleSelectionScreen(
                    onStudent = { routeName = Route.StudentHome.name },
                    onStaff = { routeName = Route.Staff.name },
                    onReplayOnboarding = { routeName = Route.Onboarding.name },
                    modifier = contentModifier
                )
                Route.StudentHome -> StudentHomeScreen(
                    onCourses = { routeName = Route.Courses.name },
                    onDrafts = { routeName = Route.Drafts.name },
                    onTimetable = { routeName = Route.Timetable.name },
                    onReview = { routeName = Route.Review.name },
                    modifier = contentModifier
                )
                Route.Courses -> StudentCoursesScreen(contentModifier)
                Route.Drafts -> DraftsScreen(contentModifier)
                Route.Timetable -> WeeklyTimetableScreen(contentModifier)
                Route.Staff -> StaffWorkspaceScreen(contentModifier)
                else -> Unit
            }
        }
    }
}

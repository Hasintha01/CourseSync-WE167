package com.example.coursesync.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.coursesync.feature.courses.StudentCoursesScreen
import com.example.coursesync.feature.courses.DraftsScreen
import com.example.coursesync.feature.review.RegistrationReviewScreen
import com.example.coursesync.feature.staff.StaffWorkspaceScreen
import com.example.coursesync.feature.timetable.WeeklyTimetableScreen
import com.example.coursesync.ui.home.RoleSelectionScreen
import com.example.coursesync.ui.home.StudentHomeScreen

private enum class Route(val title: String) {
    Roles("CourseSync"),
    StudentHome("Student Workspace"),
    Courses("Student Courses"),
    Drafts("Drafts"),
    Timetable("Weekly Timetable"),
    Review("Registration Review"),
    Staff("Staff Workspace")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSyncApp() {
    var routeName by rememberSaveable { mutableStateOf(Route.Roles.name) }
    val route = Route.entries.firstOrNull { it.name == routeName } ?: Route.Roles
    val goBack = {
        routeName = when (route) {
            Route.Courses, Route.Drafts, Route.Timetable, Route.Review -> Route.StudentHome.name
            Route.StudentHome, Route.Staff -> Route.Roles.name
            Route.Roles -> Route.Roles.name
        }
    }
    BackHandler(enabled = route != Route.Roles, onBack = goBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(route.title) },
                navigationIcon = {
                    if (route != Route.Roles) {
                        TextButton(onClick = goBack) { Text("Back") }
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (route) {
            Route.Roles -> RoleSelectionScreen(
                onStudent = { routeName = Route.StudentHome.name },
                onStaff = { routeName = Route.Staff.name },
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
            Route.Review -> RegistrationReviewScreen(contentModifier)
            Route.Staff -> StaffWorkspaceScreen(contentModifier)
        }
    }
}

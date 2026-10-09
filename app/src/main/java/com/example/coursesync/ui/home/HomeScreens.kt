package com.example.coursesync.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.coursesync.ui.theme.AppPrimaryButton as Button
import androidx.compose.material3.MaterialTheme
import com.example.coursesync.ui.theme.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coursesync.ui.theme.PrototypeStyle

@Composable
fun RoleSelectionScreen(onStudent: () -> Unit, onStaff: () -> Unit, onReplayOnboarding: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text("Welcome to CourseSync", style = MaterialTheme.typography.headlineMedium)
        Text("Choose a workspace to continue.")
        Button(onClick = onStudent, modifier = Modifier.fillMaxWidth()) { Text("Student") }
        OutlinedButton(onClick = onStaff, modifier = Modifier.fillMaxWidth()) { Text("Staff") }
        OutlinedButton(onClick = onReplayOnboarding, modifier = Modifier.fillMaxWidth()) { Text("Replay onboarding") }
    }
}

@Composable
fun StudentHomeScreen(
    onCourses: () -> Unit,
    onDrafts: () -> Unit,
    onTimetable: () -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenColumn(modifier) {
        Text("Select an area to explore.")
        Button(onClick = onCourses, modifier = Modifier.fillMaxWidth()) { Text("Student Courses") }
        Button(onClick = onDrafts, modifier = Modifier.fillMaxWidth()) { Text("Drafts") }
        Button(onClick = onTimetable, modifier = Modifier.fillMaxWidth()) { Text("Weekly Timetable") }
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Registration Review") }
    }
}

@Composable
fun PlaceholderScreen(title: String, description: String, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(description)
        Text("This area is ready for the next development step.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ScreenColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = PrototypeStyle.pagePadding, vertical = PrototypeStyle.sectionSpacing),
        verticalArrangement = Arrangement.spacedBy(PrototypeStyle.sectionSpacing),
        content = { content() }
    )
}

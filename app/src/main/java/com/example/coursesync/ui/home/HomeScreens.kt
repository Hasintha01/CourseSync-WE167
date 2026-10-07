package com.example.coursesync.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoleSelectionScreen(onStudent: () -> Unit, onStaff: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text("Welcome to CourseSync", style = MaterialTheme.typography.headlineMedium)
        Text("Choose a workspace to continue.")
        Button(onClick = onStudent, modifier = Modifier.fillMaxWidth()) { Text("Student") }
        OutlinedButton(onClick = onStaff, modifier = Modifier.fillMaxWidth()) { Text("Staff") }
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
        Text("Student Workspace", style = MaterialTheme.typography.headlineMedium)
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
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = { content() }
    )
}

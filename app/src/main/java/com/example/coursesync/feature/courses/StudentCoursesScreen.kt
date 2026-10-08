package com.example.coursesync.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.coursesync.shared.data.CourseSyncDatabase
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.ClassGroup
import com.example.coursesync.shared.model.Course
import com.example.coursesync.shared.model.Draft
import com.example.coursesync.shared.model.DraftSelection
import kotlinx.coroutines.launch

@Composable
fun StudentCoursesScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val repository = remember {
        CourseSyncRepository(
            CourseSyncDatabase.get(context)
        )
    }

    val scope = rememberCoroutineScope()

    val studentId = "S1"

    var courses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }

    var groups by remember {
        mutableStateOf<List<ClassGroup>>(emptyList())
    }

    var currentDraft by remember {
        mutableStateOf<Draft?>(null)
    }

    var confirmedDraftIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var selections by remember {
        mutableStateOf<List<DraftSelection>>(emptyList())
    }

    var selectedCourse by remember {
        mutableStateOf<Course?>(null)
    }

    var changeSelection by remember {
        mutableStateOf<DraftSelection?>(null)
    }

    var showSaveDialog by remember {
        mutableStateOf(false)
    }

    var draftName by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        confirmedDraftIds = repository.registrations(studentId).map { it.draftId }.toSet()
        courses = repository.courses()
        groups = repository.groups()

        val existing = repository.drafts(studentId)
            .firstOrNull { it.name == "Current Plan" && it.id !in confirmedDraftIds }

        currentDraft = existing

        if (existing != null) {
            selections = repository
                .reopenDraft(existing.id)
                ?.second
                ?: emptyList()
        }
    }

    fun reloadPlan() {
        scope.launch {
            val draft = currentDraft

            if (draft != null) {
                selections =
                    repository.reopenDraft(draft.id)?.second
                        ?: emptyList()
            }
        }
    }

    fun getOrCreateCurrentDraft(onReady: (Draft) -> Unit) {
        scope.launch {

            val confirmedIds = repository.registrations(studentId).map { it.draftId }.toSet()
            confirmedDraftIds = confirmedIds

            val existing = currentDraft
                ?.takeIf { it.id !in confirmedIds }
                ?: repository.drafts(studentId)
                    .firstOrNull { it.name == "Current Plan" && it.id !in confirmedIds }

            val draft = existing
                ?: repository.saveDraft(
                    studentId = studentId,
                    name = "Current Plan"
                )

            currentDraft = draft
            onReady(draft)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Course Selection",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Browse courses and build your current registration plan.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        /*
         * CURRENT PLAN
         */

        Text(
            text = "Current Plan",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selections.isEmpty()) {

            Text(
                text = "No courses selected yet."
            )

        } else {

            selections.forEach { selection ->

                val course = courses.firstOrNull {
                    it.id == selection.courseId
                }

                val group = groups.firstOrNull {
                    it.id == selection.groupId
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = course?.title
                                ?: selection.courseId,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "${selection.courseId} • Group ${group?.label ?: selection.groupId}"
                        )

                        if (group != null) {
                            Text(
                                text = "Day ${group.day}: ${
                                    formatTime(group.startMinute)
                                } - ${
                                    formatTime(group.endMinute)
                                }"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedButton(
                                onClick = {
                                    changeSelection = selection
                                }
                            ) {
                                Text("Change Group")
                            }

                            OutlinedButton(
                                onClick = {

                                    val draft = currentDraft

                                    if (draft != null) {
                                        scope.launch {

                                            repository.removeSelection(
                                                draftId = draft.id,
                                                courseId = selection.courseId
                                            )

                                            reloadPlan()
                                        }
                                    }
                                }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = selections.isNotEmpty(),
            onClick = {
                draftName = ""
                showSaveDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Draft")
        }

        Spacer(modifier = Modifier.height(20.dp))

        /*
         * COURSE CATALOGUE
         */

        Text(
            text = "Course Catalogue",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(
                items = courses,
                key = { it.id }
            ) { course ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = course.id,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (course.prerequisiteId != null) {
                                "Prerequisite: ${course.prerequisiteId}"
                            } else {
                                "No prerequisite"
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                selectedCourse = course
                            }
                        ) {
                            Text("View Details")
                        }
                    }
                }
            }
        }
    }

    /*
     * COURSE DETAILS
     */

    selectedCourse?.let { course ->

        val courseGroups = groups.filter {
            it.courseId == course.id
        }

        AlertDialog(
            onDismissRequest = {
                selectedCourse = null
            },
            title = {
                Text("${course.id} - ${course.title}")
            },
            text = {

                Column {

                    Text(
                        text = if (course.prerequisiteId != null) {
                            "Prerequisite: ${course.prerequisiteId}"
                        } else {
                            "No prerequisite"
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Available Class Groups",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    courseGroups.forEach { group ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(10.dp)
                            ) {

                                Text(
                                    text = "Group ${group.label}"
                                )

                                Text(
                                    text = "Day ${group.day}: ${
                                        formatTime(group.startMinute)
                                    } - ${
                                        formatTime(group.endMinute)
                                    }"
                                )

                                Text(
                                    text = "Capacity: ${group.capacity}"
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Button(
                                    enabled = group.capacity > 0,
                                    onClick = {

                                        getOrCreateCurrentDraft { draft ->

                                            scope.launch {

                                                repository.addSelection(
                                                    draftId = draft.id,
                                                    courseId = course.id,
                                                    groupId = group.id
                                                )

                                                reloadPlan()
                                                selectedCourse = null
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        if (group.capacity > 0) {
                                            "Add to Plan"
                                        } else {
                                            "Full"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedCourse = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    /*
     * CHANGE GROUP
     */

    changeSelection?.let { selection ->

        val courseGroups = groups.filter {
            it.courseId == selection.courseId
        }

        AlertDialog(
            onDismissRequest = {
                changeSelection = null
            },
            title = {
                Text("Change Class Group")
            },
            text = {

                Column {

                    Text(
                        text = "Select another group for ${selection.courseId}"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    courseGroups.forEach { group ->

                        OutlinedButton(
                            enabled = group.capacity > 0,
                            onClick = {

                                val draft = currentDraft

                                if (draft != null) {

                                    scope.launch {

                                        repository.changeSelection(
                                            draftId = draft.id,
                                            courseId = selection.courseId,
                                            newGroupId = group.id
                                        )

                                        reloadPlan()
                                        changeSelection = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "Group ${group.label} • Day ${group.day} • ${
                                    formatTime(group.startMinute)
                                }-${
                                    formatTime(group.endMinute)
                                }"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        changeSelection = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    /*
     * SAVE DRAFT
     */

    if (showSaveDialog) {

        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
            },
            title = {
                Text("Save Draft")
            },
            text = {

                OutlinedTextField(
                    value = draftName,
                    onValueChange = {
                        draftName = it
                    },
                    label = {
                        Text("Draft name")
                    },
                    placeholder = {
                        Text("Example: Semester 2 Plan")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {

                TextButton(
                    enabled = draftName.isNotBlank(),
                    onClick = {

                        val trimmedName = draftName.trim()

                        scope.launch {

                            repository.saveDraft(
                                studentId = studentId,
                                name = trimmedName,
                                selections = selections
                            )

                            showSaveDialog = false
                            draftName = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showSaveDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTime(minutes: Int): String {

    val hour = minutes / 60
    val minute = minutes % 60

    return String.format(
        "%02d:%02d",
        hour,
        minute
    )
}

package com.example.coursesync.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.example.coursesync.ui.theme.AppPrimaryButton as Button
import androidx.compose.material3.MaterialTheme
import com.example.coursesync.ui.theme.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.coursesync.feature.timetable.ClashWarning
import com.example.coursesync.shared.data.CourseSyncDatabase
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.ClassGroup
import com.example.coursesync.shared.model.Course
import com.example.coursesync.shared.model.Draft
import com.example.coursesync.shared.model.DraftSelection
import com.example.coursesync.ui.theme.AppCard
import com.example.coursesync.ui.theme.PrototypeStyle
import com.example.coursesync.ui.theme.readableDay
import kotlinx.coroutines.launch

@Composable
fun StudentCoursesScreen(
    modifier: Modifier = Modifier,
    activeDraftId: String,
    onDraftChange: (String) -> Unit,
    onTimetable: () -> Unit = {},
    onDrafts: () -> Unit = {}
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
    var saveError by remember { mutableStateOf<String?>(null) }
    var changeError by remember { mutableStateOf<String?>(null) }
    var attemptedGroupId by remember { mutableStateOf<String?>(null) }
    var removePending by remember { mutableStateOf<DraftSelection?>(null) }
    var removeError by remember { mutableStateOf<String?>(null) }
    var savedCopy by remember { mutableStateOf<Draft?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var remainingSeats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    suspend fun refresh() {
        confirmedDraftIds = repository.registrations(studentId).map { it.draftId }.toSet()
        courses = repository.courses()
        groups = repository.groups()
        remainingSeats = repository.remainingSeats(groups)
        val pair = repository.reopenDraft(activeDraftId)
        currentDraft = pair?.first
        selections = pair?.second.orEmpty()
    }

    LaunchedEffect(activeDraftId) { refresh() }

    fun reloadPlan() {
        scope.launch {
            val draft = currentDraft

            if (draft != null) {
                selections = repository.reopenDraft(draft.id)?.second.orEmpty()
                remainingSeats = repository.remainingSeats(groups)
            }
        }
    }

    fun getOrCreateCurrentDraft(onReady: (Draft) -> Unit) {
        scope.launch {
            try {
                val confirmedIds = repository.registrations(studentId).map { it.draftId }.toSet()
                confirmedDraftIds = confirmedIds
                val draft = currentDraft?.takeIf { it.id !in confirmedIds }
                    ?: repository.saveDraft(studentId = studentId, name = "Current Plan")
                currentDraft = draft
                if (draft.id != activeDraftId) onDraftChange(draft.id)
                onReady(draft)
            } catch (exception: Exception) {
                message = exception.message ?: "Could not open an editable draft"
                busy = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PrototypeStyle.pagePadding, vertical = PrototypeStyle.sectionSpacing)
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
            text = "Active draft: ${currentDraft?.name ?: "Choose a draft in Saved Drafts"}",
            style = MaterialTheme.typography.titleLarge
        )
        if (currentDraft?.id in confirmedDraftIds) Text("Confirmed registration • Read only")
        else Text("Changes to this active draft save automatically.", color = PrototypeStyle.muted,
            style = MaterialTheme.typography.bodySmall)
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        savedCopy?.let { copy ->
            Text("Copy created: ${copy.name}. Your active draft is still ${currentDraft?.name ?: "unchanged"}.",
                color = PrototypeStyle.ink)
            TextButton(onClick = { onDraftChange(copy.id); onDrafts() }) { Text("Open in Drafts") }
        }
        if (currentDraft?.id !in confirmedDraftIds) ClashWarning(selections, groups, courses, onTimetable)

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

                AppCard(
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
                                text = "${readableDay(group.day)}: ${
                                    formatTime(group.startMinute)
                                } - ${
                                    formatTime(group.endMinute)
                                }"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedButton(
                                enabled = currentDraft?.id !in confirmedDraftIds && !busy,
                                onClick = {
                                    changeSelection = selection
                                }
                            ) {
                                Text("Change Group")
                            }

                            OutlinedButton(
                                enabled = currentDraft?.id !in confirmedDraftIds && !busy,
                                onClick = {

                                    val draft = currentDraft

                                    if (draft != null) {
                                        removeError = null
                                        removePending = selection
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
            enabled = selections.isNotEmpty() && !busy,
            onClick = {
                draftName = ""
                saveError = null
                showSaveDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save a copy")
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            courses.forEach { course ->
                AppCard(modifier = Modifier.fillMaxWidth()) {

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
            onDismissRequest = { if (!busy) selectedCourse = null },
            title = {
                Text("${course.id} - ${course.title}")
            },
            text = {

                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {

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

                        AppCard(
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
                                    text = "${readableDay(group.day)}: ${
                                        formatTime(group.startMinute)
                                    } - ${
                                        formatTime(group.endMinute)
                                    }"
                                )

                                val remaining = remainingSeats[group.id] ?: 0
                                Text("$remaining of ${group.capacity} seats available")

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Button(
                                    enabled = remaining > 0 && !busy,
                                    onClick = {

                                        busy = true
                                        getOrCreateCurrentDraft { draft ->

                                            scope.launch {

                                                try {
                                                    repository.addSelection(draft.id, course.id, group.id)
                                                    reloadPlan()
                                                    selectedCourse = null
                                                    message = "${course.title} added to ${draft.name}."
                                                } catch (exception: Exception) {
                                                    message = exception.message ?: "Could not add module"
                                                }
                                                busy = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        if (remaining > 0) {
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
                    enabled = !busy,
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
                if (!busy) { changeSelection = null; changeError = null; attemptedGroupId = null }
            },
            title = {
                Text("Change Class Group")
            },
            text = {

                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {

                    Text(
                        text = "Select another group for ${selection.courseId}"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    courseGroups.forEach { group ->

                        OutlinedButton(
                            enabled = (remainingSeats[group.id] ?: 0) > 0 && !busy,
                            onClick = {
                                attemptedGroupId = group.id
                                changeError = null

                                val draft = currentDraft

                                if (draft != null && !busy) {
                                    busy = true
                                    scope.launch {
                                        try {
                                            repository.changeSelection(draft.id, selection.courseId, group.id)
                                            reloadPlan()
                                            changeSelection = null
                                            attemptedGroupId = null
                                            message = "Group changed to ${group.label}."
                                        } catch (exception: Exception) {
                                            changeError = exception.message ?: "Could not change group"
                                        }
                                        busy = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "${if (busy && attemptedGroupId == group.id) "Changing… " else if (attemptedGroupId == group.id) "Selected • " else ""}Group ${group.label} • ${readableDay(group.day)} • ${
                                    formatTime(group.startMinute)
                                }-${
                                    formatTime(group.endMinute)
                                }"
                            )
                        }
                    }
                    changeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        changeSelection = null
                        changeError = null
                        attemptedGroupId = null
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
            onDismissRequest = { if (!busy) showSaveDialog = false },
            title = {
                Text("Save a copy")
            },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Changes to ${currentDraft?.name ?: "your active draft"} are already saved. Name a separate copy; the active draft will stay the same.")
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it; saveError = null },
                        label = { Text("Draft name") },
                        placeholder = { Text("Example: Semester 2 Plan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {

                TextButton(
                    enabled = draftName.isNotBlank() && !busy,
                    onClick = {

                        val trimmedName = draftName.trim()
                        saveError = null
                        busy = true
                        scope.launch {
                            try {
                                val saved = repository.saveDraft(studentId, trimmedName, selections)
                                showSaveDialog = false
                                draftName = ""
                                savedCopy = saved
                                message = "Saved a copy named ${saved.name} with ${selections.size} ${if (selections.size == 1) "module" else "modules"}."
                            } catch (exception: Exception) {
                                saveError = exception.message ?: "Could not save draft"
                            }
                            busy = false
                        }
                    }
                ) {
                    Text(if (busy) "Saving…" else "Save")
                }
            },
            dismissButton = {

                TextButton(
                    enabled = !busy,
                    onClick = {
                        showSaveDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    removePending?.let { selection ->
        val draft = currentDraft
        val courseName = courses.firstOrNull { it.id == selection.courseId }?.title ?: selection.courseId
        AlertDialog(
            onDismissRequest = { if (!busy) { removePending = null; removeError = null } },
            title = { Text("Remove $courseName?") },
            text = {
                Column {
                    Text("Remove this module from ${draft?.name ?: "the active draft"}? This change saves immediately.")
                    removeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && draft != null, onClick = {
                    if (busy || draft == null) return@TextButton
                    busy = true
                    removeError = null
                    scope.launch {
                        try {
                            repository.removeSelection(draft.id, selection.courseId)
                            reloadPlan()
                            removePending = null
                            message = "$courseName removed from ${draft.name}."
                        } catch (exception: Exception) {
                            removeError = exception.message ?: "Could not remove module"
                        } finally { busy = false }
                    }
                }) { Text(if (busy) "Removing…" else "Remove") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { removePending = null; removeError = null }) { Text("Keep module") } }
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

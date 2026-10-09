package com.example.coursesync.feature.staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.example.coursesync.ui.theme.AppPrimaryButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.example.coursesync.ui.theme.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.coursesync.shared.data.CourseSyncDatabase
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.CaseHistory
import com.example.coursesync.shared.model.CaseStatus
import com.example.coursesync.shared.model.GuidanceNote
import com.example.coursesync.shared.model.StaffCase
import com.example.coursesync.shared.model.Student
import com.example.coursesync.ui.theme.AppCard
import com.example.coursesync.ui.theme.PrototypeStyle
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

/** Member 4's staff workflow, connected to the team's shared repository. */
@Composable
fun StaffWorkspaceScreen(modifier: Modifier = Modifier, onExit: () -> Unit, backRequest: Int = 0) {
    val context = LocalContext.current
    val repository = remember(context) { CourseSyncRepository(CourseSyncDatabase.get(context)) }
    val scope = rememberCoroutineScope()
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var cases by remember { mutableStateOf<List<StaffCase>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(StaffTab.CASES) }
    var selectedCase by remember { mutableStateOf<StaffCase?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var returnToCase by remember { mutableStateOf<StaffCase?>(null) }
    var notes by remember { mutableStateOf<List<GuidanceNote>>(emptyList()) }
    var history by remember { mutableStateOf<List<CaseHistory>>(emptyList()) }
    var registrations by remember { mutableStateOf<List<String>>(emptyList()) }
    var editingNote by remember { mutableStateOf<GuidanceNote?>(null) }
    var noteText by remember { mutableStateOf("") }
    var showNoteEditor by remember { mutableStateOf(false) }
    var deletingNote by remember { mutableStateOf<GuidanceNote?>(null) }
    var showStatusEditor by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var noteSaveError by remember { mutableStateOf<String?>(null) }
    var statusError by remember { mutableStateOf<String?>(null) }

    val goBack: () -> Unit = {
        when {
            selectedCase != null -> selectedCase = null
            selectedStudent != null -> { selectedStudent = null; selectedCase = returnToCase; returnToCase = null }
            else -> onExit()
        }
    }
    BackHandler(onBack = goBack)
    LaunchedEffect(backRequest) { if (backRequest > 0) goBack() }

    LaunchedEffect(Unit) {
        students = repository.students()
        cases = repository.cases()
    }
    LaunchedEffect(selectedCase?.id) {
        selectedCase?.let {
            notes = repository.notes(it.id)
            history = repository.caseHistory(it.id)
        }
    }
    LaunchedEffect(selectedStudent?.id) {
        selectedStudent?.let { student ->
            val courses = repository.courses().associateBy { it.id }
            registrations = repository.registrations(student.id).flatMap { registration ->
                repository.registrationSelections(registration.id).map { selection ->
                    "${courses[selection.courseId]?.title ?: selection.courseId} • ${selection.groupId}"
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = PrototypeStyle.pagePadding, vertical = PrototypeStyle.sectionSpacing),
        verticalArrangement = Arrangement.spacedBy(PrototypeStyle.sectionSpacing)
    ) {
        Text("Review student registrations and manage guidance cases.")
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        when {
            selectedCase != null -> {
                val staffCase = selectedCase!!
                Text(staffCase.subject, style = MaterialTheme.typography.titleLarge)
                val caseStudent = students.firstOrNull { it.id == staffCase.studentId }
                Text("${staffCase.id} • ${caseStudent?.name ?: "Student ${staffCase.studentId}"}")
                if (caseStudent != null) TextButton(onClick = {
                    returnToCase = staffCase
                    selectedCase = null
                    selectedStudent = caseStudent
                }) { Text("View student registration") }
                Text("Status: ${staffCase.status.displayName()}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        editingNote = null
                        noteText = ""
                        noteSaveError = null
                        showNoteEditor = true
                    }) { Text("Add note") }
                    OutlinedButton(onClick = { showStatusEditor = true }) { Text("Update status") }
                }
                Text("Guidance notes", style = MaterialTheme.typography.titleMedium)
                if (notes.isEmpty()) Text("No guidance notes yet.")
                notes.forEach { note ->
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(note.text)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    editingNote = note
                                    noteText = note.text
                                    noteSaveError = null
                                    showNoteEditor = true
                                }) { Text("Edit") }
                                OutlinedButton(onClick = { deletingNote = note }) { Text("Delete") }
                            }
                        }
                    }
                }
                Text("Case history", style = MaterialTheme.typography.titleMedium)
                if (history.isEmpty()) Text("No status changes yet.")
                history.forEach { event ->
                    val timeLabel = if (event.changedAt <= 1L) "Time not recorded" else
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(event.changedAt))
                    Text("${event.fromStatus?.displayName() ?: "Created"} → ${event.toStatus.displayName()} • $timeLabel")
                }
            }
            selectedStudent != null -> {
                val student = selectedStudent!!
                Text(student.name, style = MaterialTheme.typography.titleLarge)
                Text("Student ID: ${student.id}")
                Text("Confirmed registration", style = MaterialTheme.typography.titleMedium)
                if (registrations.isEmpty()) Text("No confirmed courses.")
                registrations.forEach { Text(it) }
                cases.filter { it.studentId == student.id }.forEach { staffCase ->
                    OutlinedButton(onClick = { selectedStudent = null; selectedCase = staffCase }) {
                        Text("Open ${staffCase.id}: ${staffCase.subject}")
                    }
                }
            }
            else -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StaffTab.entries.forEach { tab ->
                        FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab.label) })
                    }
                }
                if (selectedTab == StaffTab.CASES) {
                    if (cases.isEmpty()) Text("No cases available.")
                    cases.forEach { staffCase ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(staffCase.subject, style = MaterialTheme.typography.titleMedium)
                                Text("${staffCase.id} • ${staffCase.status.displayName()}")
                                Text("Student ${students.firstOrNull { it.id == staffCase.studentId }?.name ?: staffCase.studentId}")
                                Button(onClick = { selectedCase = staffCase }) { Text("Open case") }
                            }
                        }
                    }
                } else {
                    if (students.isEmpty()) Text("No students available.")
                    students.forEach { student ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(student.name, style = MaterialTheme.typography.titleMedium)
                                Text(student.id)
                                Button(onClick = { returnToCase = null; selectedStudent = student }) { Text("View registration") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteEditor && selectedCase != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) showNoteEditor = false },
            title = { Text(if (editingNote == null) "Add guidance note" else "Edit guidance note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(noteText, { noteText = it; noteSaveError = null },
                        label = { Text("Guidance") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    noteSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = noteText.isNotBlank() && !busy, onClick = {
                    val caseId = selectedCase!!.id
                    val existing = editingNote
                    noteSaveError = null
                    busy = true
                    scope.launch {
                        try {
                            if (existing == null) repository.addGuidanceNote(caseId, noteText)
                            else repository.updateGuidanceNote(existing, noteText)
                            notes = repository.notes(caseId)
                            showNoteEditor = false
                            message = "Guidance note saved."
                        } catch (exception: Exception) { noteSaveError = exception.message ?: "Could not save note" }
                        busy = false
                    }
                }) { Text(if (busy) "Saving…" else "Save") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { showNoteEditor = false }) { Text("Cancel") } }
        )
    }
    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text("Delete guidance note?") },
            text = { Text("This note will be removed from the case.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            repository.deleteGuidanceNote(note.caseId, note.id)
                            notes = repository.notes(note.caseId)
                            deletingNote = null
                            message = "Guidance note deleted."
                        } catch (exception: Exception) { message = exception.message ?: "Could not delete note" }
                        busy = false
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deletingNote = null }) { Text("Cancel") } }
        )
    }
    if (showStatusEditor && selectedCase != null) {
        AlertDialog(
            onDismissRequest = { showStatusEditor = false },
            title = { Text("Update case status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    CaseStatus.entries.forEach { status ->
                        OutlinedButton(enabled = status != selectedCase?.status && !busy, onClick = {
                            val staffCase = selectedCase!!
                            statusError = null
                            busy = true
                            scope.launch {
                                try {
                                    repository.updateCaseStatus(staffCase.id, status)
                                    cases = repository.cases()
                                    selectedCase = cases.firstOrNull { it.id == staffCase.id }
                                    history = repository.caseHistory(staffCase.id)
                                    showStatusEditor = false
                                    message = "Case status updated to ${status.displayName()}."
                                } catch (exception: Exception) { statusError = exception.message ?: "Could not update status" }
                                busy = false
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Updating…" else status.displayName()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStatusEditor = false }) { Text("Cancel") } }
        )
    }
}

private enum class StaffTab(val label: String) { CASES("Cases"), STUDENTS("Students") }
private fun CaseStatus.displayName(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

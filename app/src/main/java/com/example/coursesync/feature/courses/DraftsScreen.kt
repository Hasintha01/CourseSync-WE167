package com.example.coursesync.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
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
import com.example.coursesync.shared.model.Draft
import com.example.coursesync.shared.model.DraftSelection
import kotlinx.coroutines.launch

@Composable
fun DraftsScreen(
    modifier: Modifier = Modifier,
    activeDraftId: String,
    onDraftChange: (String) -> Unit
) {
    val context = LocalContext.current

    val database = remember {
        CourseSyncDatabase.get(context)
    }

    val repository = remember {
        CourseSyncRepository(database)
    }

    val scope = rememberCoroutineScope()

    val studentId = "S1"

    var drafts by remember {
        mutableStateOf<List<Draft>>(emptyList())
    }

    var confirmedDraftIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var selectedDraft by remember {
        mutableStateOf<Draft?>(null)
    }

    var selectedSelections by remember {
        mutableStateOf<List<DraftSelection>>(emptyList())
    }

    var showRenameDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var newName by remember {
        mutableStateOf("")
    }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun loadDrafts() {
        scope.launch {
            confirmedDraftIds = repository.registrations(studentId).map { it.draftId }.toSet()
            drafts = repository.drafts(studentId)
        }
    }

    LaunchedEffect(Unit) {
        loadDrafts()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Saved Drafts",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Open, rename or delete your saved course selections.",
            style = MaterialTheme.typography.bodyMedium
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Spacer(modifier = Modifier.height(16.dp))

        if (drafts.isEmpty()) {

            Text(
                text = "No saved drafts found.",
                style = MaterialTheme.typography.bodyLarge
            )

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = drafts,
                    key = { it.id }
                ) { draft ->

                    val isConfirmed = draft.id in confirmedDraftIds

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = draft.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (draft.id == activeDraftId) Text("Active draft", color = MaterialTheme.colorScheme.primary)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Draft ID: ${draft.id}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (isConfirmed) {
                                Text("Confirmed registration • Read only")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    enabled = !busy,
                                    onClick = {
                                        scope.launch {
                                            val result =
                                                repository.reopenDraft(draft.id)

                                            if (result != null) {
                                                selectedDraft = result.first
                                                selectedSelections = result.second
                                                onDraftChange(draft.id)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Open")
                                }

                                OutlinedButton(
                                    enabled = !isConfirmed && !busy,
                                    onClick = {
                                        selectedDraft = draft
                                        newName = draft.name
                                        showRenameDialog = true
                                    }
                                ) {
                                    Text("Rename")
                                }

                                OutlinedButton(
                                    enabled = !isConfirmed && !busy,
                                    onClick = {
                                        selectedDraft = draft
                                        showDeleteDialog = true
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Open draft dialog
    selectedDraft?.let { draft ->

        if (!showRenameDialog && !showDeleteDialog) {

            AlertDialog(
                onDismissRequest = {
                    selectedDraft = null
                    selectedSelections = emptyList()
                },
                title = {
                    Text(draft.name)
                },
                text = {
                    Column {

                        Text(
                            text = "Selected courses: ${selectedSelections.size}"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        selectedSelections.forEach { selection ->

                            Text(
                                text = "${selection.courseId} - Group ${selection.groupId}"
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedDraft = null
                            selectedSelections = emptyList()
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }

    // Rename dialog
    if (showRenameDialog && selectedDraft != null) {

        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                selectedDraft = null
            },
            title = {
                Text("Rename Draft")
            },
            text = {

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                    },
                    label = {
                        Text("Draft name")
                    },
                    singleLine = true
                )
            },
            confirmButton = {

                TextButton(
                    enabled = newName.isNotBlank() && !busy,
                    onClick = {

                        val draft = selectedDraft ?: return@TextButton

                        scope.launch {
                            busy = true
                            try {
                                repository.renameDraft(draft.id, newName)
                                showRenameDialog = false
                                selectedDraft = null
                                loadDrafts()
                                message = "Draft renamed."
                            } catch (exception: Exception) {
                                message = exception.message ?: "Could not rename draft"
                            }
                            busy = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {

                TextButton(
                    enabled = !busy,
                    onClick = {
                        showRenameDialog = false
                        selectedDraft = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation
    if (showDeleteDialog && selectedDraft != null) {

        val draft = selectedDraft!!

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedDraft = null
            },
            title = {
                Text("Delete Draft?")
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${draft.name}\"? " +
                            "Its saved course selections will also be removed."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        scope.launch {
                            busy = true
                            try {
                                repository.deleteDraft(draft.id)
                                showDeleteDialog = false
                                selectedDraft = null
                                loadDrafts()
                                if (activeDraftId == draft.id) {
                                    val next = repository.drafts(studentId).firstOrNull()
                                        ?: repository.saveDraft(studentId, "New plan")
                                    onDraftChange(next.id)
                                }
                                message = "Draft deleted."
                            } catch (exception: Exception) {
                                message = exception.message ?: "Could not delete draft"
                            }
                            busy = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedDraft = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

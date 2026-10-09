package com.example.coursesync.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coursesync.shared.data.CourseSyncRepository
import kotlinx.coroutines.launch

@Composable
internal fun NewRegistrationAction(
    repository: CourseSyncRepository,
    studentId: String,
    onCreated: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TextButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Start another registration")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!saving) showDialog = false },
            title = { Text("Start another registration") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a new editable plan. Your confirmed registration stays saved and unchanged.")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = { Text("New plan name") },
                        placeholder = { Text("Example: Second semester plan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && !saving, onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            val draft = repository.saveDraft(studentId, name.trim())
                            showDialog = false
                            onCreated(draft.id)
                        } catch (exception: Exception) {
                            error = exception.message ?: "Could not create the plan"
                        } finally {
                            saving = false
                        }
                    }
                }) { Text(if (saving) "Creating…" else "Create plan") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

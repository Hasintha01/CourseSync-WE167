package com.example.coursesync.feature.review

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.Draft
import com.example.coursesync.shared.validation.ValidationIssue
import com.example.coursesync.ui.theme.PrototypeStyle as P

@Composable
fun RegistrationReviewScreen(
    repository: CourseSyncRepository, draftId: String, onDraftChange: (String) -> Unit,
    onBack: () -> Unit, onCorrect: (ValidationIssue, String?, String?) -> Unit,
    onConfirm: () -> Unit, onSuccess: (String) -> Unit, onBrowse: () -> Unit
) {
    var plan by remember(draftId) { mutableStateOf<PlanSnapshot?>(null) }
    var drafts by remember { mutableStateOf<List<Draft>>(emptyList()) }
    var loading by remember(draftId) { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember(draftId) { mutableStateOf<String?>(null) }
    var retry by remember(draftId) { mutableIntStateOf(0) }
    LaunchedEffect(draftId, retry) {
        loading = true
        plan = null
        error = null
        try {
            drafts = repository.drafts("S1")
            plan = loadPlan(repository, draftId)
        } catch (exception: Exception) { error = exception.message ?: "Could not load this draft" }
        loading = false
    }
    val current = plan
    val empty = current?.selections?.isEmpty() == true
    val issues = current?.validation?.issues.orEmpty()
    val title = when {
        empty -> "Start your registration"
        issues.isEmpty() && current != null -> "Ready to register"
        else -> "Review registration"
    }
    val subtitle = when {
        empty -> "Your module selection is empty."
        issues.isEmpty() && current != null -> "One final look. Everything checks out."
        else -> "Check your modules before confirming."
    }
    RegistrationScaffold(if (empty) "01 / SELECT" else "02 / REVIEW", title, subtitle, onBack,
        footer = {
            if (current != null && !loading && error == null) {
                Text("${current.selections.size} modules selected", color = P.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            when {
                loading -> PrimaryAction("Loading draft…", {}, enabled = false)
                error != null -> PrimaryAction("Retry loading draft", { retry++ })
                current == null -> PrimaryAction("Choose another draft", { expanded = true }, enabled = drafts.isNotEmpty())
                current?.registration != null -> PrimaryAction("View saved registration", { onSuccess(current.registration.id) })
                empty -> PrimaryAction("Browse available modules", onBrowse)
                issues.isNotEmpty() -> PrimaryAction("Resolve ${if (issues.size == 1) "issue" else "issues"}", {
                    val issue = issues.first()
                    val selection = current?.selections?.lastOrNull { it.groupId in issue.groupIds }
                        ?: current?.selections?.firstOrNull { it.courseId in issue.courseIds }
                    onCorrect(issue, selection?.courseId, selection?.groupId)
                })
                else -> PrimaryAction("Continue to confirmation", onConfirm, enabled = current.validation.isValid)
            }
        }) {
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = drafts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Draft: ${current?.draft?.name ?: draftId}  ▾", color = P.blue)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                drafts.forEach { draft ->
                    DropdownMenuItem(text = { Text(draft.name) }, onClick = {
                        expanded = false
                        onDraftChange(draft.id)
                    })
                }
            }
        }
        when {
            loading -> CircularProgressIndicator(color = P.blue)
            error != null -> {
                Text("Could not load this draft: ${error ?: "Unknown error"}", color = P.red)
                TextButton(onClick = { retry++ }) { Text("Retry") }
            }
            current == null -> {
                Text("Draft not found. Choose another saved draft or return to the student workspace.", color = P.muted)
                if (drafts.isEmpty()) TextButton(onClick = { retry++ }) { Text("Retry") }
            }
            empty -> {
                Spacer(Modifier.height(25.dp))
                Text("Make room for what’s next.", color = P.ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Choose your modules, check your timetable, then confirm everything in one place.", color = P.muted, lineHeight = 23.sp)
                InfoBanner("Selection required", "You need at least one module to register.", positive = false)
            }
            else -> {
                if (issues.isEmpty()) {
                    InfoBanner("All registration checks passed", "No timetable clashes or eligibility issues. Your selected classes have seats.", positive = true)
                } else {
                    issues.forEach { issue ->
                        val (heading, detail) = issueText(issue)
                        InfoBanner(heading, detail, positive = false)
                        TextButton(onClick = {
                            val selection = current.selections.lastOrNull { it.groupId in issue.groupIds }
                                ?: current.selections.firstOrNull { it.courseId in issue.courseIds }
                            onCorrect(issue, selection?.courseId, selection?.groupId)
                        }) { Text("Fix $heading", color = P.blue) }
                    }
                }
                Text("Selected modules", color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                current.selections.forEach { selection ->
                    CourseCard(current.courses.firstOrNull { it.id == selection.courseId },
                        current.groups.firstOrNull { it.id == selection.groupId },
                        affected = issues.any { selection.groupId in it.groupIds || selection.courseId in it.courseIds })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

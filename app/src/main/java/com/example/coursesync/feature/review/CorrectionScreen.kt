package com.example.coursesync.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.ClassGroup
import com.example.coursesync.shared.validation.IssueType
import com.example.coursesync.ui.theme.PrototypeStyle as P
import kotlinx.coroutines.launch

/** Temporary Member 1 correction view until the course/timetable owners provide their editor. */
@Composable
fun CorrectionScreen(
    repository: CourseSyncRepository, draftId: String, issueType: IssueType,
    affectedCourseId: String?, affectedGroupId: String?, onBack: () -> Unit, onApplied: () -> Unit
) {
    var plan by remember(draftId) { mutableStateOf<PlanSnapshot?>(null) }
    var chosenGroupId by remember(draftId, issueType) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var remainingSeats by remember(draftId) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(draftId) {
        plan = loadPlan(repository, draftId)
        remainingSeats = repository.remainingSeats(plan?.groups.orEmpty())
    }
    val current = plan
    val selected = current?.selections?.firstOrNull { it.courseId == affectedCourseId || it.groupId == affectedGroupId }
    val targetCourseId = selected?.courseId ?: affectedCourseId
    val choices = when (issueType) {
        IssueType.TIME_OVERLAP, IssueType.UNKNOWN_GROUP -> current?.groups?.filter { it.courseId == targetCourseId && it.id != selected?.groupId }.orEmpty()
        IssueType.FULL_GROUP, IssueType.EMPTY_SELECTION -> current?.groups?.filter { group ->
            current.selections.none { it.courseId == group.courseId } && group.id != affectedGroupId
        }.orEmpty()
        IssueType.MISSING_PREREQUISITE -> emptyList()
    }
    val title = when (issueType) {
        IssueType.TIME_OVERLAP, IssueType.UNKNOWN_GROUP -> "Choose another group"
        IssueType.MISSING_PREREQUISITE -> "Update your selection"
        IssueType.FULL_GROUP -> "Find a replacement"
        IssueType.EMPTY_SELECTION -> "Choose a module"
    }
    val subtitle = when (issueType) {
        IssueType.TIME_OVERLAP -> "Keep the module and change its class group."
        IssueType.MISSING_PREREQUISITE -> "Remove the module until its prerequisite is completed."
        IssueType.FULL_GROUP -> "Replace the full class with an available module."
        IssueType.EMPTY_SELECTION -> "Add a class group to your draft."
        else -> "Choose a valid group for this module."
    }
    RegistrationScaffold("02 / REVIEW", title, subtitle, onBack, footer = {
        if (error != null) Text(error ?: "Could not update draft", color = P.red, fontSize = 13.sp)
        Text("Draft ${current?.draft?.id ?: draftId} · affected ${targetCourseId ?: "selection"}", color = P.muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        PrimaryAction(when (issueType) {
            IssueType.MISSING_PREREQUISITE -> "Remove ${targetCourseId ?: "module"} from draft"
            IssueType.TIME_OVERLAP, IssueType.UNKNOWN_GROUP -> "Apply group change"
            IssueType.FULL_GROUP -> "Apply replacement"
            IssueType.EMPTY_SELECTION -> "Add module to draft"
        }, onClick = {
            if (current == null) return@PrimaryAction
            busy = true
            scope.launch {
                try {
                    when (issueType) {
                        IssueType.MISSING_PREREQUISITE -> repository.removeSelection(draftId, requireNotNull(targetCourseId))
                        IssueType.TIME_OVERLAP, IssueType.UNKNOWN_GROUP -> repository.changeSelection(draftId, requireNotNull(targetCourseId), requireNotNull(chosenGroupId))
                        IssueType.FULL_GROUP -> {
                            val group = requireNotNull(current.groups.firstOrNull { it.id == chosenGroupId })
                            repository.replaceSelection(draftId, requireNotNull(targetCourseId), group.courseId, group.id)
                        }
                        IssueType.EMPTY_SELECTION -> {
                            val group = requireNotNull(current.groups.firstOrNull { it.id == chosenGroupId })
                            repository.addSelection(draftId, group.courseId, group.id)
                        }
                    }
                    onApplied()
                } catch (exception: Exception) { error = exception.message ?: "Could not update draft" }
                busy = false
            }
        }, enabled = !busy && current != null && (issueType == IssueType.MISSING_PREREQUISITE ||
            (chosenGroupId != null && (remainingSeats[chosenGroupId] ?: 0) > 0)))
    }) {
        if (current == null) {
            CircularProgressIndicator(color = P.blue)
        } else {
            selected?.let { selection ->
                CourseCard(current.courses.firstOrNull { it.id == selection.courseId },
                    current.groups.firstOrNull { it.id == selection.groupId }, affected = true)
            }
            val matchingIssue = current.validation.issues.firstOrNull { it.type == issueType }
            matchingIssue?.let {
                val (heading, detail) = issueText(it)
                InfoBanner(heading, detail, positive = false)
            }
            if (issueType == IssueType.MISSING_PREREQUISITE) {
                Text("After removing ${targetCourseId ?: "this module"}", color = P.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                current.selections.filter { it.courseId != targetCourseId }.forEach { selection ->
                    Text("${selection.courseId} · ${current.courses.firstOrNull { it.id == selection.courseId }?.title.orEmpty()}", color = P.muted)
                }
            } else {
                Text(if (issueType == IssueType.TIME_OVERLAP) "Available groups" else "Choose a class", color = P.ink,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (choices.isEmpty()) Text("No alternative is available in this sample. Remove the affected selection or return to the course workspace.", color = P.muted)
                choices.forEach { group ->
                    ChoiceCard(group, current.courses.firstOrNull { it.id == group.courseId }?.title.orEmpty(),
                        remaining = remainingSeats[group.id] ?: 0,
                        chosen = chosenGroupId == group.id, onClick = { chosenGroupId = group.id })
                }
                Text("Changes are saved to this draft. Review validation again before confirming.", color = P.muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChoiceCard(group: ClassGroup, title: String, remaining: Int, chosen: Boolean, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(enabled = remaining > 0, onClick = onClick), shape = RoundedCornerShape(12.dp), color = Color.White,
        border = BorderStroke(1.dp, if (chosen) P.blue else P.border)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = chosen, onClick = onClick, enabled = remaining > 0)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("${group.courseId} · Group ${group.label}", color = P.ink, fontWeight = FontWeight.Bold)
                Text(title, color = P.muted, fontSize = 13.sp)
                Text(groupTime(group), color = P.muted, fontSize = 13.sp)
                Text(if (remaining > 0) "$remaining seats remaining" else "Full · 0 seats remaining",
                    color = if (remaining > 0) P.muted else P.red, fontSize = 13.sp)
            }
        }
    }
}

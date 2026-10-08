package com.example.coursesync.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.*
import com.example.coursesync.shared.validation.*
import com.example.coursesync.ui.theme.PrototypeStyle as Style
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun WeeklyTimetableScreen(
    repository: CourseSyncRepository,
    activeDraftId: String,
    onBrowse: () -> Unit,
    onReview: () -> Unit,
    onDrafts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var draft by remember(activeDraftId) { mutableStateOf<Draft?>(null) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroup>>(emptyList()) }
    var selections by remember(activeDraftId) { mutableStateOf<List<DraftSelection>>(emptyList()) }
    var issues by remember(activeDraftId) { mutableStateOf<List<ValidationIssue>>(emptyList()) }
    var seats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var confirmed by remember(activeDraftId) { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember(activeDraftId) { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable(activeDraftId) { mutableStateOf<String?>(null) }
    var removingId by rememberSaveable(activeDraftId) { mutableStateOf<String?>(null) }
    val clashes = issues.filter { it.type == IssueType.TIME_OVERLAP }
    val selectedGroups = selections.mapNotNull { selection -> groups.find { it.id == selection.groupId } }
    val conflictingIds = clashes.flatMap { it.groupIds }.toSet()

    suspend fun refresh() {
        val saved = repository.reopenDraft(activeDraftId)
        draft = saved?.first
        courses = repository.courses()
        groups = repository.groups()
        seats = repository.remainingSeats(groups)
        val registration = saved?.first?.let { plan -> repository.registrations(plan.studentId).find { it.draftId == plan.id } }
        confirmed = registration != null
        selections = if (registration != null) repository.registrationSelections(registration.id).map {
            DraftSelection(activeDraftId, it.courseId, it.groupId)
        } else saved?.second.orEmpty()
        issues = if (saved != null && !confirmed) repository.validateDraft(activeDraftId).issues else emptyList()
    }
    fun perform(success: String, operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val hadClashes = clashes.isNotEmpty()
                operation()
                refresh()
                editingId = null
                removingId = null
                message = if (hadClashes && issues.none { it.type == IssueType.TIME_OVERLAP }) "Clash resolved. Your updated week is saved." else success
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (exception: Exception) { error = exception.message ?: "Could not save the change. Please try again."
            } finally { busy = false }
        }
    }
    LaunchedEffect(activeDraftId) {
        loading = true
        try { refresh(); error = null
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (exception: Exception) { error = exception.message ?: "Could not load your timetable."
        } finally { loading = false }
    }

    Column(modifier.fillMaxSize().background(Style.background).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (confirmed) "REGISTERED / TIMETABLE" else "PLAN / TIMETABLE", color = Style.blue,
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(when { clashes.isNotEmpty() -> "Your timetable needs attention"; confirmed -> "Your registered week"; else -> "Your weekly plan" },
            style = MaterialTheme.typography.headlineMedium, color = Style.ink, fontWeight = FontWeight.Bold)
        Text("${draft?.name ?: "No active draft"} • Offline demonstration", color = Style.muted)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Notice(it, true) }
        message?.let { Notice(it, false) }
        if (!loading && draft == null) {
            Text("This draft is no longer available. Open a saved draft to see your week.")
            Button(onClick = onDrafts) { Text("Open saved drafts") }
        } else if (!loading) {
            if (confirmed) Notice("Confirmed registration • Read only. These classes come from your saved registration.", false)
            if (clashes.isNotEmpty()) {
                Notice("Clash detected immediately • ${clashes.size} overlapping pair(s). Change a group or remove a class below.", true)
                clashes.forEach { issue ->
                    val affected = selectedGroups.filter { it.id in issue.groupIds }
                    if (affected.size == 2) {
                        val a = affected[0]; val b = affected[1]
                        Surface(color = Style.paleRed, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${a.courseId} + ${b.courseId}", fontWeight = FontWeight.Bold, color = Style.red)
                                Text("${timetableDays[a.day - 1]} overlap: ${timetableTime(maxOf(a.startMinute, b.startMinute))}–${timetableTime(minOf(a.endMinute, b.endMinute))}", color = Style.ink)
                                affected.forEach { group ->
                                    Text("${courses.find { it.id == group.courseId }?.title ?: group.courseId} • Group ${group.label}")
                                    OutlinedButton(enabled = !busy, onClick = { editingId = group.courseId }) { Text("Resolve clash · ${group.courseId}") }
                                }
                            }
                        }
                    }
                }
            } else if (selectedGroups.isNotEmpty()) Notice("No timetable clashes • All class times fit together.", false)
            if (selectedGroups.isEmpty()) {
                Surface(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Your week starts here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Add modules to your plan. Your classes will appear by day and time, and any overlap will be flagged.")
                        Button(onClick = onBrowse) { Text("Browse modules") }
                    }
                }
            } else {
                WeeklyGrid(selectedGroups, conflictingIds, onSession = { if (!confirmed && !busy) editingId = it.courseId })
                Text("${selectedGroups.size} modules • ${clashes.size} clashes", fontWeight = FontWeight.Bold, color = Style.ink)
                Text("Selected classes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                selectedGroups.sortedWith(compareBy<ClassGroup> { it.day }.thenBy { it.startMinute }).forEach { group ->
                    Surface(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${group.courseId} · Group ${group.label}", fontWeight = FontWeight.Bold, color = Style.ink)
                            Text(courses.find { it.id == group.courseId }?.title ?: group.courseId)
                            Text("${timetableDays[group.day - 1]} · ${timetableTime(group.startMinute)}–${timetableTime(group.endMinute)}", color = Style.muted)
                            if (group.id in conflictingIds) Text("Time clash", color = Style.red, fontWeight = FontWeight.Bold)
                            if (!confirmed) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(enabled = !busy, onClick = { editingId = group.courseId }) { Text("Change group") }
                                TextButton(enabled = !busy, onClick = { removingId = group.courseId }) { Text("Remove class", color = Style.red) }
                            }
                        }
                    }
                }
                if (!confirmed) {
                    val otherIssues = issues.count { it.type != IssueType.TIME_OVERLAP && it.type != IssueType.EMPTY_SELECTION }
                    if (otherIssues > 0) Notice("$otherIssues other registration issue(s). Review registration for prerequisite and seat details.", true)
                    Button(onClick = onReview, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Review registration") }
                    OutlinedButton(onClick = onBrowse, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Add a module") }
                }
            }
        }
        HorizontalDivider()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBrowse, enabled = !busy) { Text("Courses") }
            Text("Timetable", Modifier.padding(12.dp), color = Style.blue, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDrafts, enabled = !busy) { Text("Saved drafts") }
        }
    }

    editingId?.let { courseId ->
        val current = selectedGroups.find { it.courseId == courseId }
        if (current != null && !confirmed) AlertDialog(
            onDismissRequest = { if (!busy) editingId = null },
            title = { Text("Change $courseId group") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose a group. Changes are saved to this draft immediately.")
                    groups.filter { it.courseId == courseId }.forEach { candidate ->
                        val proposed = selections.map { if (it.courseId == courseId) it.copy(groupId = candidate.id) else it }
                        // Use the same overlap validator as registration; other eligibility rules remain in review.
                        val overlaps = PlanValidator.validate(proposed, groups, courses, emptySet(), emptyMap()).issues
                            .filter { it.type == IssueType.TIME_OVERLAP && candidate.id in it.groupIds }
                        val remaining = seats[candidate.id] ?: 0
                        val isCurrent = current.id == candidate.id
                        Surface(color = if (isCurrent) Style.paleBlue else Style.background, shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Group ${candidate.label}${if (isCurrent) " · Current" else ""}", fontWeight = FontWeight.Bold)
                                Text("${timetableDays[candidate.day - 1]} · ${timetableTime(candidate.startMinute)}–${timetableTime(candidate.endMinute)}")
                                Text("$remaining seats available")
                                Text(if (overlaps.isEmpty()) "No time clashes" else "Clashes with ${overlaps.flatMap { it.courseIds }.filter { it != courseId }.distinct().joinToString()}",
                                    color = if (overlaps.isEmpty()) Style.green else Style.red)
                                Button(enabled = !busy && !isCurrent && remaining > 0 && overlaps.isEmpty(),
                                    onClick = { perform("Group changed and saved.") { repository.changeSelection(activeDraftId, courseId, candidate.id) } }) {
                                    Text(if (isCurrent) "Current group" else if (remaining == 0) "Group full" else if (overlaps.isNotEmpty()) "Time clash" else "Choose group ${candidate.label}")
                                }
                            }
                        }
                    }
                    if (groups.count { it.courseId == courseId } < 2) Text("No alternative group is offered for this module. You can remove this class or change the other conflicting module.")
                    error?.let { Text(it, color = Style.red) }
                    TextButton(enabled = !busy, onClick = { editingId = null; removingId = courseId }) { Text("Remove this class", color = Style.red) }
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = { editingId = null }) { Text("Close") } }
        )
    }
    removingId?.let { courseId ->
        AlertDialog(onDismissRequest = { if (!busy) removingId = null }, title = { Text("Remove $courseId?") },
            text = { Column { Text("This class will be removed from your draft and timetable."); error?.let { Text(it, color = Style.red) } } },
            confirmButton = { TextButton(enabled = !busy, onClick = { perform("Class removed and draft saved.") { repository.removeSelection(activeDraftId, courseId) } }) { Text("Remove", color = Style.red) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { removingId = null }) { Text("Keep class") } })
    }
}

@Composable
private fun Notice(message: String, warning: Boolean) {
    Surface(color = if (warning) Style.paleRed else Style.paleGreen, shape = RoundedCornerShape(12.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            color = if (warning) Style.red else Style.green, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WeeklyGrid(groups: List<ClassGroup>, conflicts: Set<String>, onSession: (ClassGroup) -> Unit) {
    val firstMinute = minOf(480, (groups.minOf { it.startMinute } / 60) * 60)
    val lastMinute = maxOf(720, ((groups.maxOf { it.endMinute } + 59) / 60) * 60)
    val placements = remember(groups) { timetablePlacements(groups) }
    val dayCount = maxOf(5, groups.maxOf { it.day })
    val hourHeight = 104.dp
    Surface(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("WEEKLY TIMETABLE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Style.ink)
            Text("Swipe across to see every day. Tap a class to inspect groups.", style = MaterialTheme.typography.bodySmall, color = Style.muted)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(54.dp)) {
                    Spacer(Modifier.height(32.dp))
                    for (minute in firstMinute until lastMinute step 60) Box(Modifier.height(hourHeight)) {
                        Text(timetableTime(minute), style = MaterialTheme.typography.labelSmall, color = Style.muted)
                    }
                }
                for (day in 1..dayCount) Column(Modifier.width(144.dp)) {
                    Text(timetableDays[day - 1].take(3), Modifier.height(32.dp).padding(start = 8.dp), fontWeight = FontWeight.Bold, color = Style.ink)
                    Box(Modifier.height(hourHeight * ((lastMinute - firstMinute) / 60f)).fillMaxWidth()) {
                        for (minute in firstMinute until lastMinute step 60) HorizontalDivider(
                            Modifier.offset(y = hourHeight * ((minute - firstMinute) / 60f)), color = Style.border)
                        placements.filter { it.group.day == day }.forEach { placement ->
                            val group = placement.group
                            val clash = group.id in conflicts
                            val width = 140.dp / placement.laneCount
                            val label = "${group.courseId}, Group ${group.label}, ${timetableDays[day - 1]}, ${timetableTime(group.startMinute)} to ${timetableTime(group.endMinute)}${if (clash) ", Time clash" else ""}"
                            Column(Modifier.offset(x = width * placement.lane, y = hourHeight * ((group.startMinute - firstMinute) / 60f))
                                .width(width - 4.dp).height(hourHeight * ((group.endMinute - group.startMinute) / 60f))
                                .background(if (clash) Style.paleRed else Style.paleBlue, RoundedCornerShape(6.dp))
                                .border(1.dp, if (clash) Style.red else Style.blue, RoundedCornerShape(6.dp))
                                .clickable { onSession(group) }.semantics(mergeDescendants = true) { contentDescription = label }
                                .padding(5.dp)) {
                                Text(group.courseId, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (clash) Style.red else Style.blue)
                                Text("Group ${group.label}", style = MaterialTheme.typography.labelSmall)
                                Text(timetableTime(group.startMinute), style = MaterialTheme.typography.labelSmall)
                                if (clash) Text("Clash", style = MaterialTheme.typography.labelSmall, color = Style.red)
                            }
                        }
                    }
                }
            }
            Text("A / B = class group · Times in local time · Red blocks = clash", style = MaterialTheme.typography.bodySmall, color = Style.muted)
        }
    }
}

package com.example.coursesync.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.shared.model.*
import com.example.coursesync.shared.validation.*
import com.example.coursesync.ui.theme.PrototypeStyle as P

internal data class PlanSnapshot(
    val draft: Draft,
    val selections: List<DraftSelection>,
    val courses: List<Course>,
    val groups: List<ClassGroup>,
    val validation: ValidationResult,
    val registration: Registration?
)

internal suspend fun loadPlan(repository: CourseSyncRepository, draftId: String): PlanSnapshot? {
    val pair = repository.reopenDraft(draftId) ?: return null
    return PlanSnapshot(pair.first, pair.second, repository.courses(), repository.groups(),
        repository.validateDraft(draftId), repository.registrations(pair.first.studentId).firstOrNull { it.draftId == draftId })
}

internal fun groupTime(group: ClassGroup?): String {
    if (group == null) return "Group unavailable"
    val day = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrElse(group.day - 1) { "Day ${group.day}" }
    fun clock(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
    return "$day · ${clock(group.startMinute)}–${clock(group.endMinute)}"
}

@Composable
internal fun RegistrationScaffold(
    step: String, title: String, subtitle: String, onBack: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(P.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ Back", color = P.ink) }
            Spacer(Modifier.weight(1f))
            Text("CourseSync", color = P.ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(30.dp).background(P.blue, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("↻", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(step, color = P.blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            repeat(3) { index ->
                Box(Modifier.padding(start = 6.dp).width(26.dp).height(4.dp).background(
                    if (index <= if (step.startsWith("03")) 2 else if (step.startsWith("02")) 1 else 0) P.blue else P.border,
                    RoundedCornerShape(3.dp)))
            }
        }
        Text(title, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            color = P.ink, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 9.dp, bottom = 20.dp),
            color = P.muted, fontSize = 16.sp, lineHeight = 22.sp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 12.dp), content = footer)
    }
}

@Composable
internal fun PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(53.dp),
        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = P.blue,
            disabledContainerColor = Color(0xFFDCE2E9), disabledContentColor = P.muted)) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun InfoBanner(title: String, body: String, positive: Boolean) {
    val foreground = if (positive) P.green else P.red
    Surface(color = if (positive) P.paleGreen else P.paleRed, shape = RoundedCornerShape(13.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (positive) "✓" else "⚠", color = foreground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Column {
                Text(title, color = foreground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(5.dp))
                Text(body, color = foreground, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
internal fun CourseCard(course: Course?, group: ClassGroup?, affected: Boolean = false) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White,
        border = BorderStroke(1.dp, if (affected) P.red else P.border)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(course?.id ?: group?.courseId ?: "Unknown course", color = if (affected) P.red else P.blue,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(if (affected) P.paleRed else P.paleBlue, RoundedCornerShape(6.dp)).padding(7.dp))
                Spacer(Modifier.width(10.dp))
                Text("Group ${group?.label ?: "?"}", color = P.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(course?.title ?: "Course unavailable", color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(groupTime(group), color = P.muted, fontSize = 13.sp)
        }
    }
}

internal fun issueText(issue: ValidationIssue): Pair<String, String> {
    val courses = issue.courseIds.joinToString(" and ")
    val groups = issue.groupIds.joinToString(" and ")
    return when (issue.type) {
        IssueType.EMPTY_SELECTION -> "No modules selected" to "Add at least one module before registration."
        IssueType.UNKNOWN_GROUP -> "Group unavailable" to "$courses uses an unknown class group ($groups). Change its selection."
        IssueType.MISSING_PREREQUISITE -> "Prerequisite not met" to "${issue.courseIds.firstOrNull() ?: "This module"} requires completed ${issue.courseIds.drop(1).firstOrNull() ?: "prior study"}. Remove or replace it."
        IssueType.FULL_GROUP -> "No seats available" to "$groups has no available seats. Choose another group or module."
        IssueType.TIME_OVERLAP -> "Timetable clash found" to "$courses overlap. Affected groups: $groups. Change a group to continue."
    }
}

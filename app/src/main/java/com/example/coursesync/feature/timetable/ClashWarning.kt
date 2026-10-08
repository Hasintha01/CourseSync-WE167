package com.example.coursesync.feature.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.coursesync.shared.model.*
import com.example.coursesync.shared.validation.*
import com.example.coursesync.ui.theme.PrototypeStyle

/** Selection feedback shares registration's overlap rules; no database writes here. */
@Composable
fun ClashWarning(selections: List<DraftSelection>, groups: List<ClassGroup>, courses: List<Course>, onResolve: () -> Unit) {
    val clashes = PlanValidator.validate(selections, groups, courses, emptySet(), emptyMap()).issues
        .filter { it.type == IssueType.TIME_OVERLAP }
    if (clashes.isEmpty()) return
    Surface(color = PrototypeStyle.paleRed, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp).semantics { liveRegion = LiveRegionMode.Assertive }) {
            Text("Timetable clash detected", color = PrototypeStyle.red, style = MaterialTheme.typography.titleMedium)
            clashes.forEach { issue ->
                val pair = groups.filter { it.id in issue.groupIds }
                if (pair.size == 2) {
                    val a = pair[0]; val b = pair[1]
                    Text("${a.courseId} and ${b.courseId} overlap on ${timetableDays[a.day - 1]} ${timetableTime(maxOf(a.startMinute, b.startMinute))}–${timetableTime(minOf(a.endMinute, b.endMinute))}")
                }
            }
            TextButton(onClick = onResolve) { Text("Resolve clash") }
        }
    }
}

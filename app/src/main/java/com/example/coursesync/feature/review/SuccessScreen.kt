package com.example.coursesync.feature.review

import androidx.compose.foundation.background
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
import com.example.coursesync.shared.model.Course
import com.example.coursesync.shared.model.Registration
import com.example.coursesync.shared.model.RegistrationSelection
import com.example.coursesync.ui.theme.PrototypeStyle as P

@Composable
fun SuccessScreen(repository: CourseSyncRepository, registrationId: String, onBack: () -> Unit, onTimetable: () -> Unit) {
    var registration by remember(registrationId) { mutableStateOf<Registration?>(null) }
    var selections by remember(registrationId) { mutableStateOf<List<RegistrationSelection>>(emptyList()) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroup>>(emptyList()) }
    LaunchedEffect(registrationId) {
        registration = repository.registrations("S1").firstOrNull { it.id == registrationId }
        selections = repository.registrationSelections(registrationId)
        courses = repository.courses()
        groups = repository.groups()
    }
    RegistrationScaffold("03 / COMPLETE", "Registration complete", "Your modules are confirmed.", onBack,
        footer = {
            Text("Registration saved · ${registration?.studentId ?: "S1"}", color = P.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PrimaryAction("View my timetable", onTimetable)
        }) {
        if (registration == null) {
            CircularProgressIndicator(color = P.blue)
        } else {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(84.dp).background(P.paleGreen, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text("✓", color = P.green, fontSize = 35.sp)
                }
            }
            Surface(color = P.blue, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("YOU’RE ALL SET", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(13.dp))
                    Text("${selections.size} modules confirmed", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Registered modules", color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            selections.forEach { selection ->
                val course = courses.firstOrNull { it.id == selection.courseId }
                val group = groups.firstOrNull { it.id == selection.groupId }
                Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text("✓", color = P.green, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("${selection.courseId} · Group ${group?.label ?: "?"}", color = P.ink, fontWeight = FontWeight.Bold)
                            Text(course?.title ?: "Course unavailable", color = P.muted, fontSize = 13.sp)
                            Text(groupTime(group), color = P.muted, fontSize = 13.sp)
                        }
                    }
                }
            }
            Text("Registration ID: ${registration?.id}", color = P.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

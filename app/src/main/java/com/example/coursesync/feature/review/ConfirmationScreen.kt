package com.example.coursesync.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.shared.data.ConfirmationResult
import com.example.coursesync.shared.data.CourseSyncRepository
import com.example.coursesync.ui.theme.PrototypeStyle as P
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun ConfirmationScreen(repository: CourseSyncRepository, draftId: String, onBack: () -> Unit,
                       onInvalid: () -> Unit, onSuccess: (String) -> Unit) {
    var plan by remember(draftId) { mutableStateOf<PlanSnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(draftId, retry) {
        plan = null
        loadError = null
        try { plan = loadPlan(repository, draftId)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (exception: Exception) { loadError = exception.message ?: "Could not load the registration plan." }
    }
    val current = plan
    RegistrationScaffold("03 / CONFIRM", "Confirm registration", "You’re about to register for these modules.", onBack,
        footer = {
            if (error != null) Text(error ?: "Registration failed", color = P.red, fontSize = 13.sp)
            Text("Final step · ${current?.selections?.size ?: 0} modules", color = P.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PrimaryAction(if (busy) "Confirming…" else "Confirm registration", onClick = {
                if (busy) return@PrimaryAction
                busy = true
                scope.launch {
                    try {
                        when (val result = repository.confirmRegistration(draftId)) {
                            is ConfirmationResult.Confirmed -> onSuccess(result.registration.id)
                            is ConfirmationResult.AlreadyConfirmed -> onSuccess(result.registration.id)
                            is ConfirmationResult.Invalid -> onInvalid()
                        }
                    } catch (exception: Exception) { error = exception.message ?: "Registration failed" }
                    busy = false
                }
            }, enabled = !busy && current != null && current.validation.isValid)
        }) {
        if (loadError != null) {
            Text(loadError ?: "Could not load the registration plan.", color = P.red)
            TextButton(onClick = { retry++ }) { Text("Retry loading") }
        } else if (current == null) {
            CircularProgressIndicator(color = P.blue)
        } else {
            if (!current.validation.isValid) {
                InfoBanner("Review required", "Your draft has a blocking issue. Go back and correct it before confirming.", positive = false)
                TextButton(onClick = onInvalid) { Text("Return to review", color = P.blue) }
            }
            Surface(color = P.ink, shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("${current.selections.size.toString().padStart(2, '0')} modules", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Your final selection", color = Color.White, fontSize = 13.sp)
                }
            }
            current.selections.forEach { selection ->
                val course = current.courses.firstOrNull { it.id == selection.courseId }
                val group = current.groups.firstOrNull { it.id == selection.groupId }
                Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text("${selection.courseId} · Group ${group?.label ?: "?"}", color = P.blue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(course?.title ?: "Course unavailable", color = P.ink)
                        Text(groupTime(group), color = P.muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("By confirming, you submit the modules listed above as your final selection.", color = P.muted, lineHeight = 22.sp)
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Go back and review", color = P.blue) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

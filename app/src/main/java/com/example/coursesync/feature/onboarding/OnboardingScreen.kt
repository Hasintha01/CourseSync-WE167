package com.example.coursesync.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.ui.theme.CourseSyncBrand
import com.example.coursesync.ui.theme.PrototypeStyle as P

private data class Slide(val eyebrow: String, val title: String, val description: String)
private val slides = listOf(
    Slide("WELCOME TO COURSESYNC", "Plan with confidence.", "Choose modules, understand conflicts and prepare a registration that works."),
    Slide("AUTOMATIC CHECKS", "Catch conflicts early.", "CourseSync highlights overlapping sessions while you are still building your plan."),
    Slide("WEEKLY OVERVIEW", "See your week clearly.", "Compare days, groups and available time before committing to a selection."),
    Slide("FINAL REVIEW", "Review before you register.", "Check modules, prerequisites and timetable conflicts in one final step.")
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    Column(Modifier.fillMaxSize().background(P.background).statusBarsPadding().navigationBarsPadding()) {
        if (step < 4) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text("‹ Back", color = P.ink) }
                else Spacer(Modifier.width(72.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    repeat(4) { index ->
                        Box(Modifier.padding(horizontal = 3.dp).width(22.dp).height(4.dp)
                            .background(if (index <= step) P.blue else P.border, RoundedCornerShape(4.dp)))
                    }
                }
                if (step < 3) TextButton(onClick = onComplete) { Text("Skip", color = P.blue) }
                else Spacer(Modifier.width(72.dp))
            }
            CourseSyncBrand(48.dp, 24.sp, 11.sp,
                Modifier.align(Alignment.CenterHorizontally).padding(bottom = 28.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Hero(step)
                Spacer(Modifier.height(28.dp))
                Text(slides[step].eyebrow, color = P.blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(slides[step].title, color = P.ink, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(36.dp))
                Text(slides[step].description, color = P.muted, fontSize = 16.sp, lineHeight = 22.sp)
            }
            Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth().padding(24.dp).height(54.dp),
                shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = P.blue)) {
                Text(if (step == 3) "Start planning" else "Next", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
                CourseSyncBrand(64.dp, 32.sp, 14.sp, Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(56.dp))
                Text("ONBOARDING COMPLETE", modifier = Modifier.background(P.lime, RoundedCornerShape(10.dp)).padding(12.dp),
                    color = P.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Text("Your next semester", color = P.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Start with a clash-free plan and review every requirement before registration.", color = P.muted, lineHeight = 23.sp)
                Spacer(Modifier.height(38.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = androidx.compose.ui.graphics.Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, P.border)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("REGISTRATION DRAFT", color = P.blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(22.dp))
                        Text("Ready when you are", color = P.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Choose Student or Staff to explore the workspace.", color = P.muted)
                    }
                }
                Spacer(Modifier.height(22.dp))
                OutlinedButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(13.dp)) {
                    Text("Replay onboarding", color = P.blue)
                }
            }
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().padding(24.dp).height(54.dp),
                shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = P.blue)) {
                Text("Choose workspace")
            }
        }
    }
}

@Composable
private fun Hero(step: Int) {
    Surface(Modifier.fillMaxWidth().height(250.dp), shape = RoundedCornerShape(24.dp), color = androidx.compose.ui.graphics.Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, P.border)) {
        Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                0 -> {
                    Box(Modifier.fillMaxWidth().height(48.dp).background(P.ink, RoundedCornerShape(12.dp)).padding(14.dp)) {
                        Text("MONDAY", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroClass("CS101", "09:00", P.paleBlue, P.blue, Modifier.weight(1f))
                        HeroClass("MA101", "10:00", P.paleRed, P.red, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("Plan without clashes", modifier = Modifier.background(P.lime, RoundedCornerShape(30.dp)).padding(horizontal = 20.dp, vertical = 9.dp), color = P.ink, fontSize = 11.sp)
                }
                1 -> {
                    Text("Monday", color = P.ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        HeroClass("CS101", "09:00–10:00", P.paleBlue, P.blue, Modifier.weight(1f))
                        HeroClass("MA101", "09:30–10:30", P.paleRed, P.red, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Clash found early", modifier = Modifier.background(P.lime, RoundedCornerShape(30.dp)).padding(10.dp), color = P.ink, fontSize = 11.sp)
                }
                2 -> {
                    Text("M       T       W       T       F", color = P.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("101", "201", "303").forEachIndexed { index, label ->
                            Box(Modifier.width(56.dp).height(105.dp).background(if (index == 1) P.lime else P.blue, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                Text(label, color = if (index == 1) P.ink else androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    Box(Modifier.size(78.dp).background(P.paleGreen, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Text("✓", color = P.green, fontSize = 35.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("READY TO REGISTER", color = P.blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Review every requirement", color = P.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("All registration checks passed", modifier = Modifier.background(P.paleBlue, RoundedCornerShape(8.dp)).padding(10.dp), color = P.blue, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HeroClass(code: String, time: String, background: androidx.compose.ui.graphics.Color,
                      foreground: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier.height(86.dp).background(background, RoundedCornerShape(13.dp)).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(code, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(time, color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

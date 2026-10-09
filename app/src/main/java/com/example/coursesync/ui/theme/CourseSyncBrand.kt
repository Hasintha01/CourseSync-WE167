package com.example.coursesync.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coursesync.R

private val brandInk = Color(0xFF1C2A39)
private val brandBlue = Color(0xFF2D4BBD)
private val brandMuted = Color(0xFF566473)

@Composable
fun CourseSyncLogo(size: Dp, modifier: Modifier = Modifier, decorative: Boolean = false) {
    Image(
        painter = painterResource(R.drawable.coursesync_logo),
        contentDescription = if (decorative) null else "CourseSync logo",
        modifier = modifier.size(size)
    )
}

@Composable
fun CourseSyncWordmark(size: TextUnit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Course", color = brandInk, fontSize = size, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("Sync", color = brandBlue, fontSize = size, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun CourseSyncBrand(
    iconSize: Dp,
    wordmarkSize: TextUnit,
    taglineSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CourseSyncLogo(iconSize, decorative = true)
        Column {
            CourseSyncWordmark(wordmarkSize)
            Text("Plan clearly. Register confidently.", color = brandMuted,
                fontSize = taglineSize, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

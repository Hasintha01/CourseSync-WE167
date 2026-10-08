package com.example.coursesync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrototypeStyle.blue,
    secondary = PrototypeStyle.muted,
    tertiary = PrototypeStyle.green,
    background = PrototypeStyle.background,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = PrototypeStyle.ink,
    onSurface = PrototypeStyle.ink,
    outline = PrototypeStyle.border
)

@Composable
fun CourseSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.coursesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.coursesync.navigation.CourseSyncApp
import com.example.coursesync.ui.theme.CourseSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourseSyncTheme {
                CourseSyncApp()
            }
        }
    }
}

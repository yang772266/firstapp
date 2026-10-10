package com.example.myfirstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.myfirstapp.data.local.AppDatabase
import com.example.myfirstapp.data.repository.CourseRepository
import com.example.myfirstapp.ui.schedule.ScheduleScreen
import com.example.myfirstapp.ui.schedule.ScheduleViewModel
import com.example.myfirstapp.ui.theme.MyFirstAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFirstAppTheme {
                val context = LocalContext.current
                val database = remember { AppDatabase.getDatabase(context) }
                val repository = remember { CourseRepository(database.courseDao()) }
                val scheduleViewModel = remember { ScheduleViewModel(repository) }

                ScheduleScreen(viewModel = scheduleViewModel)
            }
        }
    }
}

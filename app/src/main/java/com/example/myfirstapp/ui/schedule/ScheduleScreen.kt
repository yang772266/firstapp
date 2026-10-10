package com.example.myfirstapp.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.domain.model.Course
import com.example.myfirstapp.ui.components.CourseGridMatrix
import com.example.myfirstapp.ui.components.WeekSelectorBar
import com.example.myfirstapp.ui.import.ImportScheduleDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📚 湖科课程表 · 第 ${uiState.currentWeek} 周",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    TextButton(onClick = { viewModel.showImportDialog(true) }) {
                        Text(
                            text = "📥 导入",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 顶部 1-20 周滑动选择条
            WeekSelectorBar(
                currentWeek = uiState.currentWeek,
                totalWeeks = uiState.totalWeeks,
                onWeekSelected = { week -> viewModel.selectWeek(week) }
            )

            // 7天 x 10节 课表网格矩阵
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CourseGridMatrix(
                    courses = uiState.courses,
                    timeSlots = uiState.timeSlots,
                    onCourseClick = { course -> selectedCourse = course }
                )
            }
        }

        // 导入课表弹窗对话框
        if (uiState.isImportDialogVisible) {
            ImportScheduleDialog(
                currentWeek = uiState.currentWeek,
                isParsing = uiState.isParsing,
                parsedCourses = uiState.parsedCoursesForPreview,
                errorMessage = uiState.importMessage,
                onDismiss = { viewModel.showImportDialog(false) },
                onFetchHbustSchedule = { cookie -> viewModel.fetchHbustSchedule(cookie) },
                onParseHtml = { html -> viewModel.parseHtmlContent(html) },
                onParseImageUri = { uri -> viewModel.parseImageUri(context, uri) },
                onConfirmImport = { courses, clear -> viewModel.confirmImport(courses, clear) }
            )
        }

        // 点击课程卡片后的详情对话框
        selectedCourse?.let { course ->
            AlertDialog(
                onDismissRequest = { selectedCourse = null },
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = course.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(text = "📍 上课地点：${course.classroom.ifBlank { "未指定" }}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "👨‍🏫 任课教师：${course.teacher.ifBlank { "未指定" }}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "⏰ 节次范围：第 ${course.startPeriod}-${course.endPeriod} 节", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "📅 周次范围：第 ${course.startWeek}-${course.endWeek} 周", fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedCourse = null }) {
                        Text("知道啦")
                    }
                }
            )
        }
    }
}

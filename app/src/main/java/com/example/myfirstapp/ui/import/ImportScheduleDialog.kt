package com.example.myfirstapp.ui.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myfirstapp.domain.model.Course

@Composable
fun ImportScheduleDialog(
    currentWeek: Int,
    isParsing: Boolean,
    parsedCourses: List<Course>?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onFetchHbustSchedule: (cookie: String) -> Unit,
    onParseHtml: (String) -> Unit,
    onParseImageUri: (Uri) -> Unit,
    onConfirmImport: (courses: List<Course>, clearExisting: Boolean) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var htmlInput by remember { mutableStateOf("") }
    var clearExisting by remember { mutableStateOf(true) }
    var showWebLoginDialog by remember { mutableStateOf(false) }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onParseImageUri(it) }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onParseImageUri(it) }
    }

    if (showWebLoginDialog) {
        HbustWebViewLoginDialog(
            onDismiss = { showWebLoginDialog = false },
            onCookieCaptured = { cookie ->
                showWebLoginDialog = false
                onFetchHbustSchedule(cookie)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "📥 导入课表 (目标：第 $currentWeek 周)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (parsedCourses == null) {
                    // 导入模式 Tab 切换：湖科教务直连 / HTML 源码 / 截图 OCR
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("湖科教务直连", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("HTML 源码", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("截图 OCR", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isParsing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("正在自动抓取并解析第 $currentWeek 周课表...")
                            }
                        }
                    } else if (selectedTabIndex == 0) {
                        // 湖科大教务直连模式
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "打开湖北科技学院教务系统 (jwgl.hbust.edu.cn)，登录成功后将自动获取第 $currentWeek 周课表。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showWebLoginDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🏫 打开湖北科技学院教务登录")
                            }
                        }
                    } else if (selectedTabIndex == 1) {
                        // HTML 源码粘贴模式
                        OutlinedTextField(
                            value = htmlInput,
                            onValueChange = { htmlInput = it },
                            label = { Text("粘贴第 $currentWeek 周课表网页源码") },
                            placeholder = { Text("粘贴包含 table.kb_table 的源码...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            maxLines = 8
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onParseHtml(htmlInput) },
                            enabled = htmlInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("开始 Jsoup 自动解析")
                        }
                    } else {
                        // 截图 OCR 模式
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "从相册选取第 $currentWeek 周课表截图，使用 Google ML Kit 离线 OCR 提取课程。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    try {
                                        openDocumentLauncher.launch(arrayOf("image/*"))
                                    } catch (_: Exception) {
                                        try {
                                            getContentLauncher.launch("image/*")
                                        } catch (e2: Exception) {
                                            e2.printStackTrace()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🖼️ 选择课表截图图片")
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                    }
                } else {
                    // 预览与确认导入
                    Text(
                        text = "第 $currentWeek 周解析结果预览 (共 ${parsedCourses.size} 门课程)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(parsedCourses) { course ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = course.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "周${parseDayName(course.dayOfWeek)} 第${course.startPeriod}-${course.endPeriod}节 | 地点：${course.classroom.ifBlank { "未指定" }} | 教师：${course.teacher.ifBlank { "未指定" }}",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = clearExisting,
                            onCheckedChange = { clearExisting = it }
                        )
                        Text(text = "清空并覆盖第 $currentWeek 周的现有课程", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirmImport(parsedCourses, clearExisting) }
                        ) {
                            Text("确认导入至第 $currentWeek 周")
                        }
                    }
                }
            }
        }
    }
}

private fun parseDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "日"
        else -> "$dayOfWeek"
    }
}

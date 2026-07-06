package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudySession
import com.example.data.Subject
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    // Calculate aggregated metrics
    val totalSeconds = subjects.sumOf { it.totalStudySeconds }
    val totalTodaySeconds = subjects.sumOf { it.todayStudySeconds }
    val averageSecondsDaily = remember(sessions) {
        if (sessions.isEmpty()) 0L else {
            val datesCount = sessions.map { it.dateString }.distinct().size.coerceAtLeast(1)
            sessions.sumOf { it.durationSeconds } / datesCount
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // --- Title Header ---
        item {
            Text(
                text = "Analytics & Focus Habits 📊",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Core Stats Row (Today vs Average vs All-Time) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricMiniCard(
                    title = "Today Focus",
                    value = formatHoursMinutes(totalTodaySeconds),
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.primary
                )
                StatMetricMiniCard(
                    title = "Daily Avg",
                    value = formatHoursMinutes(averageSecondsDaily),
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.secondary
                )
                StatMetricMiniCard(
                    title = "Total Time",
                    value = formatHoursMinutes(totalSeconds),
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // --- Heatmap Calendar Widget ---
        item {
            HabitHeatmapWidget(sessions = sessions)
        }

        // --- Subject Distribution Pie/Donut Chart ---
        item {
            SubjectDistributionDonut(subjects = subjects)
        }

        // --- Weekly Focus History Bar Chart ---
        item {
            WeeklyFocusBarChart(sessions = sessions)
        }
    }
}

// --- Companion Sub-widgets ---

@Composable
fun StatMetricMiniCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HabitHeatmapWidget(sessions: List<StudySession>) {
    // Generate dates list for the last 24 days to display as a 6x4 heatmap grid
    val dates = remember {
        val list = mutableListOf<String>()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -23)
        for (i in 0..23) {
            list.add(formatter.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Map each date to total study hours on that day
    val dateStudySeconds = remember(sessions) {
        val map = mutableMapOf<String, Long>()
        sessions.forEach { sess ->
            val cur = map[sess.dateString] ?: 0L
            map[sess.dateString] = cur + sess.durationSeconds
        }
        map
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Focus Intensity Grid 🗓️",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Grid showing study density over past 24 days",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Build grid (6 columns, 4 rows)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 6) {
                            val index = row * 6 + col
                            val dateString = dates[index]
                            val totalS = dateStudySeconds[dateString] ?: 0L
                            
                            // Color intensity mapping: more study = brighter neon cyan
                            val color = when {
                                totalS <= 0L -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                totalS < 1800L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) // <30m
                                totalS < 7200L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) // <2h
                                else -> MaterialTheme.colorScheme.primary // >2h (solid cyan focus)
                            }

                            // Tooltip info helper formatting
                            val tooltipText = remember(totalS) {
                                val hr = totalS / 3600f
                                if (totalS <= 0L) "No study" else "${"%.1f".format(hr)} hrs focus"
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Small indicator representing day of month
                                    val dayNum = dateString.takeLast(2)
                                    Text(
                                        text = dayNum,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalS > 0L) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                    MaterialTheme.colorScheme.primary
                ).forEach { legendColor ->
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(legendColor))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("More Focus", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SubjectDistributionDonut(subjects: List<Subject>) {
    val totalS = subjects.sumOf { it.totalStudySeconds }.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Subject Focus Weight 🍩",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All-time percentage allocation of focused sessions",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (totalS <= 0f) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No study hours logged yet. Start timing to build focus charts!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Canvas donut drawing
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            var startAngle = -90f
                            subjects.forEach { subject ->
                                if (subject.totalStudySeconds > 0) {
                                    val sweepAngle = (subject.totalStudySeconds.toFloat() / totalS) * 360f
                                    val color = Color(android.graphics.Color.parseColor(subject.colorHex))
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }
                        
                        // Center placeholder
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STUDY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("RATIOS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right: Colored Labels list
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { subject ->
                            if (subject.totalStudySeconds > 0) {
                                val percent = (subject.totalStudySeconds.toFloat() / totalS) * 100f
                                val color = Color(android.graphics.Color.parseColor(subject.colorHex))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = subject.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "${percent.toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyFocusBarChart(sessions: List<StudySession>) {
    // Collect the past 7 days of dates
    val past7Dates = remember {
        val list = mutableListOf<String>()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            list.add(formatter.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Map date to day name (Mon, Tue)
    val dayNameMap = remember {
        val list = mutableListOf<String>()
        val formatter = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            list.add(formatter.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Map each date to study seconds
    val dateStudyMap = remember(sessions) {
        val map = mutableMapOf<String, Long>()
        sessions.forEach { sess ->
            val cur = map[sess.dateString] ?: 0L
            map[sess.dateString] = cur + sess.durationSeconds
        }
        map
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Study Trends 📈",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Focus distribution across the last 7 days",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val maxSeconds = remember(dateStudyMap) {
                dateStudyMap.values.maxOrNull()?.toFloat()?.coerceAtLeast(3600f) ?: 7200f
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                past7Dates.forEachIndexed { index, dateKey ->
                    val totalSec = dateStudyMap[dateKey] ?: 0L
                    val dayName = dayNameMap[index]
                    val fraction = (totalSec.toFloat() / maxSeconds).coerceIn(0f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (totalSec > 0) "${(totalSec / 60)}m" else "0",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Solid bar
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.7f * fraction + 0.05f) // keep a tiny minimum bar so it scales nicely
                                .width(16.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (fraction > 0f) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

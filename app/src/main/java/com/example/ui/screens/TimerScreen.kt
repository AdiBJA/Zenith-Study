package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Subject
import com.example.ui.components.SoundPreset
import com.example.ui.theme.*
import com.example.ui.viewmodel.PomodoroState
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.TimerMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimerScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val activeSubject by viewModel.activeSubject.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val pomodoroState by viewModel.pomodoroState.collectAsState()
    val pomodoroSecondsLeft by viewModel.pomodoroSecondsLeft.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    val activeSoundPreset by viewModel.activeSoundPreset.collectAsState()
    val audioVolume by viewModel.audioVolume.collectAsState()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showSubjectSelectDialog by remember { mutableStateOf(false) }
    
    // Derived overall stats
    val totalTodayStudySeconds = subjects.sumOf { it.todayStudySeconds }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // --- Header Stats Widget ---
        item {
            TodaySummaryHeader(totalSeconds = totalTodayStudySeconds)
        }

        // --- Core Timer Wheel Widget ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background ambient glow if running
                if (isTimerRunning) {
                    val pulse by rememberInfiniteTransition(label = "").animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = ""
                    )
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .shadow(
                                elevation = (25 * pulse).dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary
                            )
                    )
                }

                // Main Timer Card
                Card(
                    modifier = Modifier
                        .size(260.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Progress ring drawing
                        val targetSeconds = activeSubject?.targetSecondsPerDay ?: 7200L
                        val fraction = if (timerMode == TimerMode.STOPWATCH) {
                            if (targetSeconds > 0) (totalTodayStudySeconds.toFloat() / targetSeconds).coerceIn(0f, 1f) else 0f
                        } else {
                            val totalPomSeconds = if (pomodoroState == PomodoroState.WORK) 1500f else 300f
                            (pomodoroSecondsLeft.toFloat() / totalPomSeconds).coerceIn(0f, 1f)
                        }

                        TimerWheelCanvas(
                            fraction = fraction,
                            color = activeSubject?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary,
                            isTicking = isTimerRunning
                        )

                        // Digital Display contents
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = activeSubject?.name ?: "No subject selected",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = activeSubject?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Huge digital ticker
                            val displayTime = if (timerMode == TimerMode.STOPWATCH) {
                                formatDuration(elapsedSeconds)
                            } else {
                                formatDuration(pomodoroSecondsLeft)
                            }

                            Text(
                                text = displayTime,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("timer_countdown")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Mode Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val badgeText = if (timerMode == TimerMode.STOPWATCH) "STOPWATCH" else {
                                    if (pomodoroState == PomodoroState.WORK) "WORK TIME 🎯" else "BREAK TIME ☕"
                                }
                                val badgeColor = if (timerMode == TimerMode.STOPWATCH) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else if (pomodoroState == PomodoroState.WORK) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }

                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = badgeColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Timer Control Buttons ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle Button (Only enabled if stopped)
                IconButton(
                    onClick = {
                        val nextMode = if (timerMode == TimerMode.STOPWATCH) TimerMode.POMODORO else TimerMode.STOPWATCH
                        viewModel.setTimerMode(nextMode)
                    },
                    enabled = !isTimerRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isTimerRunning) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = if (timerMode == TimerMode.STOPWATCH) Icons.Default.Timer else Icons.Default.HourglassEmpty,
                        contentDescription = "Toggle Timer Mode",
                        tint = if (isTimerRunning) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                    )
                }

                // Play / Pause main button
                Button(
                    onClick = {
                        val currentSubject = activeSubject
                        if (currentSubject == null) {
                            showSubjectSelectDialog = true
                        } else {
                            if (isTimerRunning) viewModel.pauseTimer() else viewModel.startTimer(currentSubject)
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("play_pause_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Start or Pause study session",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Black
                    )
                }

                // Stop Study Session Button
                IconButton(
                    onClick = { viewModel.stopTimer() },
                    enabled = activeSubject != null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (activeSubject == null) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop and Save study session",
                        tint = if (activeSubject == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // --- Soundscape Audio Synthesizer Controls ---
        item {
            AmbientSoundMixerCard(
                activePreset = activeSoundPreset,
                volume = audioVolume,
                onPresetSelected = { viewModel.selectSoundPreset(it) },
                onVolumeChanged = { viewModel.setAudioVolume(it) }
            )
        }

        // --- Subject List Management Header ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Study Subjects",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(
                    onClick = { showAddSubjectDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Subject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- List of Study Subjects ---
        if (subjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No study subjects yet!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add custom subjects like Math, Coding, or Science to track your focus habits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(subjects) { subject ->
                SubjectCardItem(
                    subject = subject,
                    isActive = activeSubject?.id == subject.id && isTimerRunning,
                    onStartClick = { viewModel.startTimer(subject) },
                    onDeleteClick = { viewModel.deleteSubject(subject) }
                )
            }
        }
    }

    // Dialogs
    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onSave = { name, color, hours ->
                viewModel.addSubject(name, color, hours)
                showAddSubjectDialog = false
            }
        )
    }

    if (showSubjectSelectDialog) {
        SelectSubjectDialog(
            subjects = subjects,
            onDismiss = { showSubjectSelectDialog = false },
            onSelect = { subject ->
                viewModel.startTimer(subject)
                showSubjectSelectDialog = false
            },
            onAddNewSubject = {
                showSubjectSelectDialog = false
                showAddSubjectDialog = true
            }
        )
    }
}

// --- Companion Helper Composables ---

@Composable
fun TodaySummaryHeader(totalSeconds: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ZENITH FOCUS TODAY",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatHoursMinutes(totalSeconds),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            // Beautiful burning fire streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "3 Day Streak!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun TimerWheelCanvas(
    fraction: Float,
    color: Color,
    isTicking: Boolean
) {
    val transition = rememberInfiniteTransition(label = "")
    val animatedAngleOffset by if (isTicking) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val primaryColor = MaterialTheme.colorScheme.primaryContainer
    
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        // Background track circle
        drawCircle(
            color = primaryColor.copy(alpha = 0.25f),
            radius = size.minDimension / 2,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        )

        // Active progress arc
        drawArc(
            color = color,
            startAngle = -90f + animatedAngleOffset,
            sweepAngle = fraction * 360f,
            useCenter = false,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw small ticking nodes if study timer is active
        if (isTicking && fraction > 0f) {
            val angleRad = ((-90f + animatedAngleOffset + fraction * 360f) * PI / 180f).toFloat()
            val radius = (size.minDimension / 2)
            val center = size / 2f
            val dotX = center.width + radius * cos(angleRad)
            val dotY = center.height + radius * sin(angleRad)

            drawCircle(
                color = color,
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(dotX, dotY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(dotX, dotY)
            )
        }
    }
}

@Composable
fun AmbientSoundMixerCard(
    activePreset: SoundPreset,
    volume: Float,
    onPresetSelected: (SoundPreset) -> Unit,
    onVolumeChanged: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header expandable trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (activePreset != SoundPreset.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Synthesized Focus Soundscapes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activePreset == SoundPreset.NONE) "Soundscapes Offline" else "Playing ${activePreset.name.replace("_", " ")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Volume Control Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChanged,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio Presets grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val presets = listOf(
                            SoundPreset.BROWN_NOISE to "Deep Brown 🌊",
                            SoundPreset.WHITE_NOISE to "White Noise 💨",
                            SoundPreset.BINAURAL_BEATS to "Gamma Beat 🧠",
                            SoundPreset.COSMIC_SPACE to "Cosmic Pad 🛸"
                        )

                        presets.forEach { (preset, label) ->
                            val isSelected = activePreset == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onPresetSelected(preset) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectCardItem(
    subject: Subject,
    isActive: Boolean,
    onStartClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val parsedColor = remember(subject.colorHex) {
        try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { Color.Gray }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isActive) parsedColor else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) parsedColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color capsule indicator
                Box(
                    modifier = Modifier
                        .size(12.dp, 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(parsedColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Progress calculations
                    val progressFrac = if (subject.targetSecondsPerDay > 0) {
                        (subject.todayStudySeconds.toFloat() / subject.targetSecondsPerDay).coerceIn(0f, 1f)
                    } else 0f

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progressFrac },
                            modifier = Modifier
                                .width(100.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = parsedColor,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${formatHoursMinutes(subject.todayStudySeconds)} / ${formatHoursMinutes(subject.targetSecondsPerDay)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Play Action / Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStartClick,
                    modifier = Modifier
                        .background(
                            if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Quick focus subject",
                        tint = if (isActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete subject",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// --- Modal Dialog Helpers ---

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("2") }
    var selectedColorHex by remember { mutableStateOf("#FF5252") }

    val colorsHex = listOf("#FF5252", "#FF8540", "#FFEB3B", "#00E676", "#00E5FF", "#2979FF", "#D500F9", "#FF4081")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Study Subject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Organic Chem)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Daily Study Goal (Hours)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Selection
                Text("Select Theme Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorsHex.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetH = hours.toIntOrNull() ?: 2
                            if (name.isNotBlank()) {
                                onSave(name, selectedColorHex, targetH)
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun SelectSubjectDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSelect: (Subject) -> Unit,
    onAddNewSubject: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Study Subject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (subjects.isEmpty()) {
                    Text(
                        text = "You don't have any subjects yet. Create one to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onAddNewSubject, modifier = Modifier.fillMaxWidth()) {
                        Text("Add New Subject")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(subjects) { subject ->
                            val color = remember(subject.colorHex) {
                                Color(android.graphics.Color.parseColor(subject.colorHex))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .clickable { onSelect(subject) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(color))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(subject.name, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}


// --- Format Duration Helpers ---

fun formatDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
}

fun formatHoursMinutes(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}

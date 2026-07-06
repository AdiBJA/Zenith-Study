package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GardenPlant
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun CoachScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val plants by viewModel.plants.collectAsState()
    val isGeneratingInsight by viewModel.isGeneratingInsight.collectAsState()
    val aiStudyInsight by viewModel.aiStudyInsight.collectAsState()
    val isGeneratingPlan by viewModel.isGeneratingPlan.collectAsState()
    val aiExamPlan by viewModel.aiExamPlan.collectAsState()

    var showAdoptSeedDialog by remember { mutableStateOf(false) }
    var activeTabState by remember { mutableIntStateOf(0) } // 0 = Focus Garden, 1 = AI Coach Buddy

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // --- Segmented Toggle Bar (Garden vs AI Coach) ---
        item {
            TabRow(
                selectedTabIndex = activeTabState,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = activeTabState == 0,
                    onClick = { activeTabState = 0 },
                    text = { Text("Focus Garden 🌲", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = activeTabState == 1,
                    onClick = { activeTabState = 1 },
                    text = { Text("AI Study Companion 🧠", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }
        }

        if (activeTabState == 0) {
            // ================== GARDEN MODE ==================
            item {
                Text(
                    text = "Grow Your Virtual Study Forest 🌲🌱",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Earn focus points (XP) for every minute you study. Use them to evolve your beautiful digital plants!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plants.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No plants in your garden. Plant a seed to start!", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAdoptSeedDialog = true }) {
                                Text("Plant a Seed")
                            }
                        }
                    }
                }
            } else {
                items(plants) { plant ->
                    GardenPlantCard(
                        plant = plant,
                        onWater = { viewModel.waterActivePlant(plant) },
                        onDelete = { viewModel.deletePlant(plant) }
                    )
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showAdoptSeedDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Plant Another Seed", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

        } else {
            // ================== AI COACH MODE ==================
            item {
                Text(
                    text = "Personalized AI Study Buddy 🧠✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Powered by Gemini 3.5 Flash. Analyze focus analytics, set scientifically proven strategies, and generate ultimate exam planners.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- Module A: Daily Stats Report & Coach ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Ask Your Focus Coach", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Generate instant focus audit & cognitive study tips", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.generateAIStudyInsight() },
                            modifier = Modifier.fillMaxWidth().testTag("ai_insight_button"),
                            enabled = !isGeneratingInsight
                        ) {
                            if (isGeneratingInsight) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Focus Patterns...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Cognitive Focus Report", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (aiStudyInsight.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = aiStudyInsight,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp).testTag("ai_insight_text"),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Module B: Smart Exam Prep Planner ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Ultimate Exam Prep Planner", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Build customized daily review timelines & active recall spacing lists", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var examTopic by remember { mutableStateOf("") }
                        var daysStr by remember { mutableStateOf("5") }
                        var hoursStr by remember { mutableStateOf("3") }

                        OutlinedTextField(
                            value = examTopic,
                            onValueChange = { examTopic = it },
                            label = { Text("What subject/exam are you studying? (e.g. Calculus III)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = daysStr,
                                onValueChange = { daysStr = it },
                                label = { Text("Days Remaining") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = hoursStr,
                                onValueChange = { hoursStr = it },
                                label = { Text("Daily Hours") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val days = daysStr.toIntOrNull() ?: 5
                                val hrs = hoursStr.toIntOrNull() ?: 3
                                if (examTopic.isNotBlank()) {
                                    viewModel.generateAIExamPrepPlan(examTopic, days, hrs)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().testTag("ai_planner_button"),
                            enabled = !isGeneratingPlan && examTopic.isNotBlank()
                        ) {
                            if (isGeneratingPlan) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Structuring Spaced Guide...")
                            } else {
                                Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Smart Timed Guide", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (aiExamPlan.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = aiExamPlan,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp).testTag("ai_planner_text"),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Module C: AI Interactive Quiz Generator ---
            item {
                QuizModule(viewModel = viewModel)
            }

            // --- Module D: AI Concept Explainer ---
            item {
                ConceptExplainerModule(viewModel = viewModel)
            }

            // --- Module E: Personalized Goals Tips ---
            item {
                PersonalizedTipsModule(viewModel = viewModel)
            }
        }
    }

    if (showAdoptSeedDialog) {
        AdoptSeedDialog(
            onDismiss = { showAdoptSeedDialog = false },
            onSave = { name, type ->
                viewModel.addPlant(name, type)
                showAdoptSeedDialog = false
            }
        )
    }
}

// --- Companion Sub-widgets ---

@Composable
fun GardenPlantCard(
    plant: GardenPlant,
    onWater: () -> Unit,
    onDelete: () -> Unit
) {
    // Beautiful emoji represent plant type & stage
    val (plantEmoji, stageLabel) = remember(plant.type, plant.stage) {
        val typeLabel = when (plant.type) {
            "BONSAI" -> listOf("🌰 (Bonsai Seed)", "🌱 (Bonsai Sprout)", "🌿 (Bonsai Sapling)", "🌲 (Growing Bonsai)", "🌳 (Fully Bloomed Bonsai)")
            "ORCHID" -> listOf("🫘 (Orchid Seed)", "🌱 (Orchid Sprout)", "🍃 (Orchid Bud)", "🌸 (Growing Orchid)", "💐 (Radiant Bloomed Orchid)")
            "CACTUS" -> listOf("⚪ (Cactus Seed)", "🌵 (Tiny Sprout)", "🌵 (Growing Cactus)", "🌵🌸 (Blooming Cactus)", "🏝️ (Imperial Desert Cactus)")
            else -> listOf("🌱 (Seed)", "🌱 (Sprout)", "🌿 (Sapling)", "🌿 (Growing)", "🌸 (Bloomed)")
        }
        val stageIndex = (plant.stage - 1).coerceIn(0, 4)
        val emoji = typeLabel[stageIndex]
        val desc = when (plant.stage) {
            1 -> "Seed Stage"
            2 -> "Sprout Stage"
            3 -> "Sapling Stage"
            4 -> "Budding / Mature"
            else -> "Full Bloom Masterpiece 🏆"
        }
        Pair(emoji, desc)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = plantEmoji.take(2), fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = plant.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$stageLabel (Stage ${plant.stage}/5)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove plant", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar to next stage
            val fraction = (plant.xp.toFloat() / plant.nextStageXp).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Focus Dew Progress", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${plant.xp} / ${plant.nextStageXp} XP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Water Plant (Gives daily interactions)
            Button(
                onClick = onWater,
                modifier = Modifier.fillMaxWidth().testTag("water_plant_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                enabled = plant.stage < 5
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (plant.stage >= 5) "Fully Grown Masterpiece!" else "Water Plant (+15 XP)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AdoptSeedDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("BONSAI") }

    val plantTypes = listOf(
        "BONSAI" to "Sacred Bonsai Tree 🌲",
        "ORCHID" to "Cosmic Orchid Flower 🌸",
        "CACTUS" to "Cozy Sand Cactus 🌵"
    )

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
                    text = "Adopt a New Seed 🌱",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What is your plant name? (e.g. Zenny)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Plant Specimen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plantTypes.forEach { (type, desc) ->
                        val isSelected = selectedType == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(desc, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
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
                            if (name.isNotBlank()) {
                                onSave(name, selectedType)
                            }
                        }
                    ) { Text("Plant Seed") }
                }
            }
        }
    }
}

// ================== BRAND NEW AI STUDY COMPANION MODULES ==================

@Composable
fun QuizModule(viewModel: StudyViewModel) {
    val isGeneratingQuiz by viewModel.isGeneratingQuiz.collectAsState()
    val generatedQuiz by viewModel.generatedQuiz.collectAsState()

    var topic by remember { mutableStateOf("") }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var correctAnswersCount by remember { mutableStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("AI Practice Quiz Generator 📝", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Test your active recall with custom generated quizzes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (generatedQuiz == null && !isGeneratingQuiz) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic or copy-paste study notes...") },
                    placeholder = { Text("e.g., Photosynthesis, Newton's Laws, SQL Joins") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (topic.isNotBlank()) {
                            viewModel.generateAIQuiz(topic)
                            currentQuestionIndex = 0
                            selectedAnswerIndex = null
                            correctAnswersCount = 0
                            isQuizFinished = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("generate_quiz_button"),
                    enabled = topic.isNotBlank()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Practice Quiz", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (isGeneratingQuiz) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("AI is assembling your practice questions...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                val quiz = generatedQuiz!!
                if (!isQuizFinished) {
                    val question = quiz.questions[currentQuestionIndex]
                    Text(
                        text = "Topic: ${quiz.topic.uppercase()}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of ${quiz.questions.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswerIndex == index
                            val isCorrect = question.correctAnswerIndex == index
                            val optionBg = when {
                                selectedAnswerIndex != null && isCorrect -> Color.Green.copy(alpha = 0.15f)
                                selectedAnswerIndex != null && isSelected && !isCorrect -> Color.Red.copy(alpha = 0.15f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val optionBorder = when {
                                selectedAnswerIndex != null && isCorrect -> Color.Green
                                selectedAnswerIndex != null && isSelected && !isCorrect -> Color.Red
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(optionBg)
                                    .border(1.dp, optionBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = selectedAnswerIndex == null) {
                                        selectedAnswerIndex = index
                                        if (isCorrect) correctAnswersCount++
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ('A' + index).toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = option, fontSize = 12.sp, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (selectedAnswerIndex != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (selectedAnswerIndex == question.correctAnswerIndex) "🎉 Correct!" else "❌ Incorrect",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedAnswerIndex == question.correctAnswerIndex) Color.Green else Color.Red
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = question.explanation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (currentQuestionIndex + 1 < quiz.questions.size) {
                                    currentQuestionIndex++
                                    selectedAnswerIndex = null
                                } else {
                                    isQuizFinished = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentQuestionIndex + 1 < quiz.questions.size) "Next Question" else "Finish Quiz",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Result Screen
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "Quiz Completed! 🏆",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "You scored $correctAnswersCount out of ${quiz.questions.size} correct.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentQuestionIndex = 0
                                    selectedAnswerIndex = null
                                    correctAnswersCount = 0
                                    isQuizFinished = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry Quiz")
                            }

                            Button(
                                onClick = {
                                    viewModel.generateAIQuiz("") // clear
                                    topic = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("New Topic", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConceptExplainerModule(viewModel: StudyViewModel) {
    val isExplainingConcept by viewModel.isExplainingConcept.collectAsState()
    val conceptExplanation by viewModel.conceptExplanation.collectAsState()

    var concept by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HelpCenter, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("AI Complex Concept Explainer 🧠", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Get instant simple analogies and key takeaways", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = concept,
                onValueChange = { concept = it },
                label = { Text("What concept are you struggling with?") },
                placeholder = { Text("e.g., Recursion, Photosynthesis, Black Holes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (concept.isNotBlank()) {
                        viewModel.explainConcept(concept)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("explain_concept_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = concept.isNotBlank() && !isExplainingConcept
            ) {
                if (isExplainingConcept) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simplifying concept...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explain Simply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (conceptExplanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = conceptExplanation,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp).testTag("concept_explanation_text"),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalizedTipsModule(viewModel: StudyViewModel) {
    val isGeneratingTips by viewModel.isGeneratingTips.collectAsState()
    val aiTips by viewModel.aiTips.collectAsState()
    val loggedInUser by viewModel.loggedInUser.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Personalized Study Tips & Goals Coach 🎯", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Get tips tailored to your desired grade and deadlines", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            loggedInUser?.let { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Goal: Focus on ${user.focusSubject}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Target Exam: ${user.examDate} | Grade: ${user.desiredGrade}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "Target: ${user.weeklyHoursTarget}h/wk", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.generateAIPerformanceTips() },
                modifier = Modifier.fillMaxWidth().testTag("get_tips_button"),
                enabled = !isGeneratingTips
            ) {
                if (isGeneratingTips) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Coaching...")
                } else {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Personalized Tips", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (aiTips.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = aiTips,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp).testTag("ai_tips_text"),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}


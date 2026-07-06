package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GroupMessage
import com.example.data.JoinedGroup
import com.example.ui.viewmodel.GroupMemberSimulation
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val joinedGroups by viewModel.joinedGroups.collectAsState()
    val groupStudyingMembers by viewModel.groupStudyingMembers.collectAsState()
    val groupFloatingEmojis by viewModel.groupFloatingEmojis.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val activeSubject by viewModel.activeSubject.collectAsState()
    val activeGroupMessages by viewModel.activeGroupMessages.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<JoinedGroup?>(null) }
    var activeSubTabState by remember { mutableIntStateOf(0) } // 0 = Live, 1 = Forum, 2 = Dashboard & Nickname

    // Auto-select first group if none selected
    LaunchedEffect(joinedGroups) {
        if (selectedGroup == null && joinedGroups.isNotEmpty()) {
            selectedGroup = joinedGroups[0]
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
        ) {
            // --- Joined Groups Horizontal Tab Selector ---
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Study Rooms 🏫",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showCreateGroupDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (joinedGroups.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = "You haven't joined any study rooms yet! Join or create one below.",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        ScrollableTabRow(
                            selectedTabIndex = joinedGroups.indexOf(selectedGroup).coerceAtLeast(0),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {}
                        ) {
                            joinedGroups.forEach { group ->
                                val isSelected = selectedGroup?.id == group.id
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { selectedGroup = group }
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = group.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Active Room Details Header ---
            selectedGroup?.let { group ->
                // Inform VM of the selected active group for messages sync
                item {
                    LaunchedEffect(group.id) {
                        viewModel.setActiveGroupId(group.id)
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = group.category.uppercase(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${group.membersCount} Online Peers",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Target: ${formatHoursMinutes(group.targetSecondsPerDay)} / Day",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Room Internal Tab Selector ---
                item {
                    TabRow(
                        selectedTabIndex = activeSubTabState,
                        containerColor = Color.Transparent,
                        divider = {},
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Tab(
                            selected = activeSubTabState == 0,
                            onClick = { activeSubTabState = 0 },
                            text = { Text("Live Study 📚", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeSubTabState == 1,
                            onClick = { activeSubTabState = 1 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Forum Board 💬", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    if (activeGroupMessages.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = activeGroupMessages.size.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = activeSubTabState == 2,
                            onClick = { activeSubTabState = 2 },
                            text = { Text("Dashboard 🏆", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // --- TAB CONTENT ---
                if (activeSubTabState == 0) {
                    // ================== TAB 0: LIVE STUDY ==================
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isTimerRunning) Color.Green else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your Status: " + if (isTimerRunning) "STUDYING (${activeSubject?.name ?: "Focus"})" else "IDLE / BREAK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Send Motivation 👇",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Live Study Library 📚",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(groupStudyingMembers) { member ->
                        LivePeerCardItem(member = member)
                    }

                } else if (activeSubTabState == 1) {
                    // ================== TAB 1: COLLABORATIVE FORUM BOARD ==================
                    item {
                        ForumPostForm(groupId = group.id, viewModel = viewModel)
                    }

                    if (activeGroupMessages.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "The discussion board is quiet. Ask a question or share a useful study link/resource to begin!",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(activeGroupMessages.reversed()) { msg ->
                            ForumMessageCard(msg = msg)
                        }
                    }

                } else {
                    // ================== TAB 2: PROGRESS LEADERBOARD & NICKNAME ==================
                    item {
                        GroupNicknameEditor(groupId = group.id, viewModel = viewModel)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Room Focus Leaderboard 🏆",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Synchronized study totals for the last 24 hours. Keep pushing toward the target!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Let's list peers and include the user under their custom nickname!
                                val userNickname = viewModel.getGroupNicknameForDisplay(group.id)
                                val userSecondsStudied = activeSubject?.todayStudySeconds ?: 0L

                                val userAsMember = GroupMemberSimulation(
                                    name = "$userNickname (You)",
                                    isStudying = isTimerRunning,
                                    activeSubject = activeSubject?.name ?: "None",
                                    todaySeconds = userSecondsStudied,
                                    currentSessionSeconds = 0L
                                )

                                val leaderboardList = (groupStudyingMembers + userAsMember)
                                    .sortedByDescending { it.todaySeconds }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    leaderboardList.forEachIndexed { index, member ->
                                        val progress = if (group.targetSecondsPerDay > 0) {
                                            (member.todaySeconds.toFloat() / group.targetSecondsPerDay).coerceIn(0f, 1f)
                                        } else 0f

                                        val metTarget = member.todaySeconds >= group.targetSecondsPerDay

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Rank Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (index) {
                                                            0 -> Color(0xFFFFD700) // Gold
                                                            1 -> Color(0xFFC0C0C0) // Silver
                                                            2 -> Color(0xFFCD7F32) // Bronze
                                                            else -> MaterialTheme.colorScheme.primaryContainer
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (index + 1).toString(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = member.name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (member.name.contains("(You)")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (metTarget) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.OfflineBolt,
                                                                contentDescription = "Target Met",
                                                                tint = Color(0xFFFFD700),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = formatHoursMinutes(member.todaySeconds),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = progress,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(CircleShape),
                                                    color = if (metTarget) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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
        }

        // --- Live Overlay: Floating Emojis ---
        if (activeSubTabState == 0) {
            groupFloatingEmojis.forEach { floating ->
                FloatingEmojiItem(floating = floating)
            }

            // --- Fixed Bottom Floating Action Social Interactions Panel ---
            SocialFloatingPanel(
                onEmojiClicked = { emoji ->
                    viewModel.sendGroupEmoji(emoji)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    if (showCreateGroupDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateGroupDialog = false },
            onSave = { name, cat, desc, hrs ->
                viewModel.joinCustomGroup(name, cat, desc, hrs)
                showCreateGroupDialog = false
            }
        )
    }
}

// --- Companion Components ---

@Composable
fun ForumPostForm(
    groupId: String,
    viewModel: StudyViewModel
) {
    var postText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    val categories = listOf("General", "Question", "Resource")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Share with the Group 💬",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = postText,
                onValueChange = { postText = it },
                placeholder = { Text("Ask a question, share a useful study link, or send motivation...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (postText.isNotBlank()) {
                            viewModel.postGroupMessage(groupId, postText, selectedCategory)
                            postText = ""
                        }
                    },
                    enabled = postText.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ForumMessageCard(msg: GroupMessage) {
    val categoryColor = when (msg.category) {
        "Question" -> Color(0xFFE57373) // soft red
        "Resource" -> Color(0xFF81C784) // soft green
        else -> Color(0xFF64B5F6) // soft blue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = msg.senderName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Category Tag
                Box(
                    modifier = Modifier
                        .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = msg.category.uppercase(),
                        fontSize = 8.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = msg.messageText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun GroupNicknameEditor(
    groupId: String,
    viewModel: StudyViewModel
) {
    var inputNickname by remember { mutableStateOf("") }
    val currentNickname = viewModel.getGroupNicknameForDisplay(groupId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "My Custom Group Nickname 📛",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Associate different nicknames with different study groups! (Unique nickname per group). Current nickname in this room: \"$currentNickname\"",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputNickname,
                    onValueChange = { inputNickname = it },
                    placeholder = { Text("Enter a custom room nickname...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (inputNickname.isNotBlank()) {
                            viewModel.setGroupNickname(groupId, inputNickname)
                            inputNickname = ""
                        }
                    },
                    enabled = inputNickname.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LivePeerCardItem(member: GroupMemberSimulation) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alphaAnim by if (member.isStudying) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (member.isStudying) Color.Green.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isStudying) Color.Green.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circle with status dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Ticking glowing dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (member.isStudying) Color.Green.copy(alpha = alphaAnim) else Color.Gray)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (member.isStudying) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Green.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("STUDYING", fontSize = 8.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("BREAK", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (member.isStudying) "Focusing on: ${member.activeSubject}" else "In deep rest",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Today: " + formatHoursMinutes(member.todaySeconds),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (member.isStudying) {
                    Text(
                        text = "Session: " + formatHoursMinutes(member.currentSessionSeconds),
                        fontSize = 9.sp,
                        color = Color.Green
                    )
                }
            }
        }
    }
}

@Composable
fun SocialFloatingPanel(
    onEmojiClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(12.dp, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val emojis = listOf("🔥", "👏", "⚡", "💡", "☕", "🙌")
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onEmojiClicked(emoji) }
                        .padding(4.dp)
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun FloatingEmojiItem(floating: com.example.ui.viewmodel.FloatingEmoji) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(floating) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = LinearOutSlowInEasing)
        )
    }

    val transparency = 1f - animProgress.value
    val offsetY = floating.offsetY * animProgress.value
    val offsetX = floating.offsetX * animProgress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = floating.emoji,
            fontSize = 32.sp,
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .shadow(elevation = 2.dp, CircleShape)
                .background(Color.Transparent)
                .clip(CircleShape)
                .testTag("floating_emoji"),
            color = Color.Unspecified.copy(alpha = transparency)
        )
    }
}

@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Habit/Routine") }
    var desc by remember { mutableStateOf("") }
    var targetHours by remember { mutableStateOf("4") }

    val categories = listOf("University/Tech", "Habit/Routine", "Exam Prep", "Middle/High School", "General")

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
                    text = "Create Study Room",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Study Room Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description/Goal (e.g. Grinding math)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it },
                    label = { Text("Daily Group Target (Hours)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(cat).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { category ->
                        val isSelected = cat == category
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                .clickable { cat = category }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
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
                            val hrs = targetHours.toIntOrNull() ?: 4
                            if (name.isNotBlank()) {
                                onSave(name, cat, desc, hrs)
                            }
                        }
                    ) { Text("Create & Join") }
                }
            }
        }
    }
}

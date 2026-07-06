package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val studyViewModel: StudyViewModel = viewModel()
                val loggedInUser by studyViewModel.loggedInUser.collectAsState()
                var currentTabState by remember { mutableIntStateOf(0) }

                if (loggedInUser == null) {
                    LoginScreen(viewModel = studyViewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "ZENITH STUDY",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                                actions = {
                                    Text(
                                        text = loggedInUser?.nickname ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { studyViewModel.logout() },
                                        modifier = Modifier.testTag("logout_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "Logout",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTabState == 0,
                                onClick = { currentTabState = 0 },
                                icon = { Icon(Icons.Default.Timer, contentDescription = "Timer", modifier = Modifier.size(24.dp)) },
                                label = { Text("Timer", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_timer_tab")
                            )

                            NavigationBarItem(
                                selected = currentTabState == 1,
                                onClick = { currentTabState = 1 },
                                icon = { Icon(Icons.Default.Assignment, contentDescription = "Planner", modifier = Modifier.size(24.dp)) },
                                label = { Text("Planner", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_planner_tab")
                            )

                            NavigationBarItem(
                                selected = currentTabState == 2,
                                onClick = { currentTabState = 2 },
                                icon = { Icon(Icons.Default.Group, contentDescription = "Rooms", modifier = Modifier.size(24.dp)) },
                                label = { Text("Rooms", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_rooms_tab")
                            )

                            NavigationBarItem(
                                selected = currentTabState == 3,
                                onClick = { currentTabState = 3 },
                                icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Stats", modifier = Modifier.size(24.dp)) },
                                label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_stats_tab")
                            )

                            NavigationBarItem(
                                selected = currentTabState == 4,
                                onClick = { currentTabState = 4 },
                                icon = { Icon(Icons.Default.Psychology, contentDescription = "AI Coach & Garden", modifier = Modifier.size(24.dp)) },
                                label = { Text("Coach", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("nav_coach_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (currentTabState) {
                        0 -> TimerScreen(viewModel = studyViewModel, modifier = modifier)
                        1 -> PlannerScreen(viewModel = studyViewModel, modifier = modifier)
                        2 -> GroupScreen(viewModel = studyViewModel, modifier = modifier)
                        3 -> StatsScreen(viewModel = studyViewModel, modifier = modifier)
                        4 -> CoachScreen(viewModel = studyViewModel, modifier = modifier)
                    }
                }
                }
            }
        }
    }
}

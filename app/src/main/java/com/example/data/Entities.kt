package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val totalStudySeconds: Long = 0,
    val todayStudySeconds: Long = 0,
    val targetSecondsPerDay: Long = 7200 // Default 2 hours
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String // YYYY-MM-DD
)

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskName: String,
    val subjectId: Int? = null,
    val isCompleted: Boolean = false,
    val dateString: String, // YYYY-MM-DD
    val estimatedMinutes: Int = 30
)

@Entity(tableName = "garden_plants")
data class GardenPlant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "BONSAI", "ORCHID", "CACTUS", "SUNFLOWER"
    val stage: Int = 1, // 1 to 5 (Seed, Sprout, Sapling, Growing, Bloomed)
    val xp: Int = 0, // XP gained (1 min study = 1 XP)
    val nextStageXp: Int = 100, // XP needed for next stage
    val dateCreated: String
)

@Entity(tableName = "joined_groups")
data class JoinedGroup(
    @PrimaryKey val id: String, // unique id of group
    val name: String,
    val category: String,
    val description: String,
    val targetSecondsPerDay: Long,
    val todaySecondsStudied: Long = 0,
    val membersCount: Int = 12
)

@Entity(tableName = "app_users")
data class AppUser(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val nickname: String,
    val examDate: String = "2026-08-01",
    val desiredGrade: String = "A+",
    val focusSubject: String = "Coding & AI",
    val weeklyHoursTarget: Int = 15
)

@Entity(tableName = "group_messages")
data class GroupMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "General" // "General", "Question", "Resource"
)

@Entity(tableName = "group_nicknames")
data class GroupNickname(
    val groupId: String,
    val username: String,
    val nickname: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)


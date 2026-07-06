package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- Subjects ---
    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("UPDATE subjects SET todayStudySeconds = todayStudySeconds + :seconds, totalStudySeconds = totalStudySeconds + :seconds WHERE id = :id")
    suspend fun addStudySeconds(id: Int, seconds: Long)

    @Query("UPDATE subjects SET todayStudySeconds = 0")
    suspend fun resetTodayStudySeconds()


    // --- Study Sessions ---
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE dateString = :dateString")
    fun getSessionsForDate(dateString: String): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)


    // --- Tasks ---
    @Query("SELECT * FROM todo_tasks WHERE dateString = :dateString ORDER BY id ASC")
    fun getTasksForDate(dateString: String): Flow<List<TodoTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoTask): Long

    @Update
    suspend fun updateTask(task: TodoTask)

    @Delete
    suspend fun deleteTask(task: TodoTask)


    // --- Garden Plants ---
    @Query("SELECT * FROM garden_plants ORDER BY id ASC")
    fun getAllPlants(): Flow<List<GardenPlant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: GardenPlant): Long

    @Update
    suspend fun updatePlant(plant: GardenPlant)

    @Delete
    suspend fun deletePlant(plant: GardenPlant)


    // --- Joined Groups ---
    @Query("SELECT * FROM joined_groups")
    fun getJoinedGroups(): Flow<List<JoinedGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: JoinedGroup)

    @Query("DELETE FROM joined_groups WHERE id = :id")
    suspend fun leaveGroup(id: String)

    // --- Users ---
    @Query("SELECT * FROM app_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): AppUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AppUser)

    @Update
    suspend fun updateUser(user: AppUser)

    // --- Group Messages ---
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroup(groupId: String): Flow<List<GroupMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMessage(message: GroupMessage)

    // --- Group Nicknames ---
    @Query("SELECT * FROM group_nicknames WHERE groupId = :groupId AND username = :username LIMIT 1")
    suspend fun getGroupNickname(groupId: String, username: String): GroupNickname?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupNickname(nickname: GroupNickname)
}

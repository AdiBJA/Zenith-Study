package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {

    // --- Subjects ---
    val allSubjects: Flow<List<Subject>> = studyDao.getAllSubjects()

    suspend fun getSubjectById(id: Int): Subject? = studyDao.getSubjectById(id)

    suspend fun insertSubject(subject: Subject): Long = studyDao.insertSubject(subject)

    suspend fun updateSubject(subject: Subject) = studyDao.updateSubject(subject)

    suspend fun deleteSubject(subject: Subject) = studyDao.deleteSubject(subject)

    suspend fun addStudySeconds(id: Int, seconds: Long) = studyDao.addStudySeconds(id, seconds)

    suspend fun resetTodayStudySeconds() = studyDao.resetTodayStudySeconds()


    // --- Study Sessions ---
    val allSessions: Flow<List<StudySession>> = studyDao.getAllSessions()

    fun getSessionsForDate(dateString: String): Flow<List<StudySession>> =
        studyDao.getSessionsForDate(dateString)

    suspend fun insertSession(session: StudySession): Long = studyDao.insertSession(session)

    suspend fun deleteSessionById(id: Int) = studyDao.deleteSessionById(id)


    // --- Tasks ---
    fun getTasksForDate(dateString: String): Flow<List<TodoTask>> =
        studyDao.getTasksForDate(dateString)

    suspend fun insertTask(task: TodoTask): Long = studyDao.insertTask(task)

    suspend fun updateTask(task: TodoTask) = studyDao.updateTask(task)

    suspend fun deleteTask(task: TodoTask) = studyDao.deleteTask(task)


    // --- Garden Plants ---
    val allPlants: Flow<List<GardenPlant>> = studyDao.getAllPlants()

    suspend fun insertPlant(plant: GardenPlant): Long = studyDao.insertPlant(plant)

    suspend fun updatePlant(plant: GardenPlant) = studyDao.updatePlant(plant)

    suspend fun deletePlant(plant: GardenPlant) = studyDao.deletePlant(plant)


    // --- Joined Groups ---
    val joinedGroups: Flow<List<JoinedGroup>> = studyDao.getJoinedGroups()

    suspend fun insertGroup(group: JoinedGroup) = studyDao.insertGroup(group)

    suspend fun leaveGroup(id: String) = studyDao.leaveGroup(id)

    // --- Users ---
    suspend fun getUserByUsername(username: String): AppUser? = studyDao.getUserByUsername(username)

    suspend fun insertUser(user: AppUser) = studyDao.insertUser(user)

    suspend fun updateUser(user: AppUser) = studyDao.updateUser(user)

    // --- Group Messages ---
    fun getMessagesForGroup(groupId: String): Flow<List<GroupMessage>> = studyDao.getMessagesForGroup(groupId)

    suspend fun insertGroupMessage(message: GroupMessage) = studyDao.insertGroupMessage(message)

    // --- Group Nicknames ---
    suspend fun getGroupNickname(groupId: String, username: String): GroupNickname? = studyDao.getGroupNickname(groupId, username)

    suspend fun insertGroupNickname(nickname: GroupNickname) = studyDao.insertGroupNickname(nickname)
}

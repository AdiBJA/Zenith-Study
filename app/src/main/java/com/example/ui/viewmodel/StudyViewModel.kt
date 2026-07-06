package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.ui.components.FocusSoundSynthesizer
import com.example.ui.components.SoundPreset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*

enum class TimerMode { STOPWATCH, POMODORO }
enum class PomodoroState { WORK, BREAK }

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = StudyRepository(database.studyDao())

    // Date formatting helpers
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String get() = dateFormatter.format(Date())

    // --- Flow States ---
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plants: StateFlow<List<GardenPlant>> = repository.allPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val joinedGroups: StateFlow<List<JoinedGroup>> = repository.joinedGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDate = MutableStateFlow(todayDateString)
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // Tasks for the currently selected planner date
    val tasks: StateFlow<List<TodoTask>> = _currentDate
        .flatMapLatest { date -> repository.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Active Study Timer States ---
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _activeSubject = MutableStateFlow<Subject?>(null)
    val activeSubject: StateFlow<Subject?> = _activeSubject.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.STOPWATCH)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _pomodoroState = MutableStateFlow(PomodoroState.WORK)
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private val _pomodoroSecondsLeft = MutableStateFlow(1500L) // Default 25 min (1500 sec)
    val pomodoroSecondsLeft: StateFlow<Long> = _pomodoroSecondsLeft.asStateFlow()


    // --- Simulated Study Group States ---
    private val _groupStudyingMembers = MutableStateFlow<List<GroupMemberSimulation>>(emptyList())
    val groupStudyingMembers: StateFlow<List<GroupMemberSimulation>> = _groupStudyingMembers.asStateFlow()

    private val _groupFloatingEmojis = MutableStateFlow<List<FloatingEmoji>>(emptyList())
    val groupFloatingEmojis: StateFlow<List<FloatingEmoji>> = _groupFloatingEmojis.asStateFlow()


    // --- Audio Soundscape States ---
    private val _activeSoundPreset = MutableStateFlow(SoundPreset.NONE)
    val activeSoundPreset: StateFlow<SoundPreset> = _activeSoundPreset.asStateFlow()

    private val _audioVolume = MutableStateFlow(0.5f)
    val audioVolume: StateFlow<Float> = _audioVolume.asStateFlow()


    // --- AI Integration States ---
    private val _isGeneratingInsight = MutableStateFlow(false)
    val isGeneratingInsight: StateFlow<Boolean> = _isGeneratingInsight.asStateFlow()

    private val _aiStudyInsight = MutableStateFlow("")
    val aiStudyInsight: StateFlow<String> = _aiStudyInsight.asStateFlow()

    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan.asStateFlow()

    private val _aiExamPlan = MutableStateFlow("")
    val aiExamPlan: StateFlow<String> = _aiExamPlan.asStateFlow()

    // --- Login & Registration States ---
    private val _loggedInUser = MutableStateFlow<AppUser?>(null)
    val loggedInUser: StateFlow<AppUser?> = _loggedInUser.asStateFlow()

    // --- Active Group ID ---
    private val _activeGroupId = MutableStateFlow("group_stem")
    val activeGroupId: StateFlow<String> = _activeGroupId.asStateFlow()

    // --- Group Messages Reactive Flow ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGroupMessages: StateFlow<List<GroupMessage>> = _activeGroupId
        .flatMapLatest { id -> repository.getMessagesForGroup(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User's Nickname Map (cached in-memory for immediate UI updates) ---
    private val _groupNicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupNicknames: StateFlow<Map<String, String>> = _groupNicknames.asStateFlow()

    // --- AI Quiz Generator States ---
    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _generatedQuiz = MutableStateFlow<StudyQuiz?>(null)
    val generatedQuiz: StateFlow<StudyQuiz?> = _generatedQuiz.asStateFlow()

    // --- Concept Explainer States ---
    private val _isExplainingConcept = MutableStateFlow(false)
    val isExplainingConcept: StateFlow<Boolean> = _isExplainingConcept.asStateFlow()

    private val _conceptExplanation = MutableStateFlow("")
    val conceptExplanation: StateFlow<String> = _conceptExplanation.asStateFlow()

    // --- Personalized Tips States ---
    private val _isGeneratingTips = MutableStateFlow(false)
    val isGeneratingTips: StateFlow<Boolean> = _isGeneratingTips.asStateFlow()

    private val _aiTips = MutableStateFlow("")
    val aiTips: StateFlow<String> = _aiTips.asStateFlow()



    // Timer tick job
    private var timerJob: Job? = null
    // Incremental seconds since last DB save (to prevent loss of time if app crashes)
    private var unsavedSeconds = 0L

    init {
        // Populate database with default values if empty on first launch
        viewModelScope.launch {
            subjects.first() // Wait for DB load
            if (subjects.value.isEmpty()) {
                val defaultSubjects = listOf(
                    Subject(name = "Mathematics 📐", colorHex = "#FF5252", targetSecondsPerDay = 7200),
                    Subject(name = "Coding & AI 💻", colorHex = "#00E5FF", targetSecondsPerDay = 10800),
                    Subject(name = "Physics & Chemistry ⚛️", colorHex = "#FFEB3B", targetSecondsPerDay = 5400),
                    Subject(name = "Literature & English 📚", colorHex = "#E040FB", targetSecondsPerDay = 3600)
                )
                defaultSubjects.forEach { repository.insertSubject(it) }
            }

            plants.first()
            if (plants.value.isEmpty()) {
                val defaultPlant = GardenPlant(
                    name = "First Sprout 🌱",
                    type = "BONSAI",
                    stage = 1,
                    xp = 0,
                    nextStageXp = 100,
                    dateCreated = todayDateString
                )
                repository.insertPlant(defaultPlant)
            }

            joinedGroups.first()
            if (joinedGroups.value.isEmpty()) {
                repository.insertGroup(
                    JoinedGroup(
                        id = "group_stem",
                        name = "STEM & Code Pioneers ⚛️💻",
                        category = "University/Tech",
                        description = "Studying advanced math, computer science, and engineering together daily. Weekly target: 20hrs.",
                        targetSecondsPerDay = 14400,
                        membersCount = 42
                    )
                )
                repository.insertGroup(
                    JoinedGroup(
                        id = "group_morning",
                        name = "5 AM Morning Owls 🦉⛅",
                        category = "Habit/Routine",
                        description = "Get a head start on your goals before the world wakes up! Focus is key.",
                        targetSecondsPerDay = 7200,
                        membersCount = 18
                    )
                )
            }

            // Seed default AppUser for instant out-of-the-box usage
            val defaultUser = repository.getUserByUsername("student")
            if (defaultUser == null) {
                val seedUser = AppUser(
                    username = "student",
                    passwordHash = "study123",
                    nickname = "ZenMaster",
                    examDate = "2026-08-15",
                    desiredGrade = "A",
                    focusSubject = "Coding & AI",
                    weeklyHoursTarget = 15
                )
                repository.insertUser(seedUser)
                _loggedInUser.value = seedUser
            } else {
                _loggedInUser.value = defaultUser
            }

            // Seed collaborative forum message boards
            val stemMsgs = repository.getMessagesForGroup("group_stem").first()
            if (stemMsgs.isEmpty()) {
                repository.insertGroupMessage(GroupMessage(groupId = "group_stem", senderName = "Sophia ☕", messageText = "Hey guys! Does anyone have a good cheat sheet for eigenvalues and eigenvectors in Linear Algebra? struggling with the diagonalization step.", category = "Question"))
                repository.insertGroupMessage(GroupMessage(groupId = "group_stem", senderName = "Ethan 💻", messageText = "Check out Paul's Online Math Notes. He has a beautiful step-by-step diagonalization section! Here is the link: tutorial.math.lamar.edu", category = "Resource"))
                repository.insertGroupMessage(GroupMessage(groupId = "group_stem", senderName = "Alex 🧠", messageText = "Just started a Pomodoro block. Feel free to join the room timer and focus together!", category = "General"))
            }

            val morningMsgs = repository.getMessagesForGroup("group_morning").first()
            if (morningMsgs.isEmpty()) {
                repository.insertGroupMessage(GroupMessage(groupId = "group_morning", senderName = "Ji-Woo 📚", messageText = "Good morning early birds! Already 30 minutes in. The morning silence is unmatched.", category = "General"))
                repository.insertGroupMessage(GroupMessage(groupId = "group_morning", senderName = "Chloe 🧠", messageText = "Does anyone want to do an active recall session on organic functional groups at 6:00 AM?", category = "Question"))
            }

            // Initialize cache of custom group nicknames for the user
            val cachedNicks = mutableMapOf<String, String>()
            joinedGroups.value.forEach { group ->
                _loggedInUser.value?.let { user ->
                    repository.getGroupNickname(group.id, user.username)?.let { record ->
                        cachedNicks[group.id] = record.nickname
                    }
                }
            }
            _groupNicknames.value = cachedNicks

            // Start simulated group peers behavior ticker
            startGroupSimulationTicker()
        }

        // Start background clock/timer ticker
        startTimerTicker()
    }

    // Set planner selected date
    fun setPlannerDate(date: String) {
        _currentDate.value = date
    }

    // --- Subject Operations ---
    fun addSubject(name: String, colorHex: String, targetHours: Int) {
        viewModelScope.launch {
            repository.insertSubject(
                Subject(
                    name = name,
                    colorHex = colorHex,
                    targetSecondsPerDay = targetHours * 3600L
                )
            )
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            if (_activeSubject.value?.id == subject.id) {
                stopTimer()
            }
            repository.deleteSubject(subject)
        }
    }


    // --- Task Operations ---
    fun addTask(taskName: String, subjectId: Int?, estimatedMin: Int) {
        viewModelScope.launch {
            repository.insertTask(
                TodoTask(
                    taskName = taskName,
                    subjectId = subjectId,
                    dateString = _currentDate.value,
                    estimatedMinutes = estimatedMin
                )
            )
        }
    }

    fun toggleTaskCompletion(task: TodoTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: TodoTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }


    // --- Garden Plant Operations ---
    fun addPlant(name: String, type: String) {
        viewModelScope.launch {
            val targetXp = when(type) {
                "BONSAI" -> 150
                "ORCHID" -> 100
                "CACTUS" -> 50
                else -> 100
            }
            repository.insertPlant(
                GardenPlant(
                    name = name,
                    type = type,
                    stage = 1,
                    xp = 0,
                    nextStageXp = targetXp,
                    dateCreated = todayDateString
                )
            )
        }
    }

    fun deletePlant(plant: GardenPlant) {
        viewModelScope.launch {
            repository.deletePlant(plant)
        }
    }

    fun waterActivePlant(plant: GardenPlant) {
        viewModelScope.launch {
            // Give 15 XP upon watering (simulated once daily interaction)
            val newXp = plant.xp + 15
            var newStage = plant.stage
            var nextXp = plant.nextStageXp
            if (newXp >= plant.nextStageXp && plant.stage < 5) {
                newStage++
                nextXp = (nextXp * 1.8).toInt()
            }
            repository.updatePlant(
                plant.copy(
                    xp = newXp,
                    stage = newStage,
                    nextStageXp = nextXp
                )
            )
        }
    }


    // --- Group Operations ---
    fun joinCustomGroup(name: String, category: String, description: String, targetHours: Int) {
        viewModelScope.launch {
            repository.insertGroup(
                JoinedGroup(
                    id = "group_" + UUID.randomUUID().toString().take(6),
                    name = name,
                    category = category,
                    description = description,
                    targetSecondsPerDay = targetHours * 3600L,
                    membersCount = 1
                )
            )
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            repository.leaveGroup(groupId)
        }
    }


    // --- Timer Management ---
    fun setTimerMode(mode: TimerMode) {
        if (_isTimerRunning.value) return // Disable changing modes while running
        _timerMode.value = mode
    }

    fun startTimer(subject: Subject) {
        if (_isTimerRunning.value) {
            if (_activeSubject.value?.id == subject.id) {
                // Clicking same active subject while running -> pause it
                pauseTimer()
                return
            } else {
                // Switching subjects: stop current, then start new
                stopTimer()
            }
        }

        _activeSubject.value = subject
        _isTimerRunning.value = true

        if (_timerMode.value == TimerMode.POMODORO) {
            _pomodoroState.value = PomodoroState.WORK
            _pomodoroSecondsLeft.value = 1500L // 25 min default
        } else {
            _elapsedSeconds.value = 0L
        }
        unsavedSeconds = 0
    }

    fun pauseTimer() {
        if (!_isTimerRunning.value) return
        _isTimerRunning.value = false
        saveCurrentProgress()
    }

    fun stopTimer() {
        if (_activeSubject.value == null) return
        _isTimerRunning.value = false
        saveCurrentProgress()
        _activeSubject.value = null
        _elapsedSeconds.value = 0L
        _pomodoroSecondsLeft.value = 1500L
    }

    private fun saveCurrentProgress() {
        val subject = _activeSubject.value ?: return
        val currentUnsaved = unsavedSeconds
        if (currentUnsaved <= 0) return

        viewModelScope.launch {
            // Update Subject today/total seconds
            repository.addStudySeconds(subject.id, currentUnsaved)
            
            // Insert Study Session
            repository.insertSession(
                StudySession(
                    subjectId = subject.id,
                    durationSeconds = currentUnsaved,
                    dateString = todayDateString
                )
            )

            // Grant XP to all plants (split or directly to active ones)
            val currentPlants = plants.value
            if (currentPlants.isNotEmpty()) {
                val activePlant = currentPlants.firstOrNull() ?: currentPlants[0]
                val xpEarned = (currentUnsaved / 10).coerceAtLeast(1) // 10 seconds = 1 XP (accelerated for delightful experience)
                var newXp = activePlant.xp + xpEarned.toInt()
                var newStage = activePlant.stage
                var nextXp = activePlant.nextStageXp

                while (newXp >= nextXp && newStage < 5) {
                    newXp -= nextXp
                    newStage++
                    nextXp = (nextXp * 1.8).toInt()
                }

                repository.updatePlant(
                    activePlant.copy(
                        xp = newXp,
                        stage = newStage,
                        nextStageXp = nextXp
                    )
                )
            }
        }
        unsavedSeconds = 0
    }

    private fun startTimerTicker() {
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                if (_isTimerRunning.value && _activeSubject.value != null) {
                    unsavedSeconds++
                    
                    if (_timerMode.value == TimerMode.STOPWATCH) {
                        _elapsedSeconds.value++
                    } else {
                        // Pomodoro Mode countdown
                        if (_pomodoroSecondsLeft.value > 0) {
                            _pomodoroSecondsLeft.value--
                        } else {
                            // Completed active segment
                            withContext(Dispatchers.Main) {
                                switchPomodoroState()
                            }
                        }
                    }

                    // Save incrementally every 1 minute to ensure persistence
                    if (unsavedSeconds >= 60L) {
                        saveCurrentProgress()
                    }
                }
            }
        }
    }

    private fun switchPomodoroState() {
        saveCurrentProgress()
        if (_pomodoroState.value == PomodoroState.WORK) {
            _pomodoroState.value = PomodoroState.BREAK
            _pomodoroSecondsLeft.value = 300L // 5 min break
        } else {
            _pomodoroState.value = PomodoroState.WORK
            _pomodoroSecondsLeft.value = 1500L // 25 min work
        }
    }


    // --- Audio Soundscape Controls ---
    fun selectSoundPreset(preset: SoundPreset) {
        if (_activeSoundPreset.value == preset) {
            _activeSoundPreset.value = SoundPreset.NONE
            FocusSoundSynthesizer.stop()
        } else {
            _activeSoundPreset.value = preset
            FocusSoundSynthesizer.start(preset)
            FocusSoundSynthesizer.volume = _audioVolume.value
        }
    }

    fun setAudioVolume(vol: Float) {
        _audioVolume.value = vol
        FocusSoundSynthesizer.volume = vol
    }


    // --- Live Study Group Peers Simulation ---
    private fun startGroupSimulationTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            val names = listOf("Alex 🧠", "Sophia ☕", "Ethan 💻", "Ji-Woo 📚", "Mei-Ling ⚛️", "Mateo 🎨", "Chloe 🧠", "Liam ⚡")
            val subjectsSim = listOf("Advanced Coding", "Organic Chem", "Calculus III", "English Literature", "World History", "AI Alignment")

            // Initialize 5 random active peers
            val initialMembers = List(5) { i ->
                GroupMemberSimulation(
                    name = names[i % names.size],
                    isStudying = Random().nextBoolean(),
                    activeSubject = subjectsSim[Random().nextInt(subjectsSim.size)],
                    todaySeconds = (3600 + Random().nextInt(14400)).toLong(),
                    currentSessionSeconds = (600 + Random().nextInt(3600)).toLong()
                )
            }
            _groupStudyingMembers.value = initialMembers

            while (isActive) {
                delay(3000) // Ticks every 3 seconds to update progress
                val currentList = _groupStudyingMembers.value.map { member ->
                    if (member.isStudying) {
                        // Tick up their active study counters
                        val updatedSession = member.currentSessionSeconds + 3
                        val updatedToday = member.todaySeconds + 3
                        
                        // 2% chance of taking a break
                        val stillStudying = Random().nextInt(100) > 2
                        member.copy(
                            currentSessionSeconds = if (stillStudying) updatedSession else 0,
                            todaySeconds = updatedToday,
                            isStudying = stillStudying
                        )
                    } else {
                        // 10% chance of resuming study
                        val startedStudying = Random().nextInt(10) == 0
                        member.copy(
                            isStudying = startedStudying,
                            activeSubject = if (startedStudying) subjectsSim[Random().nextInt(subjectsSim.size)] else member.activeSubject,
                            currentSessionSeconds = if (startedStudying) 3 else 0
                        )
                    }
                }
                _groupStudyingMembers.value = currentList

                // Keep cleaning floating emojis
                val currentTime = System.currentTimeMillis()
                _groupFloatingEmojis.update { list ->
                    list.filter { it.expiryTime > currentTime }
                }
            }
        }
    }

    fun sendGroupEmoji(emoji: String) {
        val count = 1 + Random().nextInt(3)
        val newEmojis = List(count) {
            FloatingEmoji(
                emoji = emoji,
                offsetX = -100f + Random().nextFloat() * 200f,
                offsetY = -200f - Random().nextFloat() * 400f,
                expiryTime = System.currentTimeMillis() + 1500 // 1.5 second lifespans
            )
        }
        _groupFloatingEmojis.update { it + newEmojis }
    }


    // --- Gemini AI Integration Queries ---
    fun generateAIStudyInsight() {
        viewModelScope.launch {
            _isGeneratingInsight.value = true
            val currentSubjects = subjects.value
            val currentSessions = sessions.value
            val currentTasks = tasks.value

            val statsPrompt = StringBuilder()
            statsPrompt.append("You are an expert AI Study Coach and Peak Performance Scientist.\n")
            statsPrompt.append("Review my study progress and provide customized, actionable focus coaching, study tips, and positive reinforcement.\n\n")
            
            statsPrompt.append("My Subjects and Study Time Today:\n")
            if (currentSubjects.isEmpty()) {
                statsPrompt.append("- No subjects added yet.\n")
            } else {
                currentSubjects.forEach {
                    val hrs = it.todayStudySeconds / 3600f
                    val targetHrs = it.targetSecondsPerDay / 3600f
                    statsPrompt.append("- ${it.name}: Today ${"%.1f".format(hrs)}h (Goal: ${"%.1f".format(targetHrs)}h). All-Time: ${"%.1f".format(it.totalStudySeconds / 3600f)}h\n")
                }
            }

            statsPrompt.append("\nMy Tasks for Today:\n")
            if (currentTasks.isEmpty()) {
                statsPrompt.append("- No tasks listed today.\n")
            } else {
                currentTasks.forEach {
                    val status = if (it.isCompleted) "Completed" else "Pending"
                    statsPrompt.append("- ${it.taskName} (Est: ${it.estimatedMinutes}m) - $status\n")
                }
            }

            statsPrompt.append("\nRecent Session Log (Last 10 study entries):\n")
            if (currentSessions.isEmpty()) {
                statsPrompt.append("- No sessions logged yet.\n")
            } else {
                currentSessions.take(10).forEach {
                    val durationMin = it.durationSeconds / 60
                    statsPrompt.append("- Date ${it.dateString}: Subject ID ${it.subjectId} studied for $durationMin mins.\n")
                }
            }

            statsPrompt.append("\nBased on this student profile, write a friendly, supportive study insight report (approx 200-250 words) with:\n")
            statsPrompt.append("1. Performance summary (highlighting positive progress or gently advising on focus gaps).\n")
            statsPrompt.append("2. 3 concrete scientific tips (e.g. Spaced Repetition, Pomodoro adjustments, active recall recommendations tailored to their subjects).\n")
            statsPrompt.append("3. A powerful motivational quote to ignite their focus. Keep formatting extremely elegant, utilizing bullet points and clean markdown. No developer jargon.")

            val result = GeminiHelper.generateStudyInsight(statsPrompt.toString())
            _aiStudyInsight.value = result
            _isGeneratingInsight.value = false
        }
    }

    fun generateAIExamPrepPlan(examTopic: String, daysRemaining: Int, dailyStudyHours: Int) {
        viewModelScope.launch {
            _isGeneratingPlan.value = true
            
            val plannerPrompt = """
                You are a world-class academic tutor and study planner. 
                I need an ultimate, high-efficiency, day-by-day exam preparation plan.
                
                My Exam Topic: $examTopic
                Days Remaining: $daysRemaining days
                Hours I Can Study Daily: $dailyStudyHours hours
                
                Please generate a custom study guide incorporating:
                1. A comprehensive day-by-day breakdown of topics and recommended activities (e.g. active recall, practice exams, mind mapping).
                2. Spacing effect & retrieval practice guidelines specific to "$examTopic".
                3. A daily time block outline (e.g., Morning Block, Afternoon Block) showing how to distribute the $dailyStudyHours hours effectively.
                
                Structure the guide clearly using bold headers, bullet lists, and easy-to-read formatting. Keep the tone highly motivational and encouraging!
            """.trimIndent()

            val result = GeminiHelper.generateStudyInsight(plannerPrompt)
            _aiExamPlan.value = result
            _isGeneratingPlan.value = false
        }
    }

    // --- AUTHENTICATION & GOALS ---
    fun login(usernameInput: String, passwordInput: String): Boolean {
        var success = false
        runBlocking {
            val user = repository.getUserByUsername(usernameInput.trim().lowercase())
            if (user != null && user.passwordHash == passwordInput) {
                _loggedInUser.value = user
                success = true
                
                // Load nicknames for group
                val cachedNicks = mutableMapOf<String, String>()
                joinedGroups.value.forEach { group ->
                    repository.getGroupNickname(group.id, user.username)?.let { record ->
                        cachedNicks[group.id] = record.nickname
                    }
                }
                _groupNicknames.value = cachedNicks
            }
        }
        return success
    }

    fun register(
        usernameInput: String,
        passwordInput: String,
        nicknameInput: String,
        examDateInput: String,
        desiredGradeInput: String,
        focusSubjectInput: String,
        weeklyHoursInput: Int
    ): String? {
        val cleanUser = usernameInput.trim().lowercase()
        if (cleanUser.length < 3) return "Username must be at least 3 characters."
        if (passwordInput.length < 4) return "Password must be at least 4 characters."
        if (nicknameInput.trim().isEmpty()) return "Display Nickname cannot be empty."

        var errorMsg: String? = null
        runBlocking {
            val existing = repository.getUserByUsername(cleanUser)
            if (existing != null) {
                errorMsg = "Username already exists."
            } else {
                val newUser = AppUser(
                    username = cleanUser,
                    passwordHash = passwordInput,
                    nickname = nicknameInput.trim(),
                    examDate = examDateInput.trim().ifEmpty { "2026-08-01" },
                    desiredGrade = desiredGradeInput.trim().ifEmpty { "A" },
                    focusSubject = focusSubjectInput.trim().ifEmpty { "Coding" },
                    weeklyHoursTarget = if (weeklyHoursInput > 0) weeklyHoursInput else 15
                )
                repository.insertUser(newUser)
                _loggedInUser.value = newUser
            }
        }
        return errorMsg
    }

    fun logout() {
        _loggedInUser.value = null
    }

    fun updateAcademicGoals(
        examDateInput: String,
        desiredGradeInput: String,
        focusSubjectInput: String,
        weeklyHoursInput: Int
    ) {
        val currentUser = _loggedInUser.value ?: return
        viewModelScope.launch {
            val updated = currentUser.copy(
                examDate = examDateInput,
                desiredGrade = desiredGradeInput,
                focusSubject = focusSubjectInput,
                weeklyHoursTarget = weeklyHoursInput
            )
            repository.updateUser(updated)
            _loggedInUser.value = updated
        }
    }

    // --- COOPERATIVE SOCIAL & MESSAGE BOARD FORUM ---
    fun setActiveGroupId(groupId: String) {
        _activeGroupId.value = groupId
    }

    fun postGroupMessage(groupId: String, messageText: String, category: String = "General") {
        val user = _loggedInUser.value ?: return
        val senderNickname = getGroupNicknameForDisplay(groupId)
        viewModelScope.launch {
            repository.insertGroupMessage(
                GroupMessage(
                    groupId = groupId,
                    senderName = senderNickname,
                    messageText = messageText,
                    category = category
                )
            )
        }
    }

    fun setGroupNickname(groupId: String, customNickname: String) {
        val user = _loggedInUser.value ?: return
        val trimmed = customNickname.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val record = GroupNickname(groupId = groupId, username = user.username, nickname = trimmed)
            repository.insertGroupNickname(record)
            _groupNicknames.update { it + (groupId to trimmed) }
        }
    }

    fun getGroupNicknameForDisplay(groupId: String): String {
        val user = _loggedInUser.value ?: return "Guest"
        return _groupNicknames.value[groupId] ?: user.nickname
    }

    // --- AI STUDY ASSISTANT (QUIZ, EXPLAINER, TIPS) ---
    fun generateAIQuiz(topic: String) {
        val cleanTopic = topic.trim()
        if (cleanTopic.isEmpty()) return

        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _generatedQuiz.value = null

            val apiKey = BuildConfig.GEMINI_API_KEY
            val hasApiKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

            if (hasApiKey) {
                val prompt = """
                    You are a world-class educational examiner. Create a comprehensive multiple-choice practice quiz about "$cleanTopic".
                    Return a JSON object containing a study quiz.
                    Structure:
                    {
                      "topic": "$cleanTopic",
                      "questions": [
                        {
                          "question": "Clear, precise question here?",
                          "options": ["Option A", "Option B", "Option C", "Option D"],
                          "correctAnswerIndex": 0,
                          "explanation": "Brief explanation of why this answer is correct."
                        }
                      ]
                    }
                    Generate exactly 4 high-quality questions. Ensure correct JSON format, no enclosing markdown, just return raw JSON string.
                """.trimIndent()

                try {
                    val rawResult = GeminiHelper.generateStudyInsight(prompt)
                    val cleanJson = rawResult.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val moshi = com.squareup.moshi.Moshi.Builder()
                        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val adapter = moshi.adapter(StudyQuiz::class.java)
                    val parsed = adapter.fromJson(cleanJson)
                    if (parsed != null && parsed.questions.isNotEmpty()) {
                        _generatedQuiz.value = parsed
                    } else {
                        _generatedQuiz.value = generateOfflineQuiz(cleanTopic)
                    }
                } catch (e: Exception) {
                    _generatedQuiz.value = generateOfflineQuiz(cleanTopic)
                }
            } else {
                delay(1200)
                _generatedQuiz.value = generateOfflineQuiz(cleanTopic)
            }
            _isGeneratingQuiz.value = false
        }
    }

    private fun generateOfflineQuiz(topic: String): StudyQuiz {
        val lower = topic.lowercase()
        val questions = when {
            lower.contains("math") || lower.contains("calculus") || lower.contains("algebra") -> listOf(
                QuizQuestion(
                    question = "What is the derivative of f(x) = 3x² + 5x?",
                    options = listOf("f'(x) = 6x", "f'(x) = 6x + 5", "f'(x) = 3x + 5", "f'(x) = x³ + 5"),
                    correctAnswerIndex = 1,
                    explanation = "Using the power rule: d/dx(x^n) = n*x^(n-1), the derivative of 3x² is 6x and derivative of 5x is 5."
                ),
                QuizQuestion(
                    question = "Which of the following is the identity matrix of size 2?",
                    options = listOf("[[0, 1], [1, 0]]", "[[1, 1], [1, 1]]", "[[1, 0], [0, 1]]", "[[0, 0], [0, 0]]"),
                    correctAnswerIndex = 2,
                    explanation = "The identity matrix contains 1s on its main diagonal and 0s elsewhere."
                ),
                QuizQuestion(
                    question = "Solve for x: log₂ (x) = 5.",
                    options = listOf("10", "25", "32", "50"),
                    correctAnswerIndex = 2,
                    explanation = "By log definitions, log_b(y) = x is equivalent to b^x = y. Thus, 2^5 = 32."
                ),
                QuizQuestion(
                    question = "What is a prime number?",
                    options = listOf("A number divisible by only 1 and itself", "Any odd number", "A number divisible by 2", "A number with negative coefficients"),
                    correctAnswerIndex = 0,
                    explanation = "Primes are natural numbers strictly greater than 1 that have no positive divisors other than 1 and themselves."
                )
            )
            lower.contains("code") || lower.contains("ai") || lower.contains("python") || lower.contains("program") -> listOf(
                QuizQuestion(
                    question = "What is the average time complexity of a search operation in a well-balanced Binary Search Tree (BST)?",
                    options = listOf("O(1)", "O(log n)", "O(n)", "O(n log n)"),
                    correctAnswerIndex = 1,
                    explanation = "In a balanced BST, the height is log n, so searching scales logarithmically."
                ),
                QuizQuestion(
                    question = "Which of the following describes the 'overfitting' phenomenon in Machine Learning?",
                    options = listOf("Model is too simple and fails to capture training patterns", "Model captures random noise in training data, failing to generalize to new data", "Model runs too fast", "Model is perfect and has 100% accuracy on all datasets"),
                    correctAnswerIndex = 1,
                    explanation = "Overfitting occurs when a model fits training data too closely, capturing noise instead of general patterns."
                ),
                QuizQuestion(
                    question = "In Python, which keyword is used to construct a generator function?",
                    options = listOf("return", "emit", "yield", "gen"),
                    correctAnswerIndex = 2,
                    explanation = "The 'yield' statement suspends function's execution and sends a value back to the caller, acting as a generator."
                ),
                QuizQuestion(
                    question = "What does MVC stand for in software architecture?",
                    options = listOf("Modular Velocity Controller", "Model View Controller", "Main Virtual Canvas", "Manage View Containers"),
                    correctAnswerIndex = 1,
                    explanation = "Model-View-Controller separates an application into three main components: data (Model), presentation (View), and logic (Controller)."
                )
            )
            lower.contains("chem") || lower.contains("physics") || lower.contains("atom") -> listOf(
                QuizQuestion(
                    question = "What is the first law of thermodynamics?",
                    options = listOf("Entropy always increases", "Energy cannot be created or destroyed, only transformed", "Absolute zero is unreachable", "Force equals mass times acceleration"),
                    correctAnswerIndex = 1,
                    explanation = "The first law of thermodynamics states that energy is conserved in all processes."
                ),
                QuizQuestion(
                    question = "Which subatomic particle carries a negative electric charge?",
                    options = listOf("Proton", "Neutron", "Electron", "Quark"),
                    correctAnswerIndex = 2,
                    explanation = "Electrons carry a single unit of negative electric charge."
                ),
                QuizQuestion(
                    question = "What is the chemical formula for table salt?",
                    options = listOf("HCl", "NaOH", "NaCl", "NaHCO3"),
                    correctAnswerIndex = 2,
                    explanation = "NaCl stands for Sodium Chloride, commonly known as table salt."
                ),
                QuizQuestion(
                    question = "What force holds planets in their orbits around the Sun?",
                    options = listOf("Electromagnetic force", "Frictional force", "Gravitational force", "Centrifugal force"),
                    correctAnswerIndex = 2,
                    explanation = "Gravity is the mutually attractive force pulling celestial bodies toward larger orbital centers."
                )
            )
            lower.contains("bio") || lower.contains("nature") || lower.contains("cell") -> listOf(
                QuizQuestion(
                    question = "Which organelle is widely known as the 'powerhouse' of the eukaryotic cell?",
                    options = listOf("Nucleus", "Ribosome", "Mitochondrion", "Lysosome"),
                    correctAnswerIndex = 2,
                    explanation = "Mitochondria produce ATP, the cellular energy currency, through aerobic cellular respiration."
                ),
                QuizQuestion(
                    question = "What gas do plants absorb from the atmosphere to perform photosynthesis?",
                    options = listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"),
                    correctAnswerIndex = 2,
                    explanation = "Plants take in Carbon Dioxide (CO2) and release Oxygen (O2) during light reactions."
                ),
                QuizQuestion(
                    question = "Which biological macromolecule stores genetic information?",
                    options = listOf("Lipids", "Carbohydrates", "DNA", "Proteins"),
                    correctAnswerIndex = 2,
                    explanation = "Deoxyribonucleic acid (DNA) stores and transmits hereditary coding instructions."
                ),
                QuizQuestion(
                    question = "What is the primary function of white blood cells?",
                    options = listOf("Carry oxygen", "Clot blood", "Defend body against infections and pathogens", "Digest nutrients"),
                    correctAnswerIndex = 2,
                    explanation = "White blood cells (leukocytes) are key players in the immune system defending against disease."
                )
            )
            else -> listOf(
                QuizQuestion(
                    question = "Which cognitive technique involves reviewing material at gradually increasing intervals?",
                    options = listOf("Cramming", "Spaced Repetition", "Passive Highlighting", "Rereading notes"),
                    correctAnswerIndex = 1,
                    explanation = "Spaced repetition combats the forgetting curve by prompting recall precisely when memory begins to decay."
                ),
                QuizQuestion(
                    question = "According to the Feynman Technique, how should you test your understanding of a complex topic?",
                    options = listOf("Explain it simply, as if teaching a 10-year-old child", "Memorize it verbatim from Wikipedia", "Write an advanced scientific paper", "Highlight every sentence in the textbook"),
                    correctAnswerIndex = 0,
                    explanation = "The Feynman Technique highlights focus gaps by forcing you to simplify concepts and avoid complex jargon."
                ),
                QuizQuestion(
                    question = "What is the core duration of a standard Pomodoro work segment?",
                    options = listOf("10 minutes", "25 minutes", "1 hour", "4 hours"),
                    correctAnswerIndex = 1,
                    explanation = "A standard Pomodoro interval consists of 25 minutes of high-focus work followed by a 5-minute break."
                ),
                QuizQuestion(
                    question = "What is 'active recall'?",
                    options = listOf("Reading a book over and over", "Retrieving information from memory to test yourself", "Listening to relaxing background music", "Sleeping with your textbook under your pillow"),
                    correctAnswerIndex = 1,
                    explanation = "Active recall forces your brain to retrieve information, building stronger neural pathways than passive review."
                )
            )
        }
        return StudyQuiz(topic = topic, questions = questions)
    }

    fun explainConcept(concept: String) {
        val cleanConcept = concept.trim()
        if (cleanConcept.isEmpty()) return

        viewModelScope.launch {
            _isExplainingConcept.value = true
            _conceptExplanation.value = ""

            val prompt = """
                You are a world-class academic explainer. Please provide an elegant, clear, and highly intuitive breakdown of the concept: "$cleanConcept".
                Format the explanation exactly with:
                1. **Definition**: A simple, concise explanation.
                2. **Simple Analogy**: Connect it to a familiar everyday object or scenario to make it click.
                3. **Key Takeaways**: 3 bulleted key points.
                4. **Real-world Example**: A concrete application.
                
                Keep the formatting stunning, utilizing bolding, clean headers, and spacious padding. Speak warmly and clear of heavy academic jargon.
            """.trimIndent()

            val result = GeminiHelper.generateStudyInsight(prompt)
            _conceptExplanation.value = result
            _isExplainingConcept.value = false
        }
    }

    fun generateAIPerformanceTips() {
        val user = _loggedInUser.value ?: return
        viewModelScope.launch {
            _isGeneratingTips.value = true
            _aiTips.value = ""

            val prompt = """
                You are a study psychologist and peak performance coach.
                Give me 4 personalized, scientifically-proven study tips to maximize my academic success.
                My current profile:
                - Goal exam date: ${user.examDate}
                - Target Desired Grade: ${user.desiredGrade}
                - Core Subject Focus: ${user.focusSubject}
                - Target Weekly Study Commitment: ${user.weeklyHoursTarget} hours
                
                Write a highly encouraging, actionable, and personalized set of tips in clear markdown with beautiful emoji icons. Keep it concise, around 180 words.
            """.trimIndent()

            val result = GeminiHelper.generateStudyInsight(prompt)
            _aiTips.value = result
            _isGeneratingTips.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        FocusSoundSynthesizer.stop()
    }
}

// --- Companion Support Simulation Classes ---

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class StudyQuiz(
    val topic: String,
    val questions: List<QuizQuestion>
)

data class GroupMemberSimulation(
    val name: String,
    val isStudying: Boolean,
    val activeSubject: String,
    val todaySeconds: Long,
    val currentSessionSeconds: Long
)

data class FloatingEmoji(
    val emoji: String,
    val offsetX: Float,
    val offsetY: Float,
    val expiryTime: Long
)

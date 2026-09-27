package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.audio.AmbientSoundType
import com.example.audio.FocusAudioSynthesizer
import com.example.data.FocusSessionEntity
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerMode(val displayName: String) {
    POMODORO("Pomodoro"),
    CUSTOM("Custom Timer"),
    STOPWATCH("Open Study")
}

enum class SessionPhase(val displayName: String) {
    WORK("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class FocusUiState(
    val mode: TimerMode = TimerMode.POMODORO,
    val phase: SessionPhase = SessionPhase.WORK,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val pomodoroWorkDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val completedPomodorosCount: Int = 0,
    val selectedTask: TaskEntity? = null,
    val ambientSound: AmbientSoundType = AmbientSoundType.NONE,
    val showCompletionDialog: Boolean = false,
    val lastCompletedSeconds: Int = 0
)

class FocusViewModel(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val synthesizer = FocusAudioSynthesizer()
    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    val pendingTasks: StateFlow<List<TaskEntity>> = repository.pendingTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: Job? = null

    fun selectTask(task: TaskEntity?) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_uiState.value.status == TimerStatus.RUNNING) {
            pauseTimer()
        }
        val targetSeconds = when (mode) {
            TimerMode.POMODORO -> _uiState.value.pomodoroWorkDurationMinutes * 60
            TimerMode.CUSTOM -> 30 * 60
            TimerMode.STOPWATCH -> 0
        }
        _uiState.update {
            it.copy(
                mode = mode,
                phase = SessionPhase.WORK,
                status = TimerStatus.IDLE,
                totalSeconds = targetSeconds,
                remainingSeconds = targetSeconds,
                elapsedSeconds = 0
            )
        }
    }

    fun setCustomDurationMinutes(minutes: Int) {
        if (_uiState.value.status != TimerStatus.RUNNING) {
            val seconds = minutes * 60
            _uiState.update {
                it.copy(
                    totalSeconds = seconds,
                    remainingSeconds = seconds,
                    elapsedSeconds = 0
                )
            }
        }
    }

    fun setPomodoroDurations(workMin: Int, shortBreakMin: Int, longBreakMin: Int) {
        _uiState.update {
            it.copy(
                pomodoroWorkDurationMinutes = workMin,
                shortBreakDurationMinutes = shortBreakMin,
                longBreakDurationMinutes = longBreakMin
            )
        }
        if (_uiState.value.mode == TimerMode.POMODORO && _uiState.value.status == TimerStatus.IDLE) {
            val seconds = workMin * 60
            _uiState.update { it.copy(totalSeconds = seconds, remainingSeconds = seconds) }
        }
    }

    fun startTimer() {
        if (_uiState.value.status == TimerStatus.RUNNING) return

        _uiState.update { it.copy(status = TimerStatus.RUNNING) }

        if (_uiState.value.ambientSound != AmbientSoundType.NONE) {
            synthesizer.startAmbient(_uiState.value.ambientSound)
        }

        if (_uiState.value.mode != TimerMode.STOPWATCH && _uiState.value.remainingSeconds > 0) {
            alarmScheduler.scheduleFocusTimerNotification(
                _uiState.value.remainingSeconds,
                _uiState.value.selectedTask?.title ?: _uiState.value.phase.displayName
            )
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.status == TimerStatus.RUNNING) {
                delay(1000L)
                if (_uiState.value.mode == TimerMode.STOPWATCH) {
                    _uiState.update {
                        it.copy(
                            elapsedSeconds = it.elapsedSeconds + 1,
                            remainingSeconds = it.remainingSeconds + 1
                        )
                    }
                } else {
                    if (_uiState.value.remainingSeconds > 1) {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = it.remainingSeconds - 1,
                                elapsedSeconds = it.elapsedSeconds + 1
                            )
                        }
                    } else {
                        // Timer completed!
                        onTimerFinished()
                        break
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(status = TimerStatus.PAUSED) }
        timerJob?.cancel()
        alarmScheduler.cancelFocusTimerNotification()
        synthesizer.stopAmbient()
    }

    fun resetTimer() {
        pauseTimer()
        val defaultSecs = when (_uiState.value.mode) {
            TimerMode.POMODORO -> {
                when (_uiState.value.phase) {
                    SessionPhase.WORK -> _uiState.value.pomodoroWorkDurationMinutes * 60
                    SessionPhase.SHORT_BREAK -> _uiState.value.shortBreakDurationMinutes * 60
                    SessionPhase.LONG_BREAK -> _uiState.value.longBreakDurationMinutes * 60
                }
            }
            TimerMode.CUSTOM -> _uiState.value.totalSeconds
            TimerMode.STOPWATCH -> 0
        }
        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                remainingSeconds = defaultSecs,
                elapsedSeconds = 0
            )
        }
    }

    fun addMinutes(minutes: Int) {
        val extraSecs = minutes * 60
        _uiState.update {
            it.copy(
                totalSeconds = it.totalSeconds + extraSecs,
                remainingSeconds = it.remainingSeconds + extraSecs
            )
        }
        if (_uiState.value.status == TimerStatus.RUNNING) {
            alarmScheduler.scheduleFocusTimerNotification(
                _uiState.value.remainingSeconds,
                _uiState.value.selectedTask?.title ?: _uiState.value.phase.displayName
            )
        }
    }

    fun setAmbientSound(sound: AmbientSoundType) {
        _uiState.update { it.copy(ambientSound = sound) }
        if (_uiState.value.status == TimerStatus.RUNNING) {
            synthesizer.startAmbient(sound)
        }
    }

    fun stopAndFinishEarly() {
        pauseTimer()
        val elapsed = _uiState.value.elapsedSeconds
        if (elapsed >= 30) { // Require at least 30 seconds to record a session
            _uiState.update {
                it.copy(
                    showCompletionDialog = true,
                    lastCompletedSeconds = elapsed
                )
            }
        } else {
            resetTimer()
        }
    }

    private fun onTimerFinished() {
        _uiState.update { it.copy(status = TimerStatus.COMPLETED) }
        alarmScheduler.cancelFocusTimerNotification()
        synthesizer.stopAmbient()
        synthesizer.playCompletionChime()

        val isWork = _uiState.value.phase == SessionPhase.WORK
        val elapsed = _uiState.value.totalSeconds

        if (isWork) {
            val newCompletedCount = _uiState.value.completedPomodorosCount + 1
            val nextPhase = if (_uiState.value.mode == TimerMode.POMODORO) {
                if (newCompletedCount % 4 == 0) SessionPhase.LONG_BREAK else SessionPhase.SHORT_BREAK
            } else {
                SessionPhase.WORK
            }

            _uiState.update {
                it.copy(
                    showCompletionDialog = true,
                    lastCompletedSeconds = elapsed,
                    completedPomodorosCount = newCompletedCount
                )
            }
        } else {
            // Break finished! Return to work
            val nextSeconds = _uiState.value.pomodoroWorkDurationMinutes * 60
            _uiState.update {
                it.copy(
                    phase = SessionPhase.WORK,
                    status = TimerStatus.IDLE,
                    totalSeconds = nextSeconds,
                    remainingSeconds = nextSeconds,
                    elapsedSeconds = 0
                )
            }
        }
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showCompletionDialog = false) }
        prepareNextPhase()
    }

    fun saveCompletedSession(rating: Int, notes: String, markTaskDone: Boolean) {
        val currentState = _uiState.value
        val actualSecs = currentState.lastCompletedSeconds
        val category = currentState.selectedTask?.category ?: "Study"
        val taskTitle = currentState.selectedTask?.title
        val taskId = currentState.selectedTask?.id

        viewModelScope.launch {
            val session = FocusSessionEntity(
                taskId = taskId,
                taskTitle = taskTitle,
                category = category,
                durationSeconds = currentState.totalSeconds,
                actualSeconds = actualSecs,
                sessionType = currentState.mode.name,
                isCompleted = true,
                rating = rating,
                notes = notes,
                timestampMillis = System.currentTimeMillis()
            )
            repository.recordFocusSession(session)

            if (markTaskDone && currentState.selectedTask != null) {
                repository.toggleTaskCompleted(currentState.selectedTask)
            }
        }

        _uiState.update { it.copy(showCompletionDialog = false) }
        prepareNextPhase()
    }

    private fun prepareNextPhase() {
        if (_uiState.value.mode == TimerMode.POMODORO) {
            val isNextLongBreak = _uiState.value.completedPomodorosCount > 0 &&
                    _uiState.value.completedPomodorosCount % 4 == 0
            val nextPhase = if (isNextLongBreak) SessionPhase.LONG_BREAK else SessionPhase.SHORT_BREAK
            val nextSeconds = if (isNextLongBreak) {
                _uiState.value.longBreakDurationMinutes * 60
            } else {
                _uiState.value.shortBreakDurationMinutes * 60
            }

            _uiState.update {
                it.copy(
                    phase = nextPhase,
                    status = TimerStatus.IDLE,
                    totalSeconds = nextSeconds,
                    remainingSeconds = nextSeconds,
                    elapsedSeconds = 0
                )
            }
        } else {
            resetTimer()
        }
    }

    fun skipBreak() {
        val nextSeconds = _uiState.value.pomodoroWorkDurationMinutes * 60
        _uiState.update {
            it.copy(
                phase = SessionPhase.WORK,
                status = TimerStatus.IDLE,
                totalSeconds = nextSeconds,
                remainingSeconds = nextSeconds,
                elapsedSeconds = 0
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        synthesizer.stopAmbient()
    }

    class Factory(
        private val repository: TaskRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(repository, alarmScheduler) as T
        }
    }
}

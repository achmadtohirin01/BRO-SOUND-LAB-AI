package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioEngine
import com.example.data.api.AiLyricsSheet
import com.example.data.api.AiMasteringPreset
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.database.AudioProjectEntity
import com.example.data.database.PresetEntity
import com.example.data.database.SoundLabDatabase
import com.example.data.database.SoundLabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StartupStage {
    SPLASH,
    INITIALIZING,
    PERMISSION_CHECK,
    ONBOARDING,
    MAIN_DASHBOARD
}

sealed interface SoundLabUiState {
    object Idle : SoundLabUiState
    object Loading : SoundLabUiState
    data class Success(val message: String) : SoundLabUiState
    data class Error(val throwable: Throwable) : SoundLabUiState
}

class SoundLabViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SoundLabDatabase.getDatabase(application)
    private val repository = SoundLabRepository(database.soundLabDao())

    // Live Audio Engine
    val audioEngine = AudioEngine()

    // Navigation & Workspace Architecture States
    private val _startupStage = MutableStateFlow(StartupStage.SPLASH)
    val startupStage: StateFlow<StartupStage> = _startupStage.asStateFlow()

    private val _currentTab = MutableStateFlow("HOME")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _isCreateModalOpen = MutableStateFlow(false)
    val isCreateModalOpen: StateFlow<Boolean> = _isCreateModalOpen.asStateFlow()

    private val _isSideDrawerOpen = MutableStateFlow(false)
    val isSideDrawerOpen: StateFlow<Boolean> = _isSideDrawerOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProjectCategory = MutableStateFlow("Recent")
    val selectedProjectCategory: StateFlow<String> = _selectedProjectCategory.asStateFlow()

    private val _isProjectGridView = MutableStateFlow(false)
    val isProjectGridView: StateFlow<Boolean> = _isProjectGridView.asStateFlow()

    private val _selectedToolId = MutableStateFlow<String?>(null)
    val selectedToolId: StateFlow<String?> = _selectedToolId.asStateFlow()

    // Persistent Mini Player states
    private val _miniPlayerPlaying = MutableStateFlow(false)
    val miniPlayerPlaying: StateFlow<Boolean> = _miniPlayerPlaying.asStateFlow()

    private val _miniPlayerPosition = MutableStateFlow(0.24f)
    val miniPlayerPosition: StateFlow<Float> = _miniPlayerPosition.asStateFlow()

    private val _miniPlayerVolume = MutableStateFlow(0.8f)
    val miniPlayerVolume: StateFlow<Float> = _miniPlayerVolume.asStateFlow()

    private val _miniPlayerExpanded = MutableStateFlow(false)
    val miniPlayerExpanded: StateFlow<Boolean> = _miniPlayerExpanded.asStateFlow()

    // Persistent Recording states
    private val _isRecordingActive = MutableStateFlow(false)
    val isRecordingActive: StateFlow<Boolean> = _isRecordingActive.asStateFlow()

    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _recordingInputDevice = MutableStateFlow("Internal Array Mic (96kHz Ultra)")
    val recordingInputDevice: StateFlow<String> = _recordingInputDevice.asStateFlow()

    private val _notifications = MutableStateFlow(listOf("DAW Master online", "Ready for Stereo Mix", "Cloud preset sync active"))
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    // UI Interactive States
    val allPresets: StateFlow<List<PresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<AudioProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<SoundLabUiState>(SoundLabUiState.Idle)
    val uiState: StateFlow<SoundLabUiState> = _uiState.asStateFlow()

    // Active screen coordinates
    private val _selectedProject = MutableStateFlow<AudioProjectEntity?>(null)
    val selectedProject: StateFlow<AudioProjectEntity?> = _selectedProject.asStateFlow()

    // Real-Time Slider values matching AudioEngine (Maintains visual feedback)
    val eqBandsState = MutableStateFlow(FloatArray(10) { 0.0f })
    val driveState = MutableStateFlow(0.0f)
    val delayFeedbackState = MutableStateFlow(0.2f)
    val reverbDecayState = MutableStateFlow(0.3f)
    val widenessState = MutableStateFlow(0.5f)
    val compressThresholdState = MutableStateFlow(-20f)
    val vocalsGainState = MutableStateFlow(1.0f)
    val beatsGainState = MutableStateFlow(1.0f)

    // Synth Playback flag
    private val _isSynthPlaying = MutableStateFlow(false)
    val isSynthPlaying: StateFlow<Boolean> = _isSynthPlaying.asStateFlow()

    // Keyboard frequency list (C4 to B4)
    val keyFrequencies = mapOf(
        "C" to 261.63f,
        "D" to 293.66f,
        "E" to 329.63f,
        "F" to 349.23f,
        "G" to 392.00f,
        "A" to 440.00f,
        "B" to 493.88f,
        "C5" to 523.25f,
        "D5" to 587.33f,
        "E5" to 659.25f
    )

    init {
        // Prepare pre-baked engineering master presets on fast-launch if empty
        viewModelScope.launch {
            repository.allPresets.collect { list ->
                if (list.isEmpty()) {
                    createPrebakedPresets()
                }
            }
        }

        // Auto transition splash screen -> initialize stages
        viewModelScope.launch {
            delay(1500)
            if (_startupStage.value == StartupStage.SPLASH) {
                _startupStage.value = StartupStage.INITIALIZING
                delay(1200)
                _startupStage.value = StartupStage.PERMISSION_CHECK
            }
        }

        // Active Player timer feedback
        viewModelScope.launch {
            while (true) {
                delay(250)
                if (_miniPlayerPlaying.value) {
                    val nextPos = _miniPlayerPosition.value + 0.004f
                    _miniPlayerPosition.value = if (nextPos > 1.0f) 0f else nextPos
                }
            }
        }

        // Active Recording timer feedback
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isRecordingActive.value && !_isRecordingPaused.value) {
                    _recordingSeconds.value = _recordingSeconds.value + 1
                }
            }
        }
    }

    fun setStartupStage(stage: StartupStage) {
        _startupStage.value = stage
    }

    fun setCurrentTab(tab: String) {
        _currentTab.value = tab
    }

    fun setCreateModalOpen(open: Boolean) {
        _isCreateModalOpen.value = open
    }

    fun setSideDrawerOpen(open: Boolean) {
        _isSideDrawerOpen.value = open
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProjectCategory(category: String) {
        _selectedProjectCategory.value = category
    }

    fun toggleProjectGridView() {
        _isProjectGridView.value = !_isProjectGridView.value
    }

    fun setSelectedToolId(toolId: String?) {
        _selectedToolId.value = toolId
    }

    fun toggleMiniPlayerPlaying() {
        _miniPlayerPlaying.value = !_miniPlayerPlaying.value
    }

    fun setMiniPlayerPosition(position: Float) {
        _miniPlayerPosition.value = position
    }

    fun setMiniPlayerVolume(volume: Float) {
        _miniPlayerVolume.value = volume
    }

    fun setMiniPlayerExpanded(expanded: Boolean) {
        _miniPlayerExpanded.value = expanded
    }

    fun startRecording() {
        _isRecordingActive.value = true
        _isRecordingPaused.value = false
        _recordingSeconds.value = 0
        addNotification("Recording started: ${_recordingInputDevice.value}")
    }

    fun pauseToggleRecording() {
        if (_isRecordingActive.value) {
            _isRecordingPaused.value = !_isRecordingPaused.value
            if (_isRecordingPaused.value) {
                addNotification("Recording paused")
            } else {
                addNotification("Recording resumed")
            }
        }
    }

    fun stopAndSaveRecording(title: String) {
        if (_isRecordingActive.value) {
            val seconds = _recordingSeconds.value
            _isRecordingActive.value = false
            _isRecordingPaused.value = false
            _recordingSeconds.value = 0
            
            // Save recording project
            viewModelScope.launch {
                val defaultProject = AudioProjectEntity(
                    title = if (title.isBlank()) "Recording Clip #${System.currentTimeMillis() % 1000}" else title,
                    bpm = 110,
                    keySignature = "A Minor",
                    lyrics = "[Recording Session]\nDuration: ${seconds / 60}:${String.format("%02d", seconds % 60)}\nInput: ${_recordingInputDevice.value}",
                    isRecordingSample = true
                )
                val id = repository.insertProject(defaultProject)
                val created = repository.getProjectById(id.toInt())
                if (created != null) {
                    _selectedProject.value = created
                }
                addNotification("Recording saved as '${defaultProject.title}'")
            }
        }
    }

    fun discardRecording() {
        _isRecordingActive.value = false
        _isRecordingPaused.value = false
        _recordingSeconds.value = 0
        addNotification("Recording discarded")
    }

    fun addNotification(message: String) {
        val current = _notifications.value.toMutableList()
        current.add(0, message)
        if (current.size > 20) {
            current.removeAt(current.lastIndex)
        }
        _notifications.value = current
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    private suspend fun createPrebakedPresets() {
        val classicMastering = PresetEntity(
            name = "Warm Studio Master",
            bands = "2.5,1.5,0.0,-1.0,-1.5,-1.0,1.2,2.0,1.5,2.0",
            compressorThreshold = -18f,
            reverbDecay = 0.25f,
            delayFeedback = 0.15f,
            drive = 0.1f,
            wideness = 0.6f,
            vocalsGain = 1.0f,
            isUserCreated = false
        )
        val airVocal = PresetEntity(
            name = "Air Crisp Vocal",
            bands = "-4.0,-2.0,0.0,0.5,1.5,2.0,3.5,4.0,2.5,3.0",
            compressorThreshold = -24f,
            reverbDecay = 0.45f,
            delayFeedback = 0.35f,
            drive = 0.05f,
            wideness = 0.8f,
            vocalsGain = 1.4f,
            isUserCreated = false
        )
        val heavyBase = PresetEntity(
            name = "Cyberpunk Club Boost",
            bands = "6.5,5.0,2.5,0.0,-2.0,-2.5,-1.0,0.5,1.5,2.0",
            compressorThreshold = -12f,
            reverbDecay = 0.15f,
            delayFeedback = 0.4f,
            drive = 0.5f,
            wideness = 0.7f,
            vocalsGain = 0.8f,
            isUserCreated = false
        )

        repository.insertPreset(classicMastering)
        repository.insertPreset(airVocal)
        repository.insertPreset(heavyBase)
    }

    // --- ENGINE CONTROL ACTIONS ---

    fun toggleSynthesizer() {
        if (_isSynthPlaying.value) {
            audioEngine.stop()
            _isSynthPlaying.value = false
        } else {
            audioEngine.start()
            _isSynthPlaying.value = true
        }
    }

    fun playNoteOnPress(freq: Float) {
        if (!_isSynthPlaying.value) {
            toggleSynthesizer()
        }
        audioEngine.playTone(freq)
    }

    fun stopNoteOnRelease() {
        audioEngine.stopTone()
    }

    // Synchronize individual band adjustments
    fun setEqBandLevel(bandIndex: Int, value: Float) {
        val bands = eqBandsState.value.clone()
        bands[bandIndex] = value
        eqBandsState.value = bands

        audioEngine.eqBands[bandIndex] = value
        audioEngine.recalculateFilterBands()
    }

    fun setDriveLevel(value: Float) {
        driveState.value = value
        audioEngine.drive = value
    }

    fun setDelayFeedbackLevel(value: Float) {
        delayFeedbackState.value = value
        audioEngine.delayFeedback = value
    }

    fun setReverbDecayLevel(value: Float) {
        reverbDecayState.value = value
        audioEngine.reverbDecay = value
    }

    fun setWidenessLevel(value: Float) {
        widenessState.value = value
        audioEngine.wideness = value
    }

    fun setCompressorThreshold(value: Float) {
        compressThresholdState.value = value
        audioEngine.compressThreshold = value
    }

    fun setVocalsVolume(value: Float) {
        vocalsGainState.value = value
        audioEngine.vocalsGain = value
    }

    fun setBeatsVolume(value: Float) {
        beatsGainState.value = value
        audioEngine.beatsGain = value
    }

    fun selectProject(project: AudioProjectEntity) {
        _selectedProject.value = project
        audioEngine.bpm = project.bpm
        
        // Load associated preset if exists
        viewModelScope.launch {
            if (project.selectedPresetId > 0) {
                val preset = repository.getPresetById(project.selectedPresetId)
                if (preset != null) {
                    loadPresetToDsp(preset)
                }
            }
        }
    }

    fun createNewProject(title: String) {
        viewModelScope.launch {
            val defaultProject = AudioProjectEntity(
                title = title,
                bpm = 120,
                keySignature = "C Major",
                lyrics = "[Intro]\nWelcome to BRO Sound Studio...\n\n[Chorus]\nCreate the beat and feel the sound...\nAI workspace of the future!"
            )
            val id = repository.insertProject(defaultProject)
            val created = repository.getProjectById(id.toInt())
            if (created != null) {
                _selectedProject.value = created
            }
        }
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            if (_selectedProject.value?.id == projectId) {
                _selectedProject.value = null
            }
            repository.deleteProjectById(projectId)
        }
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            val bandsString = eqBandsState.value.joinToString(",") { it.toString() }
            val newPreset = PresetEntity(
                name = name,
                bands = bandsString,
                compressorThreshold = compressThresholdState.value,
                reverbDecay = reverbDecayState.value,
                delayFeedback = delayFeedbackState.value,
                drive = driveState.value,
                wideness = widenessState.value,
                vocalsGain = vocalsGainState.value
            )
            repository.insertPreset(newPreset)
        }
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch {
            repository.deletePresetById(id)
        }
    }

    fun loadPresetToDsp(preset: PresetEntity) {
        // Update local Stateflows to re-trigger sliders inside Compose
        driveState.value = preset.drive
        delayFeedbackState.value = preset.delayFeedback
        reverbDecayState.value = preset.reverbDecay
        widenessState.value = preset.wideness
        compressThresholdState.value = preset.compressorThreshold
        vocalsGainState.value = preset.vocalsGain

        // Push directly to volatile AudioEngine
        audioEngine.drive = preset.drive
        audioEngine.delayFeedback = preset.delayFeedback
        audioEngine.reverbDecay = preset.reverbDecay
        audioEngine.wideness = preset.wideness
        audioEngine.compressThreshold = preset.compressorThreshold
        audioEngine.vocalsGain = preset.vocalsGain

        val bands = FloatArray(10) { 0f }
        val splitList = preset.bands.split(",")
        for (i in 0 until 10) {
            val dbVal = splitList.getOrNull(i)?.toFloatOrNull() ?: 0f
            bands[i] = dbVal
            audioEngine.eqBands[i] = dbVal
        }
        eqBandsState.value = bands
        audioEngine.recalculateFilterBands()
    }

    // --- GEMINI REST API ACTIONS ---

    // 1. AI Intelligent Mastering advisor
    fun runAiMasteringAssistant(userInput: String) {
        _uiState.value = SoundLabUiState.Loading

        val systemPrompt = """
            You are world-class mastering engineer at BRO SOUND LAB AI.
            Analyze the user's description (e.g., genre, sound vibe, voice characteristics) and generate optimal technical DSP rack settings.
            
            You must return ONLY a raw JSON block format conforming perfectly to this data schema:
            {
              "b_31": Float, 
              "b_62": Float,
              "b_125": Float,
              "b_250": Float,
              "b_500": Float,
              "b_1k": Float,
              "b_2k": Float,
              "b_4k": Float,
              "b_8k": Float,
              "b_16k": Float,
              "compressor_threshold": Float,
              "reverb_decay": Float,
              "delay_feedback": Float,
              "drive": Float,
              "wideness": Float,
              "comment": "Brief human engineering description of what was changed and why"
            }
            
            Constrain Float parameters strictly: 
            - Frequency bands (b_31..b_16k) must range between -12.0 and +12.0 (representing decibels dB).
            - compressor_threshold must range between -40f and 0f.
            - reverb_decay must range between 0.0f and 0.8f.
            - delay_feedback must range between 0.0f and 0.6f.
            - drive (saturation) must range between 0.0f (clean) and 1.2f (fuzzy tube overdrive).
            - wideness must range between 0.1f and 1.0f.
            - Do not include any HTML markdown text styling or surrounding comments, just valid JSON.
        """.trimIndent()

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("API keys are not configured in AI Studio Secrets.")
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = "User Description: $userInput")))),
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
                )

                val result = withContext(Dispatchers.IO) {
                    GeminiApiClient.service.generateContent(apiKey, request)
                }

                val responseText = result.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText == null) {
                    _uiState.value = SoundLabUiState.Error(Exception("Blank response from Gemini audio engine."))
                    return@launch
                }

                val parsedPreset = GeminiApiClient.parseMasteringPreset(responseText)
                if (parsedPreset != null) {
                    // Instantly apply parsed parameters onto EQ and DSP rack
                    applyAiPreset(parsedPreset)
                    _uiState.value = SoundLabUiState.Success("AI Master Loaded: ${parsedPreset.comment}")
                } else {
                    _uiState.value = SoundLabUiState.Error(Exception("Failed to clean JSON package. Try refining description keywords."))
                }

            } catch (e: Exception) {
                _uiState.value = SoundLabUiState.Error(e)
            }
        }
    }

    // 2. AI Lyrics & Vocal Vibe Generator
    fun runAiLyricsGenerator(themeDescription: String) {
        _uiState.value = SoundLabUiState.Loading

        val systemPrompt = """
            You are creative songwriting companion at BRO SOUND LAB AI.
            Create a custom, professional lyric sheet, key signature, and BPM suggestions based on the user's requested song theme.
            
            Return ONLY a raw JSON block format matching this schema:
            {
              "title": "Title of the song",
              "bpm": Int,
              "key": "Suggested Key Signature",
              "vibe_notes": "Prompt instructions on vocal pacing and dynamic transitions",
              "lyrics": "Song lyrics structured with labels like [Verse 1], [Chorus], [Bridge]"
            }
            
            Return only the JSON block without surrounding conversational fluff.
        """.trimIndent()

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw Exception("API credentials are not set on AI Studio secrets drawer.")
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = "Theme description: $themeDescription")))),
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
                )

                val result = withContext(Dispatchers.IO) {
                    GeminiApiClient.service.generateContent(apiKey, request)
                }

                val responseText = result.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (responseText == null) {
                    _uiState.value = SoundLabUiState.Error(Exception("No dynamic results returned."))
                    return@launch
                }

                val parsedLyrics = GeminiApiClient.parseLyricsSheet(responseText)
                if (parsedLyrics != null) {
                    // Update active project with lyrics details and save to local SQLite
                    updateProjectWithLyricsAndSave(parsedLyrics)
                    _uiState.value = SoundLabUiState.Success("Lyrics generated successfully for '${parsedLyrics.title}'!")
                } else {
                    _uiState.value = SoundLabUiState.Error(Exception("Could not extract sheet metrics. Try again."))
                }

            } catch (e: Exception) {
                _uiState.value = SoundLabUiState.Error(e)
            }
        }
    }

    private fun applyAiPreset(preset: AiMasteringPreset) {
        // Update live DSP state values
        driveState.value = preset.drive
        delayFeedbackState.value = preset.delay_feedback
        reverbDecayState.value = preset.reverb_decay
        widenessState.value = preset.wideness
        compressThresholdState.value = preset.compressor_threshold

        // Push to active Engine
        audioEngine.drive = preset.drive
        audioEngine.delayFeedback = preset.delay_feedback
        audioEngine.reverbDecay = preset.reverb_decay
        audioEngine.wideness = preset.wideness
        audioEngine.compressThreshold = preset.compressor_threshold

        // Load 10 EQ Bands
        val bands = floatArrayOf(
            preset.b_31, preset.b_62, preset.b_125, preset.b_250, preset.b_500,
            preset.b_1k, preset.b_2k, preset.b_4k, preset.b_8k, preset.b_16k
        )
        eqBandsState.value = bands
        for (i in 0 until 10) {
            audioEngine.eqBands[i] = bands[i]
        }
        audioEngine.recalculateFilterBands()

        // Persistent save of AI preset to the DB list
        viewModelScope.launch {
            val entity = PresetEntity(
                name = "AI: " + if (preset.comment.length > 25) preset.comment.take(22) + "..." else preset.comment,
                bands = bands.joinToString(","),
                compressorThreshold = preset.compressor_threshold,
                reverbDecay = preset.reverb_decay,
                delayFeedback = preset.delay_feedback,
                drive = preset.drive,
                wideness = preset.wideness,
                vocalsGain = 1.0f
            )
            repository.insertPreset(entity)
        }
    }

    private suspend fun updateProjectWithLyricsAndSave(lyricsSheet: AiLyricsSheet) {
        val current = _selectedProject.value
        val updated = if (current != null) {
            current.copy(
                title = lyricsSheet.title,
                bpm = lyricsSheet.bpm,
                keySignature = lyricsSheet.key,
                lyrics = lyricsSheet.lyrics,
                notes = lyricsSheet.vibe_notes,
                lastSaved = System.currentTimeMillis()
            )
        } else {
            AudioProjectEntity(
                title = lyricsSheet.title,
                bpm = lyricsSheet.bpm,
                keySignature = lyricsSheet.key,
                lyrics = lyricsSheet.lyrics,
                notes = lyricsSheet.vibe_notes
            )
        }
        val id = repository.insertProject(updated)
        val saved = repository.getProjectById(if (current != null) current.id else id.toInt())
        if (saved != null) {
            _selectedProject.value = saved
            audioEngine.bpm = saved.bpm
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}

class SoundLabViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoundLabViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SoundLabViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

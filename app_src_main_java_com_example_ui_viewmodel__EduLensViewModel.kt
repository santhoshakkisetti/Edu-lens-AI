package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.local.AppDatabase
import com.example.data.local.ScanHistory
import com.example.data.model.EduLensResponse
import com.example.data.model.Flashcard
import com.example.data.model.MCQ
import com.example.data.preferences.UserPreferences
import com.example.data.repository.EduLensRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EduLensViewModel(
    application: Application,
    private val repository: EduLensRepository,
    private val preferences: UserPreferences
) : AndroidViewModel(application) {

    private val moshi: Moshi = RetrofitClient.moshiInstance

    // Auth States
    val isLoggedIn: StateFlow<Boolean> = preferences.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val userEmail: StateFlow<String?> = preferences.userEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val memberSince: StateFlow<String> = preferences.memberSince.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "June 2026"
    )

    // Study Streak
    val studyStreak: StateFlow<Int> = preferences.studyStreak.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // App Preferences
    val themeMode: StateFlow<String> = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val selectedLanguage: StateFlow<String> = preferences.selectedLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    // Scan History Flow
    val scanHistory: StateFlow<List<ScanHistory>> = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Scan / Analysis States
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<EduLensResponse?>(null)
    val analysisResult: StateFlow<EduLensResponse?> = _analysisResult.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _capturedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedImageBitmap: StateFlow<Bitmap?> = _capturedImageBitmap.asStateFlow()

    // Follow-up Doubt States
    private val _isAskingDoubt = MutableStateFlow(false)
    val isAskingDoubt: StateFlow<Boolean> = _isAskingDoubt.asStateFlow()

    private val _doubtResponse = MutableStateFlow<String?>(null)
    val doubtResponse: StateFlow<String?> = _doubtResponse.asStateFlow()

    // Auth actions
    fun login(email: String) {
        viewModelScope.launch {
            preferences.saveLoginState(email, true)
            preferences.incrementStreak() // First login of the day increments streak!
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferences.clear()
            _analysisResult.value = null
            _capturedImageBitmap.value = null
            _doubtResponse.value = null
        }
    }

    // Setters
    fun setCapturedImage(bitmap: Bitmap?) {
        _capturedImageBitmap.value = bitmap
    }

    /**
     * Pre-populates a general doubt clearing guide for students to query Gemini freely.
     */
    fun loadGeneralDoubtGuide() {
        _analysisResult.value = EduLensResponse(
            extractedText = "General AI doubt clearing session",
            explanation = "Welcome to your personal doubt session. Type any question in the chat bar below (e.g. 'Explain photosynthesis', 'How does a transformer work', or paste code) and I will clarify your concepts in detail.",
            teluguTranslation = "మీ వ్యక్తిగత సందేహాల నివృత్తి సెషన్‌కు స్వాగతం. కింద మీ ప్రశ్న టైప్ చేయండి.",
            hindiTranslation = "आपके व्यक्तिगत संदेह समाधान सत्र में आपका स्वागत है। नीचे अपना प्रश्न लिखें।",
            flashcards = listOf(
                Flashcard("What can I ask?", "You can ask about science, math, history, coding, languages, or specific homework questions!")
            ),
            mcqs = listOf(
                MCQ("Which AI is powering this tutor?", listOf("Gemini 3.5 Flash", "GPT-4", "Claude", "Llama"), 0, "EduLens AI uses Google's recommended Gemini 3.5 Flash model for lightning-fast educational analysis.")
            ),
            revisionNotes = "• EduLens AI is ready to help you learn.\n• Ask anything about formula derivations, grammatical rules, or science experiments."
        )
        _capturedImageBitmap.value = null
    }

    fun clearAnalysis() {
        _analysisResult.value = null
        _analysisError.value = null
        _doubtResponse.value = null
    }

    fun updateTheme(mode: String) {
        viewModelScope.launch {
            preferences.updateTheme(mode)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            preferences.updateLanguage(lang)
        }
    }

    // Main AI Trigger
    fun analyzeCapturedImage(localImageUri: String? = null) {
        val bitmap = _capturedImageBitmap.value
        if (bitmap == null) {
            _analysisError.value = "No image found to analyze."
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            _analysisResult.value = null

            repository.analyzeContent(bitmap, null)
                .onSuccess { result ->
                    _analysisResult.value = result
                    // Automatically increment study streak on successful analysis!
                    preferences.incrementStreak()
                    
                    // Automatically save to local history database for offline access
                    saveResultToHistory(result, localImageUri)
                }
                .onFailure { exception ->
                    _analysisError.value = exception.localizedMessage ?: "An error occurred during AI processing."
                }
            _isAnalyzing.value = false
        }
    }

    /**
     * Re-analyze / explain from mock text.
     */
    fun analyzeText(text: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            _analysisResult.value = null

            repository.analyzeContent(null, text)
                .onSuccess { result ->
                    _analysisResult.value = result
                    preferences.incrementStreak()
                    saveResultToHistory(result, null)
                }
                .onFailure { exception ->
                    _analysisError.value = exception.localizedMessage ?: "An error occurred during AI processing."
                }
            _isAnalyzing.value = false
        }
    }

    // Doubt resolution
    fun askDoubt(doubt: String) {
        val currentText = _analysisResult.value?.explanation ?: "No lesson text"
        if (doubt.isBlank()) return

        viewModelScope.launch {
            _isAskingDoubt.value = true
            _doubtResponse.value = null
            repository.answerDoubt(currentText, doubt)
                .onSuccess { answer ->
                    _doubtResponse.value = answer
                }
                .onFailure { exception ->
                    _doubtResponse.value = "Tutor Error: ${exception.localizedMessage}"
                }
            _isAskingDoubt.value = false
        }
    }

    fun clearDoubt() {
        _doubtResponse.value = null
    }

    // History Database actions
    private fun saveResultToHistory(result: EduLensResponse, imageUri: String?) {
        viewModelScope.launch {
            val flashcardsJson = try {
                val type = Types.newParameterizedType(List::class.java, Flashcard::class.java)
                moshi.adapter<List<Flashcard>>(type).toJson(result.flashcards)
            } catch (e: Exception) {
                "[]"
            }

            val mcqsJson = try {
                val type = Types.newParameterizedType(List::class.java, MCQ::class.java)
                moshi.adapter<List<MCQ>>(type).toJson(result.mcqs)
            } catch (e: Exception) {
                "[]"
            }

            val history = ScanHistory(
                extractedText = result.extractedText,
                explanation = result.explanation,
                teluguTranslation = result.teluguTranslation,
                hindiTranslation = result.hindiTranslation,
                flashcardsJson = flashcardsJson,
                mcqsJson = mcqsJson,
                revisionNotes = result.revisionNotes,
                imageUri = imageUri
            )
            repository.saveScan(history)
        }
    }

    fun deleteHistoryItem(item: ScanHistory) {
        viewModelScope.launch {
            repository.deleteScan(item)
        }
    }

    fun deleteHistoryItemById(id: Long) {
        viewModelScope.launch {
            repository.deleteScanById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Parsers for History items
    fun parseFlashcards(json: String): List<Flashcard> {
        return try {
            val type = Types.newParameterizedType(List::class.java, Flashcard::class.java)
            moshi.adapter<List<Flashcard>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseMCQs(json: String): List<MCQ> {
        return try {
            val type = Types.newParameterizedType(List::class.java, MCQ::class.java)
            moshi.adapter<List<MCQ>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Inject history item into active result (for viewing history details)
    fun loadHistoryItemToActive(item: ScanHistory) {
        _analysisResult.value = EduLensResponse(
            extractedText = item.extractedText,
            explanation = item.explanation,
            teluguTranslation = item.teluguTranslation,
            hindiTranslation = item.hindiTranslation,
            flashcards = parseFlashcards(item.flashcardsJson),
            mcqs = parseMCQs(item.mcqsJson),
            revisionNotes = item.revisionNotes
        )
        _capturedImageBitmap.value = null // Clears camera preview bitmap, we're showing a history item
    }
}

class EduLensViewModelFactory(
    private val application: Application,
    private val repository: EduLensRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EduLensViewModel::class.java)) {
            return EduLensViewModel(application, repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.vokabelblitz.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vokabelblitz.ai.GeminiTranslator
import com.example.vokabelblitz.ai.ModelStatus
import com.example.vokabelblitz.ai.TranslationResult
import com.example.vokabelblitz.data.AppDatabase
import com.example.vokabelblitz.data.Word
import com.example.vokabelblitz.data.WordDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the word input / translation flow on HomeScreen.
 */
sealed class TranslationState {
    data object Idle : TranslationState()
    data object Translating : TranslationState()
    data class Error(val message: String) : TranslationState()
}

/**
 * State for the quiz session.
 */
data class QuizState(
    val words: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val isRevealed: Boolean = false,
    val isFinished: Boolean = false,
    val knownCount: Int = 0,
    val learningCount: Int = 0,
    val isReversed: Boolean = false
) {
    val currentWord: Word? get() = words.getOrNull(currentIndex)
    val progress: Float get() = if (words.isEmpty()) 0f else currentIndex.toFloat() / words.size
    val totalWords: Int get() = words.size
}

class WordViewModel(application: Application) : AndroidViewModel(application) {

    private val wordDao: WordDao = AppDatabase.getDatabase(application).wordDao()
    private val translator = GeminiTranslator(application)

    // All words from the database, observed reactively
    val allWords: StateFlow<List<Word>?> = wordDao.getAllWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val wordCount: StateFlow<Int> = wordDao.getWordCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Input state
    private val _inputWord = MutableStateFlow("")
    val inputWord: StateFlow<String> = _inputWord.asStateFlow()

    // Translation state
    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    // Model status
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Downloading)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    // Quiz state
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    // Quiz active flag
    private val _isQuizActive = MutableStateFlow(false)
    val isQuizActive: StateFlow<Boolean> = _isQuizActive.asStateFlow()

    init {
        viewModelScope.launch {
            _modelStatus.value = translator.initialize()
        }
    }

    fun updateInputWord(word: String) {
        _inputWord.value = word
        if (_translationState.value is TranslationState.Error) {
            _translationState.value = TranslationState.Idle
        }
    }

    fun translateWord() {
        val word = _inputWord.value.trim()
        if (word.isBlank()) return

        _translationState.value = TranslationState.Translating

        viewModelScope.launch {
            val result = translator.translate(word)
            if (result != null) {
                val newWord = Word(
                    germanWord = result.germanWord,
                    englishTranslation = result.englishTranslation,
                    usageExample = result.usageExample
                )
                wordDao.insert(newWord)
                _inputWord.value = ""
                _translationState.value = TranslationState.Idle
            } else {
                _translationState.value = TranslationState.Error(
                    "Ungültiges Wort. Bitte überprüfe deine Schreibweise."
                )
            }
        }
    }

    fun clearTranslation() {
        _translationState.value = TranslationState.Idle
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordDao.delete(word)
        }
    }

    fun restoreWord(word: Word) {
        viewModelScope.launch {
            wordDao.insert(word)
        }
    }

    // Quiz functions
    fun startQuiz() {
        Log.d("WordViewModel", "startQuiz called. isReversed: ${_quizState.value.isReversed}")
        viewModelScope.launch {
            val words = wordDao.getAllWordsShuffled()
            Log.d("WordViewModel", "startQuiz: fetched ${words.size} words from DB")
            if (words.isNotEmpty()) {
                val previousReversed = _quizState.value.isReversed
                _quizState.value = QuizState(words = words, isReversed = previousReversed)
                _isQuizActive.value = true
                Log.d("WordViewModel", "startQuiz: _quizState updated with new words")
            } else {
                Log.w("WordViewModel", "startQuiz: words list from DB is empty!")
            }
        }
    }

    fun toggleQuizLanguage() {
        val current = _quizState.value
        val shuffledWords = current.words.shuffled()
        _quizState.value = QuizState(
            words = shuffledWords,
            isReversed = !current.isReversed
        )
    }

    fun revealAnswer() {
        _quizState.value = _quizState.value.copy(isRevealed = true)
    }

    fun markKnown() {
        advanceQuiz(known = true)
    }

    fun markLearning() {
        advanceQuiz(known = false)
    }

    private fun advanceQuiz(known: Boolean) {
        val current = _quizState.value
        val currentWord = current.currentWord

        val newWords = if (!known && currentWord != null) {
            current.words + currentWord
        } else {
            current.words
        }

        val nextIndex = current.currentIndex + 1
        val isFinished = nextIndex >= newWords.size

        _quizState.value = current.copy(
            words = newWords,
            currentIndex = nextIndex,
            isRevealed = false,
            isFinished = isFinished,
            knownCount = current.knownCount + if (known) 1 else 0,
            learningCount = current.learningCount + if (!known) 1 else 0
        )
    }

    fun endQuiz() {
        Log.d("WordViewModel", "endQuiz called. Resetting quiz state.")
        _isQuizActive.value = false
        _quizState.value = QuizState()
    }

    override fun onCleared() {
        super.onCleared()
        translator.close()
    }
}

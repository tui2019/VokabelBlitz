package com.example.vokabelblitz.ai

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/**
 * Result of a translation request from Gemini Nano.
 */
data class TranslationResult(
    val germanWord: String,
    val englishTranslation: String,
    val usageExample: String
)

/**
 * Sealed class representing the status of the Gemini Nano model.
 */
sealed class ModelStatus {
    data object Available : ModelStatus()
    data object Downloading : ModelStatus()
    data object Unavailable : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

/**
 * Service that uses Gemini Nano (via ML Kit GenAI Prompt API) to translate
 * English words to German and generate usage examples, all on-device.
 */
class GeminiTranslator(private val context: Context) {

    companion object {
        private const val TAG = "GeminiTranslator"
    }

    private var generativeModel: GenerativeModel? = null

    /**
     * Initialize the generative model. Should be called once, e.g. from ViewModel init.
     */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    suspend fun initialize(): ModelStatus = withContext(Dispatchers.IO) {
        try {
            val model = Generation.getClient()
            val status = model.checkStatus()

            Log.d(TAG, "Gemini Nano status: $status")

            when (status) {
                FeatureStatus.AVAILABLE -> {
                    generativeModel = model
                    Log.d(TAG, "Gemini Nano model is available")
                    ModelStatus.Available
                }
                FeatureStatus.DOWNLOADABLE -> {
                    Log.w(TAG, "Gemini Nano model is downloadable, starting download...")
                    generativeModel = model
                    
                    // Trigger download in the background
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        try {
                            model.download().collect { downloadStatus ->
                                when (downloadStatus) {
                                    is DownloadStatus.DownloadCompleted -> {
                                        Log.d(TAG, "Model download completed successfully")
                                    }
                                    is DownloadStatus.DownloadFailed -> {
                                        Log.e(TAG, "Model download failed", downloadStatus.e)
                                    }
                                    is DownloadStatus.DownloadProgress -> {
                                        Log.d(TAG, "Download progress: ${downloadStatus.totalBytesDownloaded} bytes")
                                    }
                                    else -> {
                                        Log.d(TAG, "Download status: $downloadStatus")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in model download collection", e)
                        }
                    }
                    ModelStatus.Downloading
                }
                FeatureStatus.DOWNLOADING -> {
                    Log.d(TAG, "Gemini Nano model download is in progress")
                    generativeModel = model
                    ModelStatus.Downloading
                }
                FeatureStatus.UNAVAILABLE -> {
                    Log.w(TAG, "Gemini Nano is unavailable on this device")
                    ModelStatus.Unavailable
                }
                else -> {
                    Log.d(TAG, "Gemini Nano model status: $status")
                    generativeModel = model
                    ModelStatus.Downloading
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemini Nano", e)
            ModelStatus.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Translate a German word to English and provide a usage example.
     * Returns null if the model is not available.
     */
    suspend fun translate(germanWord: String): TranslationResult? = withContext(Dispatchers.IO) {
        val model = generativeModel
        if (model == null) {
            Log.w(TAG, "Model not initialized, attempting initialization")
            initialize()
        }

        val activeModel = generativeModel ?: run {
            Log.e(TAG, "Model still not available after initialization attempt")
            return@withContext null
        }

        try {
            val prompt = buildTranslationPrompt(germanWord)
            val request = GenerateContentRequest.Builder(TextPart(prompt)).build()

            val response = activeModel.generateContent(request)
            val responseText = response.candidates.firstOrNull()?.text ?: return@withContext null

            Log.d(TAG, "Raw AI response: $responseText")
            parseTranslationResponse(responseText, germanWord)
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed for '$germanWord'", e)
            null
        }
    }

    private fun buildTranslationPrompt(word: String): String {
        return """You are a highly accurate German-to-English dictionary and grammar assistant.
Analyze the German input word or phrase: "$word"

First, determine its primary part of speech. Then, format the German word/phrase and translate it strictly according to these rules:

1. VERBS: If the input is primarily a German verb in its infinitive form (e.g., "spielen", "laufen", "kochen", "gehen", "sehen", "arbeiten", "essen"):
   - Keep it strictly in its lowercase infinitive form (e.g., "spielen").
   - DO NOT capitalize the first letter.
   - DO NOT prepend any definite article ("der", "die", or "das").
   - DO NOT convert it into a noun (for example, "laufen" must remain "laufen", NOT "das Laufen").
   - The English translation MUST start with "to " (e.g., "to run", "to play").

2. ADJECTIVES/ADVERBS: If the input is primarily a German adjective or adverb (e.g., "schnell", "schön", "oft", "blau", "kalt"):
   - Keep it strictly in its lowercase, basic natural form (e.g., "schnell").
   - DO NOT prepend any article.
   - DO NOT convert it into a noun (for example, "schnell" must remain "schnell", NOT "die Schnelligkeit").

3. NOUNS: ONLY if the input is primarily a German noun (e.g., "Apfel", "Tasche", "Buch", "Computer", "Küche"):
   - Capitalize the first letter (e.g., "Apfel").
   - Determine its grammatical gender and prepend the correct definite article ("der", "die", or "das") if it is missing (e.g., "Apfel" -> "der Apfel", "Tasche" -> "die Tasche", "Buch" -> "das Buch").

If the input "$word" is gibberish, invalid, misspelled, or not a real German word/phrase, you MUST respond ONLY with:
ERROR: Invalid German word

Otherwise, respond ONLY in this exact format with no extra text or explanations:
FORMATTED: [the formatted German word/phrase based on the rules above]
TRANSLATION: [the English translation]
EXAMPLE: [a natural, grammatically correct German sentence using the formatted German word/phrase]"""
    }

    private fun parseTranslationResponse(response: String, originalWord: String): TranslationResult? {
        val lines = response.lines().map { it.trim() }.filter { it.isNotBlank() }

        // If the AI flagged it as gibberish or invalid, return null to trigger error state in VM
        if (lines.any { it.contains("ERROR:", ignoreCase = true) || it.contains("Invalid German word", ignoreCase = true) }) {
            Log.w(TAG, "Input was flagged as invalid or gibberish: $response")
            return null
        }

        var formatted: String? = null
        var translation: String? = null
        var example: String? = null

        for (line in lines) {
            when {
                line.startsWith("FORMATTED:", ignoreCase = true) -> {
                    formatted = line.substringAfter(":").trim()
                }
                line.startsWith("TRANSLATION:", ignoreCase = true) -> {
                    translation = line.substringAfter(":").trim()
                }
                line.startsWith("EXAMPLE:", ignoreCase = true) -> {
                    example = line.substringAfter(":").trim()
                }
            }
        }

        // Safe fallback in case AI outputs translation directly or format is slightly different
        if (formatted == null) {
            formatted = originalWord
        }
        if (translation == null && lines.isNotEmpty()) {
            // Find a line that doesn't start with keywords or just use the first line
            translation = lines.firstOrNull { 
                !it.startsWith("FORMATTED:", ignoreCase = true) && 
                !it.startsWith("EXAMPLE:", ignoreCase = true) 
            }?.substringAfter(":")?.trim() ?: lines.first().trim()
        }

        return translation?.let {
            TranslationResult(
                germanWord = formatted,
                englishTranslation = it,
                usageExample = example ?: "Kein Beispiel verfügbar."
            )
        }
    }

    /**
     * Release resources when no longer needed.
     */
    fun close() {
        generativeModel = null
    }
}

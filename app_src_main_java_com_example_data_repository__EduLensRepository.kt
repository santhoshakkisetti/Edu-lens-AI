package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.api.RetrofitClient
import com.example.data.local.ScanHistory
import com.example.data.local.ScanHistoryDao
import com.example.data.model.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EduLensRepository(
    private val scanHistoryDao: ScanHistoryDao,
    private val moshi: Moshi = RetrofitClient.moshiInstance
) {
    val allHistory: Flow<List<ScanHistory>> = scanHistoryDao.getAllHistory()

    fun getHistoryById(id: Long): Flow<ScanHistory?> {
        return scanHistoryDao.getHistoryById(id)
    }

    suspend fun saveScan(history: ScanHistory): Long = withContext(Dispatchers.IO) {
        scanHistoryDao.insert(history)
    }

    suspend fun deleteScan(history: ScanHistory) = withContext(Dispatchers.IO) {
        scanHistoryDao.delete(history)
    }

    suspend fun deleteScanById(id: Long) = withContext(Dispatchers.IO) {
        scanHistoryDao.deleteById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        scanHistoryDao.clearAll()
    }

    /**
     * Converts a Bitmap to a Base64 JPEG string for Gemini API.
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Analyzes an image (represented as a Bitmap) or a piece of text with Gemini,
     * returning a parsed EduLensResponse object.
     */
    suspend fun analyzeContent(bitmap: Bitmap?, fallbackText: String?): Result<EduLensResponse> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not set. Please add it via the Secrets panel in AI Studio."))
        }

        val prompt = """
            You are an expert OCR and Educational assistant. Analyze the provided image or text.
            If an image is provided, perform OCR to extract the main textbook, blackboard, or notebook content.
            If no image is provided, use the given text.
            
            Then, generate a comprehensive educational analysis in simple English:
            1. Explain the concepts captured in simple, friendly English. Include clear real-world examples.
            2. Translate the concept explanation into Telugu (using Telugu script).
            3. Translate the concept explanation into Hindi (using Devanagari script).
            4. Generate a list of 4-6 flashcards (each with a question and answer).
            5. Generate 3-5 multiple-choice questions (MCQs) (each with question, list of options, correctAnswerIndex (0-based), and explanation).
            6. Generate a brief list of Revision Notes summarizing key points.

            You MUST format your response as a single, valid JSON object matching this schema:
            {
              "extractedText": "the raw extracted OCR text",
              "explanation": "comprehensive explanation in English with examples",
              "teluguTranslation": "translation in Telugu script",
              "hindiTranslation": "translation in Hindi script",
              "flashcards": [
                {
                  "question": "question string",
                  "answer": "answer string"
                }
              ],
              "mcqs": [
                {
                  "question": "question string",
                  "options": ["option A", "option B", "option C", "option D"],
                  "correctAnswerIndex": 0,
                  "explanation": "explanation of why it's correct"
                }
              ],
              "revisionNotes": "bulleted string or paragraph summarizing main points for quick revision"
            }

            Ensure the output contains ONLY this JSON block. No markdown formatting like ```json or any other text outside the JSON.
        """.trimIndent()

        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))

        if (bitmap != null) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64())))
        } else if (!fallbackText.isNullOrBlank()) {
            parts.add(Part(text = "Input text to analyze: $fallbackText"))
        } else {
            return@withContext Result.failure(Exception("No image or text provided for analysis"))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No response received from Gemini API"))

            val adapter = moshi.adapter(EduLensResponse::class.java)
            val parsed = adapter.fromJson(jsonText)
                ?: return@withContext Result.failure(Exception("Failed to parse Gemini response into structured data"))
            
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Answers a follow-up student doubt.
     */
    suspend fun answerDoubt(contextText: String, doubt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not set."))
        }

        val prompt = """
            You are a helpful educational tutor. The student is reviewing this material:
            
            $contextText
            
            They have the following doubt or question about it:
            "$doubt"
            
            Provide a clear, engaging, and easy-to-understand response that directly answers their doubt, using examples if helpful. Keep it concise.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val answer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No answer returned"))
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

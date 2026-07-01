package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Flashcard(
    val question: String,
    val answer: String
)

@JsonClass(generateAdapter = true)
data class MCQ(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class EduLensResponse(
    val extractedText: String,
    val explanation: String,
    val teluguTranslation: String,
    val hindiTranslation: String,
    val flashcards: List<Flashcard>,
    val mcqs: List<MCQ>,
    val revisionNotes: String
)

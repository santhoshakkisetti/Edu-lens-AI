package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val extractedText: String,
    val explanation: String,
    val teluguTranslation: String,
    val hindiTranslation: String,
    val flashcardsJson: String,
    val mcqsJson: String,
    val revisionNotes: String,
    val imageUri: String?,
    val timestamp: Long = System.currentTimeMillis()
)

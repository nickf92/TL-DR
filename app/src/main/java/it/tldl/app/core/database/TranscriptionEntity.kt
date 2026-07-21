package it.tldl.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val transcribedText: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_projects")
data class AudioProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String = "",
    val bpm: Int = 120,
    val keySignature: String = "C Major",
    val lyrics: String = "",
    val selectedPresetId: Int = 0,
    val isRecordingSample: Boolean = false,
    val lastSaved: Long = System.currentTimeMillis()
)

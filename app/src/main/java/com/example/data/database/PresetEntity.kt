package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bands: String, // Comma-separated string for 10 frequency bands in dB (-12.0 to +12.0)
    val compressorThreshold: Float, // -60f to 0f
    val reverbDecay: Float, // 0f to 1f
    val delayFeedback: Float, // 0f to 1f
    val drive: Float, // 0f to 2f (overdrive distortion)
    val wideness: Float, // 0f to 1f
    val vocalsGain: Float, // 0f to 2f
    val isUserCreated: Boolean = true,
    val creationDate: Long = System.currentTimeMillis()
)

package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundLabDao {

    // --- PRESETS ---
    @Query("SELECT * FROM presets ORDER BY creationDate DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: Int): PresetEntity?

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)

    // --- PROJECTS ---
    @Query("SELECT * FROM audio_projects ORDER BY lastSaved DESC")
    fun getAllProjects(): Flow<List<AudioProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AudioProjectEntity): Long

    @Query("SELECT * FROM audio_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): AudioProjectEntity?

    @Query("DELETE FROM audio_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

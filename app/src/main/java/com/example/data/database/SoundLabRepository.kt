package com.example.data.database

import kotlinx.coroutines.flow.Flow

class SoundLabRepository(private val dao: SoundLabDao) {

    val allPresets: Flow<List<PresetEntity>> = dao.getAllPresets()
    val allProjects: Flow<List<AudioProjectEntity>> = dao.getAllProjects()

    suspend fun insertPreset(preset: PresetEntity): Long {
        return dao.insertPreset(preset)
    }

    suspend fun getPresetById(id: Int): PresetEntity? {
        return dao.getPresetById(id)
    }

    suspend fun deletePresetById(id: Int) {
        dao.deletePresetById(id)
    }

    suspend fun insertProject(project: AudioProjectEntity): Long {
        return dao.insertProject(project)
    }

    suspend fun getProjectById(id: Int): AudioProjectEntity? {
        return dao.getProjectById(id)
    }

    suspend fun deleteProjectById(id: Int) {
        dao.deleteProjectById(id)
    }
}

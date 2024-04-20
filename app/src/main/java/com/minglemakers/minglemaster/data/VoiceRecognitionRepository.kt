package com.minglemakers.minglemaster.data

import kotlinx.coroutines.flow.Flow
import com.minglemakers.minglemaster.data.entity.VoiceCommandEntity

interface VoiceRecognitionRepository {

    val commandsFlow: Flow<VoiceCommandEntity>
    fun startRecognition()
    fun resume()
    fun pause()
    fun release()
}
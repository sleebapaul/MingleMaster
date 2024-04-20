package com.minglemakers.minglemaster.ui.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import com.minglemakers.minglemaster.data.VoiceRecognitionRepository
import com.minglemakers.minglemaster.data.entity.VoiceCommandEntity

abstract class VoiceManagedViewModel(
    private val voiceRecognizer: VoiceRecognitionRepository,
) : ViewModel() {
    val commandState: Flow<VoiceCommandEntity> = voiceRecognizer.commandsFlow

    fun startRecognition(){
        voiceRecognizer.startRecognition()
    }

    protected fun release() {
        voiceRecognizer.release()
    }
}
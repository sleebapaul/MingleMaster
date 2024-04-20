package com.minglemakers.minglemaster.ui.home

import com.minglemakers.minglemaster.data.VoiceRecognitionRepository
import com.minglemakers.minglemaster.ui.base.VoiceManagedViewModel

class HomeScreenViewModel(
    voiceRecognitionRepository: VoiceRecognitionRepository
): VoiceManagedViewModel(voiceRecognitionRepository)
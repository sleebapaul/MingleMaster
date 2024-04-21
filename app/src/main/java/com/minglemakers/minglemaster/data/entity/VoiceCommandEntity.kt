package com.minglemakers.minglemaster.data.entity

import com.google.ai.client.generativeai.GenerativeModel

class VoiceCommandEntity() {

    public var command = "";

    companion object {
            fun processCommand(output: String): VoiceCommandEntity =
                VoiceCommandEntity().applyParams(output)
        }
}

fun VoiceCommandEntity.applyParams(text: String): VoiceCommandEntity = this.apply {
    command = text
}


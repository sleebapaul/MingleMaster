package com.minglemakers.minglemaster.data.entity

import com.google.ai.client.generativeai.GenerativeModel

enum class VoiceCommandEntity(val keyWords: Set<String>, var params: String? = null) {
    YES(setOf("yes")),
    NO(setOf("no")),
    DEFAULT(setOf());

    companion object {
            fun processCommand(output: String): VoiceCommandEntity =
                values().firstOrNull { command ->
                    command.keyWords.firstOrNull { word -> output.trim().contains(word) } != null
                }?.applyParams(output) ?: DEFAULT

            fun getAllCommands() = values().flatMap { set -> set.keyWords.map { it } }
        }
}

fun VoiceCommandEntity.isIn(text: String): Boolean =
    keyWords.sumOf { if (text.contains(it)) 1.toInt() else 0 } > 0


fun VoiceCommandEntity.applyParams(text: String): VoiceCommandEntity = this.apply {
    params = text
    keyWords.forEach { params = params?.replace(it, "", true) }
}


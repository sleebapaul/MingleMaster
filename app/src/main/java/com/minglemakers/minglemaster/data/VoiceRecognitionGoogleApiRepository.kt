package com.minglemakers.minglemaster.data

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.api.gax.rpc.ClientStream
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechContext
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import com.minglemakers.minglemaster.data.entity.VoiceCommandEntity
import android.speech.tts.TextToSpeech
import org.koin.android.ext.BuildConfig

class VoiceRecognitionGoogleApiRepository(
    private val _speechClient: SpeechClient,
    private val _externalScope: CoroutineScope,
) : VoiceRecognitionRepository {
    companion object {
        private const val TAG = "VoiceRecognitionGoogleApiRepository"
    }

    private var _clientStream: ClientStream<StreamingRecognizeRequest>? = null
    private var _voiceRecorder: AudioRecord? = null
    private var _isStreaming = true

    override val commandsFlow: Flow<VoiceCommandEntity> = callbackFlow {
        val callback = object : ResponseObserver<StreamingRecognizeResponse> {
            override fun onStart(controller: StreamController?) {
                Log.d(TAG, "ResponseObserver.onStart")
            }

            override fun onResponse(response: StreamingRecognizeResponse?) {
                Log.d(TAG, "ResponseObserver.onResponse($response)")
                response?.let {
                    var text: String? = null
                    var isFinal = false
                    if (response.resultsCount > 0) {
                        val result = response.getResults(0)
                        isFinal = result.isFinal
                        if (result.alternativesCount > 0) {
                            val alternative = result.getAlternatives(0)
                            text = alternative.transcript
                        }
                    }
                    if (isFinal) text?.let {
                        val generativeModel = GenerativeModel(
                            // For text-only input, use the gemini-pro model
                            modelName = "gemini-pro",
                            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
                            apiKey = ""
                        )

                        val prompt = """Based on following considerations return if there is an awkward silence in the transcript mentioned in triple quotes. You must return just "Yes" or "No".

                        1. A sudden drop in sentiment score (positive to neutral or negative) could indicate a shift in conversation, potentially leading to silence.

                        2. Analyze audio properties like pauses, speech rate, and disfluencies ("um," "uh").

                        3. Longer pauses than the average conversation flow could indicate silence.

                        4. Sudden drops in speech rate or increased disfluencies might suggest someone searching for words, potentially leading to silence.

                        5. Analyze transitions between topics. A sudden shift without proper conclusion might suggest an awkward pause.

                        6. Pay attention to neutral scores too. A string of neutral sentences might suggest a lack of engagement, possibly leading to an awkward pause.
                        
                        7. If no transcript to process, it must be awkward silence. 
                        
                        /"/"/" $text /"/"/"  """


                        _externalScope.launch {
                            val response = generativeModel.generateContent(prompt)
                            trySend(VoiceCommandEntity.processCommand(response.text!!))
                        }
                    }
                }

            }

            override fun onError(t: Throwable?) {
                Log.e(TAG, "onError[${t?.message}]")
            }

            override fun onComplete() {
                Log.d(TAG, "onComplete $_clientStream $_voiceRecorder")
            }
        }


        _clientStream = _speechClient.streamingRecognizeCallable()?.splitCall(callback)
        _clientStream?.send(startRequest)

        awaitClose {
            _voiceRecorder?.release()
            _clientStream?.closeSend()
            _voiceRecorder = null
            _clientStream = null
        }
    }.shareIn(_externalScope, SharingStarted.Lazily, replay = 0)


    override fun startRecognition() {
        startRecording()
    }

    private fun startRecording() {
        if (_voiceRecorder != null) return
        _externalScope.launch {
            var minBufferSize = Constants.BUFFER_SIZE
            try {
                val buffer = ByteArray(minBufferSize)
                initVoiceRecorder()
                _voiceRecorder?.apply {
                    startRecording()
                    while (true) {
                        minBufferSize = read(buffer, 0, buffer.size)
                        recognize(buffer, minBufferSize)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun recognize(data: ByteArray?, size: Int) {
        if (!_isStreaming) return
        _clientStream?.send(
            StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size)).build()
        )
    }

    override fun resume() {
        _isStreaming = true
    }

    override fun pause() {
        _isStreaming = false
    }

    private val sampleRate: Int
        get() = _voiceRecorder?.sampleRate ?: 0

    private val startRequest = StreamingRecognizeRequest.newBuilder()
        .setStreamingConfig(
            StreamingRecognitionConfig.newBuilder()
                .setConfig(
                    RecognitionConfig.newBuilder()
//                        .addSpeechContexts(
//                            SpeechContext.newBuilder()
//                                .addAllPhrases(VoiceCommandEntity.getAllCommands())
//                        )
                        .setLanguageCode(Constants.LANGUAGE_CODE)
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(Constants.SAMPLE_RATE)
                        .build()
                )
                .setInterimResults(true)
                .setSingleUtterance(false)
                .build()
        ).build()

    @SuppressLint("MissingPermission")
    private fun initVoiceRecorder() = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        Constants.CHANNEL_CONFIG,
        Constants.AUDIO_FORMAT,
        Constants.BUFFER_SIZE * 10
    ).also {
        _voiceRecorder = it
    }

    override fun release() {
        _voiceRecorder?.release()
        _voiceRecorder = null
        _clientStream?.closeSend()
        _clientStream = null
    }
}
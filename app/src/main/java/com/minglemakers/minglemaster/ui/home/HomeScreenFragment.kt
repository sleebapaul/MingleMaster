package com.minglemakers.minglemaster.ui.home

import android.Manifest.permission.RECORD_AUDIO
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.minglemakers.minglemaster.R
import com.minglemakers.minglemaster.data.entity.VoiceCommandEntity
import com.minglemakers.minglemaster.databinding.FragmentHomeScreenBinding
import com.minglemakers.minglemaster.ui.base.VoiceManagedFragment
import com.minglemakers.minglemaster.utils.checkAndRequestPermissions
import com.minglemakers.minglemaster.utils.navigateTo
import com.minglemakers.minglemaster.utils.viewBinding
import android.speech.tts.TextToSpeech
import java.util.Locale


class HomeScreenFragment :
    VoiceManagedFragment<HomeScreenViewModel>(R.layout.fragment_home_screen),TextToSpeech.OnInitListener {
    companion object {
        private const val TAG = "HomeScreenFragment"
    }

    private val binding by viewBinding { FragmentHomeScreenBinding.bind(it) }
    override val viewModel: HomeScreenViewModel by viewModel()
    private var tts: TextToSpeech? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->
        Log.d(TAG, "RECORD_AUDIO granted=${permission[RECORD_AUDIO]}")
        if (permission[RECORD_AUDIO] == true) viewModel.startRecognition()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions(
            listOf(RECORD_AUDIO),
            requestPermissionLauncher,
            onGranted = { viewModel.startRecognition() }
        )
        tts = TextToSpeech(activity, this)
    }

    override fun commandProcessing(command: VoiceCommandEntity) {
        if (command.command == "Yes")
            speakOut("Awkward Silence Detected")
    }

    override fun navigateNext() {
//        navigateTo(HomeScreenFragmentDirections.actionHomeScreenFragmentToContentScreenFragment())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            } else {
            }
        }
    }
    private fun speakOut(text : String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    public override fun onDestroy() {
        // Shutdown TTS when
        // activity is destroyed
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}
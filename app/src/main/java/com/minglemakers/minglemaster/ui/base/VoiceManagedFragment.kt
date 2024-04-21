package com.minglemakers.minglemaster.ui.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.core.context.stopKoin
import com.minglemakers.minglemaster.data.entity.VoiceCommandEntity
import com.minglemakers.minglemaster.utils.backTo
import com.minglemakers.minglemaster.utils.toast

abstract class VoiceManagedFragment<T : VoiceManagedViewModel>(
    fragmentId: Int,
) : Fragment(fragmentId) {
    companion object {
        private const val TAG = "VoiceManagedFragment"
    }

    abstract val viewModel: T

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commandSubscription()
    }

    private fun commandSubscription() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.commandState.collect {
                        requireContext().toast(it.command)
                        commandProcessing(it)
                    }
                }
            }
        }
    }

    open fun commandProcessing(command: VoiceCommandEntity) {

    }

    open fun exit() {
        requireActivity().finishAffinity()
        stopKoin()
    }

    open fun navigateNext() {
    }

    open fun navigateBack() {
        backTo()
    }
}
package com.liferlighdow.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HardwareViewModel : ViewModel() {

    private val _cpuFrequencies = MutableStateFlow<List<String>>(emptyList())
    val cpuFrequencies: StateFlow<List<String>> = _cpuFrequencies.asStateFlow()

    private var frequencyJob: Job? = null

    fun startFrequencyUpdates() {
        frequencyJob?.cancel()
        frequencyJob = viewModelScope.launch(Dispatchers.IO) {
            val coreCount = Runtime.getRuntime().availableProcessors()
            while (isActive) {
                val freqs = mutableListOf<String>()
                for (i in 0 until coreCount) {
                    val freq = HardwareProvider.readFileLine("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                    val displayFreq = if (freq.isNotEmpty()) {
                        try {
                            val f = freq.trim().toLong()
                            "${f / 1000} MHz"
                        } catch (e: Exception) {
                            freq
                        }
                    } else "N/A"
                    freqs.add(displayFreq)
                }
                _cpuFrequencies.value = freqs
                delay(1000)
            }
        }
    }

    fun stopFrequencyUpdates() {
        frequencyJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopFrequencyUpdates()
    }
}

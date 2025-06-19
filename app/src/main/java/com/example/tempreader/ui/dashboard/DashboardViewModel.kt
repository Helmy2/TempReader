package com.example.tempreader.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tempreader.data.local.Reading
import com.example.tempreader.data.repository.ReadingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(repository: ReadingRepository) : ViewModel() {
    val readings: StateFlow<List<Reading>> = repository.allReadings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        repository.syncWithFirebase()
    }
}
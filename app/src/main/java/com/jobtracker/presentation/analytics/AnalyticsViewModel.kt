package com.jobtracker.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.domain.model.AnalyticsData
import com.jobtracker.domain.usecase.GetAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsUseCase: GetAnalyticsUseCase
) : ViewModel() {

    private val _analytics = MutableStateFlow(AnalyticsData())
    val analytics: StateFlow<AnalyticsData> = _analytics.asStateFlow()

    init {
        viewModelScope.launch {
            getAnalyticsUseCase().collect { _analytics.value = it }
        }
    }
}

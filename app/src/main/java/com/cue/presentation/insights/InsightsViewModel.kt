package com.cue.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cue.domain.model.Insight
import com.cue.domain.usecase.GenerateInsightsUseCase
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InsightsUiState(
    val insights: List<Insight> = emptyList(),
    val isLoading: Boolean = false
)

class InsightsViewModel(
    private val userRepository: UserRepository,
    private val insightRepository: InsightRepository,
    private val generateInsightsUseCase: GenerateInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Trigger generation before loading
            generateInsightsUseCase()
            
            val user = userRepository.getCurrentUser()
            if (user != null) {
                //instead of loading all the insights we load the latest one by using latest per type filter
                val insights = insightRepository.getUserInsights(user.id)

                //get the latest insight
                _uiState.value = _uiState.value.copy(
                    insights =  insights!!.groupBy { insight -> insight.type }.map { it.value.maxByOrNull { insight -> insight.timestamp }!!},
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

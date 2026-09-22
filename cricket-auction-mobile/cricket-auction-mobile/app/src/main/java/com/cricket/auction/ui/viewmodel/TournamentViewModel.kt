package com.cricket.auction.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.data.model.TournamentStatus
import com.cricket.auction.data.repository.CricketAuctionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TournamentUiState(
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

class TournamentViewModel(
    private val repository: CricketAuctionRepository
) : ViewModel() {

    val tournaments: StateFlow<List<TournamentEntity>> = repository.getAllTournaments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(TournamentUiState())
    val uiState: StateFlow<TournamentUiState> = _uiState.asStateFlow()

    fun createTournament(
        name: String,
        edition: String,
        budgetPerTeam: Long,
        currencyFormat: CurrencyFormat,
        minSquadSize: Int,
        maxSquadSize: Int,
        maxOverseasPlayers: Int,
        defaultBasePrice: Long,
        onSuccess: (Long) -> Unit
    ) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Tournament name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val tournament = TournamentEntity(
                    name = name.trim(),
                    edition = edition.trim(),
                    budgetPerTeam = budgetPerTeam,
                    currencyFormat = currencyFormat,
                    minSquadSize = minSquadSize,
                    maxSquadSize = maxSquadSize,
                    maxOverseasPlayers = maxOverseasPlayers,
                    defaultBasePrice = defaultBasePrice,
                    status = TournamentStatus.UPCOMING
                )
                val id = repository.saveTournament(tournament)
                _uiState.value = TournamentUiState()
                onSuccess(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to create tournament")
            }
        }
    }

    fun deleteTournament(tournamentId: Long) {
        viewModelScope.launch {
            repository.deleteTournament(tournamentId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        fun provideFactory(repository: CricketAuctionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TournamentViewModel(repository) as T
                }
            }
        }
    }
}

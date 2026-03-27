package com.example.pokedex.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.data.repository.PokemonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val pokemonRepository: PokemonRepository,
    private val favouriteRepository: FavouriteRepository,
    savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val pokemonId: Int = checkNotNull(savedStateHandle["pokemonId"])

    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).also {
        viewModelScope.launch { it.emit(Unit) }
    }

    private val detailFlow = retryTrigger.flatMapLatest {
        flow {
            emit(DetailLoadState.Loading)
            pokemonRepository.getPokemonDetail(pokemonId).fold(
                onSuccess = { emit(DetailLoadState.Success(it)) },
                onFailure = { emit(DetailLoadState.Failure(it.message ?: "Failed to load Pokémon")) }
            )
        }
    }

    private val isFavouriteFlow = favouriteRepository.favouriteIds
        .map { ids -> ids.contains(pokemonId) }
        .distinctUntilChanged()

    val uiState: StateFlow<PokemonDetailUiState> = combine(
        detailFlow,
        isFavouriteFlow
    ) { loadState, isFavourite ->
        when (loadState) {
            is DetailLoadState.Loading -> PokemonDetailUiState.Loading
            is DetailLoadState.Failure -> PokemonDetailUiState.Error(loadState.message)
            is DetailLoadState.Success -> PokemonDetailUiState.Success(
                pokemon = loadState.pokemon,
                isFavourite = isFavourite
            )
        }
    }
        .onStart { emit(PokemonDetailUiState.Loading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PokemonDetailUiState.Loading)

    fun onEvent(event: PokemonDetailEvent) {
        when (event) {
            is PokemonDetailEvent.Retry -> viewModelScope.launch { retryTrigger.emit(Unit) }
            is PokemonDetailEvent.ToggleFavourite -> toggleFavourite(event.pokemonName)
        }
    }

    private fun toggleFavourite(pokemonName: String) {
        viewModelScope.launch(dispatcher) {
            if (favouriteRepository.isFavourite(pokemonId)) {
                favouriteRepository.removeFavourite(pokemonId)
            } else {
                favouriteRepository.addFavourite(pokemonId, pokemonName)
            }
        }
    }

    private sealed interface DetailLoadState {
        data object Loading : DetailLoadState
        data class Success(val pokemon: com.example.pokedex.data.model.PokemonDetail) : DetailLoadState
        data class Failure(val message: String) : DetailLoadState
    }
}

sealed interface PokemonDetailEvent {
    data object Retry : PokemonDetailEvent
    data class ToggleFavourite(val pokemonName: String) : PokemonDetailEvent
}
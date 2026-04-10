package com.example.pokedex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.data.repository.PokemonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val pokemonRepository: PokemonRepository,
    private val favouriteRepository: FavouriteRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _refreshTrigger = MutableStateFlow(0)

    private val debouncedQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    private val pokemonListFlow = combine(debouncedQuery, _refreshTrigger) { query, _ -> query }
        .flatMapLatest { query ->
            flow {
                emit(PokemonListLoadState.Loading)
                val result = withContext(dispatcher) {
                    if (query.isBlank()) {
                        pokemonRepository.getPokemonList()
                    } else {
                        pokemonRepository.searchPokemon(query)
                    }
                }
                result.fold(
                    onSuccess = { list ->
                        if (list.isEmpty()) emit(PokemonListLoadState.Empty)
                        else emit(PokemonListLoadState.Success(list, query))
                    },
                    onFailure = { error ->
                        emit(PokemonListLoadState.Failure(error.message ?: "Unknown error"))
                    }
                )
            }
        }

    private val favouritesFlow = favouriteRepository.favouriteIds
        .onStart { emit(emptyList()) }
        .map { it.toSet() }

    val favourites: StateFlow<Set<Int>> = favouriteRepository.favouriteIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val uiState: StateFlow<PokemonListUiState> = combine(
        pokemonListFlow,
        favouritesFlow
    ) { listState, favs ->
        when (listState) {
            is PokemonListLoadState.Loading -> PokemonListUiState.Loading
            is PokemonListLoadState.Empty -> PokemonListUiState.Empty
            is PokemonListLoadState.Failure -> PokemonListUiState.Error(listState.message)
            is PokemonListLoadState.Success -> PokemonListUiState.Success(
                pokemonList = listState.list,
                searchQuery = listState.query,
                favourites = favs
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PokemonListUiState.Loading)

    fun onEvent(event: PokemonListEvent) {
        when (event) {
            is PokemonListEvent.Search -> _searchQuery.value = event.query
            is PokemonListEvent.Retry -> _refreshTrigger.value++
            is PokemonListEvent.Refresh -> {
                _searchQuery.value = ""
                _refreshTrigger.value++
            }
            is PokemonListEvent.AddFavourite -> addFavourite(event.pokemonId, event.pokemonName)
            is PokemonListEvent.RemoveFavourite -> removeFavourite(event.pokemonId)
        }
    }

    private fun addFavourite(pokemonId: Int, pokemonName: String) {
        viewModelScope.launch {
            withContext(dispatcher) { favouriteRepository.addFavourite(pokemonId, pokemonName) }
        }
    }

    private fun removeFavourite(pokemonId: Int) {
        viewModelScope.launch {
            withContext(dispatcher) { favouriteRepository.removeFavourite(pokemonId) }
        }
    }

    private sealed interface PokemonListLoadState {
        data object Loading : PokemonListLoadState
        data object Empty : PokemonListLoadState
        data class Success(
            val list: List<com.example.pokedex.data.model.PokemonListItem>,
            val query: String
        ) : PokemonListLoadState
        data class Failure(val message: String) : PokemonListLoadState
    }
}

sealed interface PokemonListEvent {
    data class Search(val query: String) : PokemonListEvent
    data object Retry : PokemonListEvent
    data object Refresh : PokemonListEvent
    data class AddFavourite(val pokemonId: Int, val pokemonName: String) : PokemonListEvent
    data class RemoveFavourite(val pokemonId: Int) : PokemonListEvent
}
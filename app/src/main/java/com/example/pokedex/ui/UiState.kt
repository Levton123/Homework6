package com.example.pokedex.ui

import com.example.pokedex.data.model.PokemonDetail
import com.example.pokedex.data.model.PokemonListItem

sealed interface PokemonListUiState {
    data object Loading : PokemonListUiState
    data class Success(
        val pokemonList: List<PokemonListItem>,
        val searchQuery: String = "",
        val favourites: Set<Int> = emptySet()
    ) : PokemonListUiState
    data class Error(val message: String) : PokemonListUiState
    data object Empty : PokemonListUiState
}

sealed interface PokemonDetailUiState {
    data object Loading : PokemonDetailUiState
    data class Success(
        val pokemon: PokemonDetail,
        val isFavourite: Boolean = false
    ) : PokemonDetailUiState
    data class Error(val message: String) : PokemonDetailUiState
}
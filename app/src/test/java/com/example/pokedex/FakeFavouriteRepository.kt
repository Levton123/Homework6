package com.example.pokedex

import com.example.pokedex.data.repository.FavouriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFavouriteRepository : FavouriteRepository {

    private val _favouriteIds = MutableStateFlow<List<Int>>(emptyList())
    override val favouriteIds: Flow<List<Int>> = _favouriteIds

    val addedFavourites = mutableListOf<Pair<Int, String>>()
    val removedFavourites = mutableListOf<Int>()

    override suspend fun addFavourite(pokemonId: Int, pokemonName: String) {
        addedFavourites.add(pokemonId to pokemonName)
        _favouriteIds.value = _favouriteIds.value + pokemonId
    }

    override suspend fun removeFavourite(pokemonId: Int) {
        removedFavourites.add(pokemonId)
        _favouriteIds.value = _favouriteIds.value - pokemonId
    }

    override suspend fun isFavourite(pokemonId: Int): Boolean =
        _favouriteIds.value.contains(pokemonId)

    fun setFavourites(ids: List<Int>) {
        _favouriteIds.value = ids
    }
}
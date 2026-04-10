package com.example.pokedex.data.repository

import kotlinx.coroutines.flow.Flow

interface FavouriteRepository {
    val favouriteIds: Flow<List<Int>>
    suspend fun addFavourite(pokemonId: Int, pokemonName: String)
    suspend fun removeFavourite(pokemonId: Int)
    suspend fun isFavourite(pokemonId: Int): Boolean
}
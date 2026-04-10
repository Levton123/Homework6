package com.example.pokedex.data.repository

import com.example.pokedex.data.db.FavouriteDao
import com.example.pokedex.data.db.FavouriteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouriteRepositoryImpl @Inject constructor(
    private val dao: FavouriteDao
) : FavouriteRepository {

    override val favouriteIds: Flow<List<Int>> = dao.getFavouriteIds()

    override suspend fun addFavourite(pokemonId: Int, pokemonName: String) {
        dao.addFavourite(FavouriteEntity(pokemonId = pokemonId, pokemonName = pokemonName))
    }

    override suspend fun removeFavourite(pokemonId: Int) {
        dao.removeFavourite(pokemonId)
    }

    override suspend fun isFavourite(pokemonId: Int): Boolean = dao.isFavourite(pokemonId)
}
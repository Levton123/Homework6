package com.example.pokedex.data.repository

import com.example.pokedex.data.api.PokemonApiService
import com.example.pokedex.data.model.PokemonDetail
import com.example.pokedex.data.model.PokemonListItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PokemonRepository @Inject constructor(
    private val apiService: PokemonApiService
) {
    private var cachedPokemonList: List<PokemonListItem>? = null
    private val cachedDetails = mutableMapOf<Int, PokemonDetail>()

    open suspend fun getPokemonList(limit: Int = 151, offset: Int = 0): Result<List<PokemonListItem>> {
        return try {
            val response = apiService.getPokemonList(limit, offset)
            cachedPokemonList = response.results
            Result.success(response.results)
        } catch (e: Exception) {
            cachedPokemonList?.let {
                Result.success(it)
            } ?: Result.failure(e)
        }
    }

    open suspend fun getPokemonDetail(id: Int): Result<PokemonDetail> {
        cachedDetails[id]?.let {
            return Result.success(it)
        }
        return try {
            val detail = apiService.getPokemonDetail(id)
            cachedDetails[id] = detail
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun searchPokemon(query: String): Result<List<PokemonListItem>> {
        val allPokemon = cachedPokemonList ?: run {
            val result = getPokemonList()
            result.getOrNull() ?: return Result.failure(
                result.exceptionOrNull() ?: Exception("Failed to load Pokemon list")
            )
        }
        val filtered = if (query.isBlank()) allPokemon
        else allPokemon.filter { it.name.contains(query, ignoreCase = true) }
        return Result.success(filtered)
    }
}
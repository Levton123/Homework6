package com.example.pokedex

import com.example.pokedex.data.api.PokemonApiService
import com.example.pokedex.data.model.OfficialArtwork
import com.example.pokedex.data.model.OtherSprites
import com.example.pokedex.data.model.PokemonAbility
import com.example.pokedex.data.model.PokemonAbilitySlot
import com.example.pokedex.data.model.PokemonDetail
import com.example.pokedex.data.model.PokemonListItem
import com.example.pokedex.data.model.PokemonListResponse
import com.example.pokedex.data.model.PokemonStat
import com.example.pokedex.data.model.PokemonSprites
import com.example.pokedex.data.model.PokemonType
import com.example.pokedex.data.model.PokemonTypeSlot
import com.example.pokedex.data.model.StatInfo
import com.example.pokedex.data.repository.PokemonRepository

class FakePokemonRepository : PokemonRepository(NoOpPokemonApiService()) {

    var pokemonListResult: Result<List<PokemonListItem>> = Result.success(defaultPokemonList())
    var pokemonDetailResult: Result<PokemonDetail> = Result.success(defaultPokemonDetail())
    var getPokemonListCallCount = 0

    override suspend fun getPokemonList(limit: Int, offset: Int): Result<List<PokemonListItem>> {
        getPokemonListCallCount++
        return pokemonListResult
    }

    override suspend fun getPokemonDetail(id: Int): Result<PokemonDetail> = pokemonDetailResult

    override suspend fun searchPokemon(query: String): Result<List<PokemonListItem>> = pokemonListResult

    companion object {
        fun defaultPokemonList() = listOf(
            PokemonListItem("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/"),
            PokemonListItem("ivysaur", "https://pokeapi.co/api/v2/pokemon/2/"),
            PokemonListItem("venusaur", "https://pokeapi.co/api/v2/pokemon/3/")
        )

        fun defaultPokemonDetail(id: Int = 1, name: String = "bulbasaur") = PokemonDetail(
            id = id,
            name = name,
            height = 7,
            weight = 69,
            sprites = PokemonSprites(
                frontDefault = "https://example.com/sprite.png",
                frontShiny = null,
                other = OtherSprites(
                    officialArtwork = OfficialArtwork(frontDefault = "https://example.com/artwork.png")
                )
            ),
            types = listOf(PokemonTypeSlot(slot = 1, type = PokemonType("grass", "url"))),
            abilities = listOf(
                PokemonAbilitySlot(slot = 1, ability = PokemonAbility("overgrow", "url"), isHidden = false)
            ),
            stats = listOf(PokemonStat(baseStat = 45, effort = 0, stat = StatInfo("hp", "url")))
        )
    }
}

class NoOpPokemonApiService : PokemonApiService {
    override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse =
        PokemonListResponse(count = 0, next = null, previous = null, results = emptyList())

    override suspend fun getPokemonDetail(id: Int): PokemonDetail =
        FakePokemonRepository.defaultPokemonDetail()
}
package com.example.pokedex

import com.example.pokedex.data.api.PokemonApiService
import com.example.pokedex.data.model.PokemonListResponse
import com.example.pokedex.data.repository.PokemonRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PokemonRepositoryTest {

    private lateinit var apiService: PokemonApiService
    private lateinit var repository: PokemonRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = PokemonRepository(apiService)
    }

    @Test
    fun `getPokemonList returns success with data from api`() = runTest {
        val items = FakePokemonRepository.defaultPokemonList()
        coEvery { apiService.getPokemonList(any(), any()) } returns PokemonListResponse(
            count = 3, next = null, previous = null, results = items
        )

        val result = repository.getPokemonList()

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun `getPokemonList returns failure when api throws`() = runTest {
        coEvery { apiService.getPokemonList(any(), any()) } throws Exception("Network error")

        val result = repository.getPokemonList()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getPokemonDetail returns cached result on second call`() = runTest {
        val detail = FakePokemonRepository.defaultPokemonDetail()
        coEvery { apiService.getPokemonDetail(1) } returns detail

        repository.getPokemonDetail(1)
        repository.getPokemonDetail(1)

        coVerify(exactly = 1) { apiService.getPokemonDetail(1) }
    }

    @Test
    fun `searchPokemon with blank query returns full list`() = runTest {
        val items = FakePokemonRepository.defaultPokemonList()
        coEvery { apiService.getPokemonList(any(), any()) } returns PokemonListResponse(
            count = 3, next = null, previous = null, results = items
        )
        repository.getPokemonList()

        val result = repository.searchPokemon("")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun `searchPokemon filters by name case insensitively`() = runTest {
        val items = FakePokemonRepository.defaultPokemonList()
        coEvery { apiService.getPokemonList(any(), any()) } returns PokemonListResponse(
            count = 3, next = null, previous = null, results = items
        )
        repository.getPokemonList()

        val result = repository.searchPokemon("BULBA")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("bulbasaur", result.getOrNull()?.first()?.name)
    }

    @Test
    fun `searchPokemon returns empty list when no match`() = runTest {
        val items = FakePokemonRepository.defaultPokemonList()
        coEvery { apiService.getPokemonList(any(), any()) } returns PokemonListResponse(
            count = 3, next = null, previous = null, results = items
        )
        repository.getPokemonList()

        val result = repository.searchPokemon("zzzzz")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `getPokemonList returns cached data when api fails`() = runTest {
        val items = FakePokemonRepository.defaultPokemonList()
        coEvery { apiService.getPokemonList(any(), any()) } returns PokemonListResponse(
            count = 3, next = null, previous = null, results = items
        )
        repository.getPokemonList()

        coEvery { apiService.getPokemonList(any(), any()) } throws Exception("No internet")
        val result = repository.getPokemonList()

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }
}
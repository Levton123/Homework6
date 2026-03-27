package com.example.pokedex

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.pokedex.ui.PokemonDetailEvent
import com.example.pokedex.ui.PokemonDetailUiState
import com.example.pokedex.ui.PokemonDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        pokemonId: Int = 1,
        repo: FakePokemonRepository = FakePokemonRepository(),
        favouriteRepo: FakeFavouriteRepository = FakeFavouriteRepository()
    ): PokemonDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("pokemonId" to pokemonId))
        return PokemonDetailViewModel(repo, favouriteRepo, savedStateHandle, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `uiState emits Loading then Success in sequence`() = runTest {
        val detail = FakePokemonRepository.defaultPokemonDetail(id = 1, name = "bulbasaur")
        val repo = FakePokemonRepository().apply { pokemonDetailResult = Result.success(detail) }
        val vm = createViewModel(repo = repo)

        val emissions = mutableListOf<PokemonDetailUiState>()
        vm.uiState.test {
            emissions.add(awaitItem())
            advanceUntilIdle()
            emissions.add(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue("Must have 2 emissions", emissions.size == 2)
        assertTrue("First must be Loading", emissions[0] is PokemonDetailUiState.Loading)
        assertTrue("Second must be Success", emissions[1] is PokemonDetailUiState.Success)
        assertEquals("bulbasaur", (emissions[1] as PokemonDetailUiState.Success).pokemon.name)
    }

    @Test
    fun `uiState emits Loading then Error in sequence`() = runTest {
        val repo = FakePokemonRepository().apply {
            pokemonDetailResult = Result.failure(Exception("Not found"))
        }
        val vm = createViewModel(repo = repo)

        val emissions = mutableListOf<PokemonDetailUiState>()
        vm.uiState.test {
            emissions.add(awaitItem())
            advanceUntilIdle()
            emissions.add(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue("Must have 2 emissions", emissions.size == 2)
        assertTrue("First must be Loading", emissions[0] is PokemonDetailUiState.Loading)
        assertTrue("Second must be Error", emissions[1] is PokemonDetailUiState.Error)
        assertEquals("Not found", (emissions[1] as PokemonDetailUiState.Error).message)
    }

    @Test
    fun `retry after error emits Loading then Error then Loading then Success`() = runTest {
        val repo = FakePokemonRepository().apply {
            pokemonDetailResult = Result.failure(Exception("Timeout"))
        }
        val vm = createViewModel(repo = repo)

        val emissions = mutableListOf<PokemonDetailUiState>()
        vm.uiState.test {
            emissions.add(awaitItem())
            advanceUntilIdle()
            emissions.add(awaitItem())

            repo.pokemonDetailResult = Result.success(FakePokemonRepository.defaultPokemonDetail())
            vm.onEvent(PokemonDetailEvent.Retry)

            emissions.add(awaitItem())
            advanceUntilIdle()
            emissions.add(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(4, emissions.size)
        assertTrue(emissions[0] is PokemonDetailUiState.Loading)
        assertTrue(emissions[1] is PokemonDetailUiState.Error)
        assertTrue(emissions[2] is PokemonDetailUiState.Loading)
        assertTrue(emissions[3] is PokemonDetailUiState.Success)
    }

    @Test
    fun `SavedStateHandle provides correct pokemonId`() = runTest {
        val detail = FakePokemonRepository.defaultPokemonDetail(id = 42, name = "mewtwo")
        val repo = FakePokemonRepository().apply { pokemonDetailResult = Result.success(detail) }
        val vm = createViewModel(pokemonId = 42, repo = repo)

        val emissions = mutableListOf<PokemonDetailUiState>()
        vm.uiState.test {
            emissions.add(awaitItem())
            advanceUntilIdle()
            emissions.add(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val success = emissions.last() as PokemonDetailUiState.Success
        assertEquals(42, success.pokemon.id)
        assertEquals("mewtwo", success.pokemon.name)
    }

    @Test
    fun `isFavourite reflects Room state reactively`() = runTest {
        val detail = FakePokemonRepository.defaultPokemonDetail(id = 1, name = "bulbasaur")
        val repo = FakePokemonRepository().apply { pokemonDetailResult = Result.success(detail) }
        val favouriteRepo = FakeFavouriteRepository()
        val vm = createViewModel(repo = repo, favouriteRepo = favouriteRepo)

        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            val afterLoad = awaitItem() as PokemonDetailUiState.Success
            assertFalse("Initially not favourite", afterLoad.isFavourite)

            favouriteRepo.addFavourite(1, "bulbasaur")
            val afterAdd = awaitItem() as PokemonDetailUiState.Success
            assertTrue("Should be favourite after add", afterAdd.isFavourite)

            favouriteRepo.removeFavourite(1)
            val afterRemove = awaitItem() as PokemonDetailUiState.Success
            assertFalse("Should not be favourite after remove", afterRemove.isFavourite)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavourite adds when not favourite`() = runTest {
        val detail = FakePokemonRepository.defaultPokemonDetail(id = 1, name = "bulbasaur")
        val repo = FakePokemonRepository().apply { pokemonDetailResult = Result.success(detail) }
        val favouriteRepo = FakeFavouriteRepository()
        val vm = createViewModel(repo = repo, favouriteRepo = favouriteRepo)

        advanceUntilIdle()
        vm.onEvent(PokemonDetailEvent.ToggleFavourite("bulbasaur"))
        advanceUntilIdle()

        assertTrue(favouriteRepo.addedFavourites.contains(1 to "bulbasaur"))
    }

    @Test
    fun `toggleFavourite removes when already favourite`() = runTest {
        val detail = FakePokemonRepository.defaultPokemonDetail(id = 1, name = "bulbasaur")
        val repo = FakePokemonRepository().apply { pokemonDetailResult = Result.success(detail) }
        val favouriteRepo = FakeFavouriteRepository().apply { setFavourites(listOf(1)) }
        val vm = createViewModel(repo = repo, favouriteRepo = favouriteRepo)

        advanceUntilIdle()
        vm.onEvent(PokemonDetailEvent.ToggleFavourite("bulbasaur"))
        advanceUntilIdle()

        assertTrue(favouriteRepo.removedFavourites.contains(1))
    }
}
package com.example.pokedex

import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.example.pokedex.data.model.PokemonListItem
import com.example.pokedex.ui.PokemonListEvent
import com.example.pokedex.ui.PokemonListUiState
import com.example.pokedex.ui.PokemonListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePokemonRepo: FakePokemonRepository
    private lateinit var fakeFavouriteRepo: FakeFavouriteRepository

    @Before
    fun setup() {
        fakePokemonRepo = FakePokemonRepository()
        fakeFavouriteRepo = FakeFavouriteRepository()
    }

    private fun createViewModel() = PokemonListViewModel(
        fakePokemonRepo,
        fakeFavouriteRepo,
        mainDispatcherRule.testDispatcher
    )

    @Test
    fun `uiState emits Loading then Success in sequence`() = runTest {
        fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            val loading = turbine.awaitItem()
            advanceUntilIdle()
            val success = turbine.awaitItem()
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("First must be Loading", loading is PokemonListUiState.Loading)
            assertTrue("Second must be Success", success is PokemonListUiState.Success)
            assertEquals(3, (success as PokemonListUiState.Success).pokemonList.size)
        }
    }

    @Test
    fun `uiState emits Loading then Error in sequence`() = runTest {
        fakePokemonRepo.pokemonListResult = Result.failure(Exception("Network error"))
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            val loading = turbine.awaitItem()
            advanceUntilIdle()
            val error = turbine.awaitItem()
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("First must be Loading", loading is PokemonListUiState.Loading)
            assertTrue("Second must be Error", error is PokemonListUiState.Error)
            assertEquals("Network error", (error as PokemonListUiState.Error).message)
        }
    }

    @Test
    fun `retry after error calls repository again and succeeds`() = runTest {
        fakePokemonRepo.pokemonListResult = Result.failure(Exception("Network error"))
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            turbine.awaitItem()
            advanceUntilIdle()
            turbine.awaitItem()

            val callsBefore = fakePokemonRepo.getPokemonListCallCount
            fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
            vm.onEvent(PokemonListEvent.Retry)
            advanceUntilIdle()

            val afterRetry = turbine.expectMostRecentItem()
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("After retry must be Success", afterRetry is PokemonListUiState.Success)
            assertTrue("Retry must call repo again", fakePokemonRepo.getPokemonListCallCount > callsBefore)
        }
    }

    @Test
    fun `empty list emits Loading then Empty not Success`() = runTest {
        fakePokemonRepo.pokemonListResult = Result.success(emptyList())
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            val loading = turbine.awaitItem()
            advanceUntilIdle()
            val empty = turbine.awaitItem()
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("First must be Loading", loading is PokemonListUiState.Loading)
            assertTrue("Second must be Empty not Success", empty is PokemonListUiState.Empty)
        }
    }

    @Test
    fun `empty search result emits Empty not Success with empty list`() = runTest {
        fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
        fakePokemonRepo.searchResult = Result.success(emptyList())
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            turbine.awaitItem()
            advanceUntilIdle()
            turbine.awaitItem()

            vm.onEvent(PokemonListEvent.Search("zzz"))
            advanceTimeBy(350)
            advanceUntilIdle()

            val searchResult = turbine.expectMostRecentItem()
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("Search result must be Empty", searchResult is PokemonListUiState.Empty)
        }
    }

    @Test
    fun `refresh after search resets to full list`() = runTest {
        val filteredList = listOf(PokemonListItem("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/"))
        fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
        fakePokemonRepo.searchResult = Result.success(filteredList)
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            turbine.awaitItem()
            advanceUntilIdle()
            turbine.awaitItem()

            vm.onEvent(PokemonListEvent.Search("bulba"))
            advanceTimeBy(350)
            advanceUntilIdle()

            val filtered = turbine.expectMostRecentItem()
            assertTrue(
                "Must be filtered Success with 1 item",
                filtered is PokemonListUiState.Success &&
                        (filtered as PokemonListUiState.Success).pokemonList.size == 1
            )

            fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
            vm.onEvent(PokemonListEvent.Refresh)
            advanceUntilIdle()

            val afterRefresh = turbine.expectMostRecentItem()
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("After refresh must be full list Success", afterRefresh is PokemonListUiState.Success)
            assertEquals(
                "After refresh must have 3 items",
                3, (afterRefresh as PokemonListUiState.Success).pokemonList.size
            )
        }
    }

    @Test
    fun `addFavourite appears in next uiState emission`() = runTest {
        fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            turbine.awaitItem()
            advanceUntilIdle()
            val initial = turbine.awaitItem() as PokemonListUiState.Success
            assertTrue("Initially no favourites", initial.favourites.isEmpty())

            vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
            advanceUntilIdle()
            val withFav = turbine.awaitItem() as PokemonListUiState.Success
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("Favourites must contain 1", withFav.favourites.contains(1))
        }
    }

    @Test
    fun `removeFavourite reflects in next uiState emission`() = runTest {
        fakeFavouriteRepo.setFavourites(listOf(1, 2))
        fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
        val vm = createViewModel()

        turbineScope {
            val turbine = vm.uiState.testIn(backgroundScope)
            turbine.awaitItem()
            advanceUntilIdle()
            val initial = turbine.awaitItem() as PokemonListUiState.Success
            assertTrue("Initially has 1 and 2", initial.favourites.containsAll(listOf(1, 2)))

            vm.onEvent(PokemonListEvent.RemoveFavourite(1))
            advanceUntilIdle()
            val afterRemove = turbine.awaitItem() as PokemonListUiState.Success
            turbine.cancelAndIgnoreRemainingEvents()

            assertTrue("After remove must not contain 1", !afterRemove.favourites.contains(1))
            assertTrue("After remove must still contain 2", afterRemove.favourites.contains(2))
        }
    }
}
package com.example.pokedex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokedex.data.db.PokedexDatabase
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.ui.PokemonListEvent
import com.example.pokedex.ui.PokemonListUiState
import com.example.pokedex.ui.PokemonListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PokemonListViewModelIntegrationTest {

    private lateinit var db: PokedexDatabase
    private lateinit var favouriteRepository: FavouriteRepository
    private lateinit var fakePokemonRepo: FakePokemonRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favouriteRepository = FavouriteRepository(db.favouriteDao())
        fakePokemonRepo = FakePokemonRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun addFavourite_persists_inRoom() = runTest {
        val vm = PokemonListViewModel(fakePokemonRepo, favouriteRepository, testDispatcher)
        advanceUntilIdle()

        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()

        val ids = db.favouriteDao().getFavouriteIds().first()
        assertTrue("Pokemon 1 should be in favourites", ids.contains(1))
    }

    @Test
    fun removeFavourite_removesFromRoom() = runTest {
        val vm = PokemonListViewModel(fakePokemonRepo, favouriteRepository, testDispatcher)
        advanceUntilIdle()

        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()
        val idsAfterAdd = db.favouriteDao().getFavouriteIds().first()
        assertTrue("Should contain 1 after add", idsAfterAdd.contains(1))

        vm.onEvent(PokemonListEvent.RemoveFavourite(1))
        advanceUntilIdle()

        val idsAfterRemove = db.favouriteDao().getFavouriteIds().first()
        assertFalse("Should not contain 1 after remove", idsAfterRemove.contains(1))
    }

    @Test
    fun addSameFavouriteTwice_doesNotDuplicateInRoom() = runTest {
        val vm = PokemonListViewModel(fakePokemonRepo, favouriteRepository, testDispatcher)
        advanceUntilIdle()

        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()
        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()

        val ids = db.favouriteDao().getFavouriteIds().first()
        assertEquals(1, ids.size)
    }

    @Test
    fun errorState_thenRetry_transitionsToSuccess() = runTest {
        fakePokemonRepo.pokemonListResult = Result.failure(Exception("Timeout"))
        val vm = PokemonListViewModel(fakePokemonRepo, favouriteRepository, testDispatcher)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is PokemonListUiState.Error)

        fakePokemonRepo.pokemonListResult = Result.success(FakePokemonRepository.defaultPokemonList())
        vm.onEvent(PokemonListEvent.Retry)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is PokemonListUiState.Success)
    }
}
package com.example.pokedex

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokedex.data.db.FavouriteDao
import com.example.pokedex.data.db.PokedexDatabase
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.data.repository.FavouriteRepositoryImpl
import com.example.pokedex.ui.PokemonListEvent
import com.example.pokedex.ui.PokemonListUiState
import com.example.pokedex.ui.PokemonListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    private lateinit var dao: FavouriteDao
    private lateinit var favouriteRepository: FavouriteRepository
    private lateinit var fakePokemonRepo: FakePokemonRepository
    private var vm: PokemonListViewModel? = null
    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        scheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favouriteDao()
        favouriteRepository = FavouriteRepositoryImpl(dao)
        fakePokemonRepo = FakePokemonRepository()
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        vm = null
        Dispatchers.resetMain()
        if (db.isOpen) db.close()
    }

    private fun createVm(): PokemonListViewModel =
        PokemonListViewModel(fakePokemonRepo, favouriteRepository, testDispatcher)
            .also { vm = it }

    private fun getFavouriteIds(): List<Int> = runBlocking {
        dao.getFavouriteIds().first()
    }

    @Test
    fun addFavourite_persists_inRoom() = runTest(testDispatcher) {
        val vm = createVm()
        advanceTimeBy(301L)
        advanceUntilIdle()

        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()

        assertTrue("Pokemon 1 should be in favourites", getFavouriteIds().contains(1))
    }

    @Test
    fun removeFavourite_removesFromRoom() = runTest(testDispatcher) {
        val vm = createVm()
        advanceTimeBy(301L)
        advanceUntilIdle()

        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()
        assertTrue("Should contain 1 after add", getFavouriteIds().contains(1))

        vm.onEvent(PokemonListEvent.RemoveFavourite(1))
        advanceUntilIdle()

        assertFalse("Should not contain 1 after remove", getFavouriteIds().contains(1))
    }

    @Test
    fun addSameFavouriteTwice_doesNotDuplicateInRoom() = runTest(testDispatcher) {
        val vm = createVm()
        advanceTimeBy(301L)
        advanceUntilIdle()

        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()
        vm.onEvent(PokemonListEvent.AddFavourite(1, "bulbasaur"))
        advanceUntilIdle()

        assertEquals(1, getFavouriteIds().size)
    }

    @Test
    fun errorState_thenRetry_transitionsToSuccess() = runTest(testDispatcher) {
        fakePokemonRepo.pokemonListResult = Result.failure(Exception("Timeout"))
        val vm = createVm()
        advanceTimeBy(301L)
        advanceUntilIdle()

        assertTrue(
            "Should be Error after failed load",
            vm.uiState.value is PokemonListUiState.Error
        )

        fakePokemonRepo.pokemonListResult =
            Result.success(FakePokemonRepository.defaultPokemonList())
        vm.onEvent(PokemonListEvent.Retry)
        advanceUntilIdle()

        assertTrue(
            "Should be Success after retry",
            vm.uiState.value is PokemonListUiState.Success
        )
    }
}
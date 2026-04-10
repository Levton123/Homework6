package com.example.pokedex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.pokedex.data.db.PokedexDatabase
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.data.repository.FavouriteRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavouriteRepositoryIntegrationTest {

    private lateinit var db: PokedexDatabase
    private lateinit var repository: FavouriteRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FavouriteRepositoryImpl(db.favouriteDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addFavourite_thenReadFromFlow_returnsCorrectData() = runTest {
        repository.addFavourite(1, "bulbasaur")

        repository.favouriteIds.test {
            val ids = awaitItem()
            assertTrue(ids.contains(1))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addAndRemoveFavourite_flowReflectsChanges() = runTest {
        repository.favouriteIds.test {
            awaitItem()

            repository.addFavourite(1, "bulbasaur")
            val afterAdd = awaitItem()
            assertTrue(afterAdd.contains(1))

            repository.removeFavourite(1)
            val afterRemove = awaitItem()
            assertFalse(afterRemove.contains(1))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addDuplicateFavourite_doesNotCreateDuplicate() = runTest {
        repository.addFavourite(1, "bulbasaur")
        repository.addFavourite(1, "bulbasaur")

        repository.favouriteIds.test {
            val ids = awaitItem()
            assertEquals(1, ids.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isFavourite_correctlyReflectsRoomState() = runTest {
        assertFalse(repository.isFavourite(1))

        repository.addFavourite(1, "bulbasaur")
        assertTrue(repository.isFavourite(1))

        repository.removeFavourite(1)
        assertFalse(repository.isFavourite(1))
    }
}
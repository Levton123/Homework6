package com.example.pokedex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.pokedex.data.db.FavouriteDao
import com.example.pokedex.data.db.FavouriteEntity
import com.example.pokedex.data.db.PokedexDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavouriteDaoTest {

    private lateinit var db: PokedexDatabase
    private lateinit var dao: FavouriteDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favouriteDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun addFavourite_thenGetFavouriteIds_returnsCorrectId() = runTest {
        dao.addFavourite(FavouriteEntity(pokemonId = 1, pokemonName = "bulbasaur"))

        dao.getFavouriteIds().test {
            val ids = awaitItem()
            assertTrue(ids.contains(1))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeFavourite_thenGetFavouriteIds_doesNotContainRemovedId() = runTest {
        dao.addFavourite(FavouriteEntity(pokemonId = 1, pokemonName = "bulbasaur"))
        dao.addFavourite(FavouriteEntity(pokemonId = 2, pokemonName = "ivysaur"))

        dao.removeFavourite(1)

        dao.getFavouriteIds().test {
            val ids = awaitItem()
            assertFalse(ids.contains(1))
            assertTrue(ids.contains(2))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addSameFavouriteTwice_doesNotCreateDuplicate() = runTest {
        dao.addFavourite(FavouriteEntity(pokemonId = 1, pokemonName = "bulbasaur"))
        dao.addFavourite(FavouriteEntity(pokemonId = 1, pokemonName = "bulbasaur"))

        dao.getFavouriteIds().test {
            val ids = awaitItem()
            assertEquals(1, ids.count { it == 1 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isFavourite_returnsTrueAfterAdd() = runTest {
        dao.addFavourite(FavouriteEntity(pokemonId = 25, pokemonName = "pikachu"))

        assertTrue(dao.isFavourite(25))
    }

    @Test
    fun isFavourite_returnsFalseForNonExistentId() = runTest {
        assertFalse(dao.isFavourite(999))
    }

    @Test
    fun isFavourite_returnsFalseAfterRemove() = runTest {
        dao.addFavourite(FavouriteEntity(pokemonId = 1, pokemonName = "bulbasaur"))
        dao.removeFavourite(1)

        assertFalse(dao.isFavourite(1))
    }

    @Test
    fun getFavouriteIds_emitsNewListAfterAdd() = runTest {
        dao.getFavouriteIds().test {
            val empty = awaitItem()
            assertTrue(empty.isEmpty())

            dao.addFavourite(FavouriteEntity(pokemonId = 1, pokemonName = "bulbasaur"))
            val withOne = awaitItem()
            assertTrue(withOne.contains(1))

            dao.addFavourite(FavouriteEntity(pokemonId = 2, pokemonName = "ivysaur"))
            val withTwo = awaitItem()
            assertEquals(2, withTwo.size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
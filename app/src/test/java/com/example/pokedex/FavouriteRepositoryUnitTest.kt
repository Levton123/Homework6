package com.example.pokedex

import app.cash.turbine.test
import com.example.pokedex.data.db.FavouriteDao
import com.example.pokedex.data.db.FavouriteEntity
import com.example.pokedex.data.repository.FavouriteRepository
import com.example.pokedex.data.repository.FavouriteRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavouriteRepositoryUnitTest {

    private lateinit var dao: FavouriteDao
    private lateinit var repository: FavouriteRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = FavouriteRepositoryImpl(dao)
    }

    @Test
    fun `favouriteIds emits list from dao`() = runTest {
        every { dao.getFavouriteIds() } returns flowOf(listOf(1, 2, 3))
        val repo = FavouriteRepositoryImpl(dao)

        repo.favouriteIds.test {
            assertEquals(listOf(1, 2, 3), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addFavourite calls dao with correct entity`() = runTest {
        repository.addFavourite(42, "mewtwo")

        coVerify {
            dao.addFavourite(match { it.pokemonId == 42 && it.pokemonName == "mewtwo" })
        }
    }

    @Test
    fun `removeFavourite calls dao with correct id`() = runTest {
        repository.removeFavourite(7)

        coVerify { dao.removeFavourite(7) }
    }

    @Test
    fun `isFavourite returns true when dao returns true`() = runTest {
        coEvery { dao.isFavourite(1) } returns true

        assertTrue(repository.isFavourite(1))
    }

    @Test
    fun `isFavourite returns false when dao returns false`() = runTest {
        coEvery { dao.isFavourite(99) } returns false

        assertFalse(repository.isFavourite(99))
    }
}
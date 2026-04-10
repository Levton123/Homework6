package com.example.pokedex

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokedex.ui.PokemonListEvent
import com.example.pokedex.ui.PokemonListScreen
import com.example.pokedex.ui.PokemonListUiState
import com.example.pokedex.data.model.PokemonListItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PokemonListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeRule.setContent {
            PokemonListScreen(
                uiState = PokemonListUiState.Loading,
                onEvent = {},
                onPokemonClick = {},
                onFavouritesClick = {}
            )
        }

        composeRule.onNodeWithText("Loading Pokemon...").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessageAndRetryButton() {
        composeRule.setContent {
            PokemonListScreen(
                uiState = PokemonListUiState.Error("Network error"),
                onEvent = {},
                onPokemonClick = {},
                onFavouritesClick = {}
            )
        }

        composeRule.onNodeWithText("Error: Network error").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_triggersRetryEvent() {
        var retryClicked = false
        composeRule.setContent {
            PokemonListScreen(
                uiState = PokemonListUiState.Error("Network error"),
                onEvent = { event ->
                    if (event is PokemonListEvent.Retry) retryClicked = true
                },
                onPokemonClick = {},
                onFavouritesClick = {}
            )
        }

        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun successState_showsPokemonList() {
        val pokemon = listOf(
            PokemonListItem("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/"),
            PokemonListItem("ivysaur", "https://pokeapi.co/api/v2/pokemon/2/")
        )
        composeRule.setContent {
            PokemonListScreen(
                uiState = PokemonListUiState.Success(pokemonList = pokemon),
                onEvent = {},
                onPokemonClick = {},
                onFavouritesClick = {}
            )
        }

        composeRule.onNodeWithText("Bulbasaur").assertIsDisplayed()
        composeRule.onNodeWithText("Ivysaur").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        composeRule.setContent {
            PokemonListScreen(
                uiState = PokemonListUiState.Empty,
                onEvent = {},
                onPokemonClick = {},
                onFavouritesClick = {}
            )
        }

        composeRule.onNodeWithText("No Pokemon found").assertIsDisplayed()
    }

    @Test
    fun successState_pokemonClick_triggersNavigation() {
        var clickedId = -1
        val pokemon = listOf(
            PokemonListItem("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/")
        )
        composeRule.setContent {
            PokemonListScreen(
                uiState = PokemonListUiState.Success(pokemonList = pokemon),
                onEvent = {},
                onPokemonClick = { id -> clickedId = id },
                onFavouritesClick = {}
            )
        }

        composeRule.onNodeWithText("Bulbasaur").performClick()
        assertEquals(1, clickedId)
    }
}

private fun assertTrue(condition: Boolean) {
    org.junit.Assert.assertTrue(condition)
}

private fun assertEquals(expected: Int, actual: Int) {
    org.junit.Assert.assertEquals(expected.toLong(), actual.toLong())
}
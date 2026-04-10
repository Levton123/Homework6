package com.example.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    data object PokemonList : Screen("pokemon_list")
    data object PokemonDetail : Screen("pokemon_detail/{pokemonId}") {
        fun createRoute(pokemonId: Int) = "pokemon_detail/$pokemonId"
    }
    data object Favourites : Screen("favourites")
}

@Composable
fun PokemonNavigation(
    navController: NavHostController = rememberNavController()
) {
    val sharedListViewModel: PokemonListViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.PokemonList.route
    ) {
        composable(Screen.PokemonList.route) {
            val uiState by sharedListViewModel.uiState.collectAsState()
            PokemonListScreen(
                uiState = uiState,
                onEvent = sharedListViewModel::onEvent,
                onPokemonClick = { navController.navigate(Screen.PokemonDetail.createRoute(it)) },
                onFavouritesClick = { navController.navigate(Screen.Favourites.route) }
            )
        }

        composable(
            route = Screen.PokemonDetail.route,
            arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
        ) {
            val detailViewModel: PokemonDetailViewModel = hiltViewModel()
            val detailUiState by detailViewModel.uiState.collectAsState()

            PokemonDetailScreen(
                uiState = detailUiState,
                onEvent = detailViewModel::onEvent,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(Screen.Favourites.route) {
            val favourites by sharedListViewModel.favourites.collectAsState()
            FavouritesScreen(
                favouriteIds = favourites,
                onPokemonClick = { navController.navigate(Screen.PokemonDetail.createRoute(it)) },
                onRemoveFavourite = { sharedListViewModel.onEvent(PokemonListEvent.RemoveFavourite(it)) },
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}
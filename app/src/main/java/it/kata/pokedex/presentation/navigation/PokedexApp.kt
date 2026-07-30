package it.kata.pokedex.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.kata.pokedex.presentation.favorites.FavoritesRoute
import it.kata.pokedex.presentation.list.PokemonListRoute

/** The current tab is read back from the graph, so there is one source of truth about where we are. */
@Composable
fun PokedexApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val current = PokedexDestination.entries
        .firstOrNull { it.route == currentRoute?.destination?.route }
        ?: PokedexDestination.LIST

    Scaffold(
        bottomBar = {
            PokedexBottomBar(
                current = current,
                onSelect = navController::switchTab,
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PokedexDestination.LIST.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(PokedexDestination.LIST.route) { PokemonListRoute() }
            composable(PokedexDestination.FAVORITES.route) { FavoritesRoute() }
        }
    }
}

/**
 * `saveState` and `restoreState` are the reason this is not a plain `navigate`: without them, coming
 * back to the list would rebuild it from the top and lose where the user had scrolled to.
 */
private fun NavHostController.switchTab(destination: PokedexDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

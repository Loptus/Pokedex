package it.kata.pokedex.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems

/**
 * The stateful half of the screen: it collects the state and hands plain values to the composable
 * below, which stays testable and previewable on its own.
 */
@Composable
fun PokemonListRoute(
    modifier: Modifier = Modifier,
    viewModel: PokemonListViewModel = hiltViewModel(),
) {
    PokemonListScreen(
        pokemon = viewModel.pokemon.collectAsLazyPagingItems(),
        modifier = modifier,
    )
}

package it.kata.pokedex.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PokemonListScreen(
        pokemon = viewModel.pokemon.collectAsLazyPagingItems(),
        query = uiState.query,
        onQueryChange = viewModel::onQueryChange,
        modifier = modifier,
    )
}

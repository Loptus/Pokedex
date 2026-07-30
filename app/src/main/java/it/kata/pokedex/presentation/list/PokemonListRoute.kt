package it.kata.pokedex.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems

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
        selectedTypes = uiState.selectedTypes,
        onTypeToggle = viewModel::onTypeToggle,
        onFavoriteToggle = viewModel::onFavoriteToggle,
        // Composed per row: the LaunchedEffect is what ties a row's request to that row being on
        // screen, and cancels it when the row leaves. See PokemonRowLoader.
        rowFor = { ref ->
            LaunchedEffect(ref.id) { viewModel.loadRow(ref) }
            viewModel.rowState(ref.id).collectAsStateWithLifecycle().value
        },
        modifier = modifier,
    )
}

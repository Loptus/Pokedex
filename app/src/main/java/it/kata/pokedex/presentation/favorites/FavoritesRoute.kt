package it.kata.pokedex.presentation.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FavoritesRoute(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    FavoritesScreen(
        favorites = favorites,
        onRemove = viewModel::onRemove,
        // Composed per row: the LaunchedEffect is what ties a row's request to that row being on
        // screen, and cancels it when the row leaves. See PokemonRowLoader.
        rowFor = { ref ->
            LaunchedEffect(ref.id) { viewModel.loadRow(ref) }
            viewModel.rowState(ref.id).collectAsStateWithLifecycle().value
        },
        modifier = modifier,
    )
}

package it.kata.pokedex.presentation.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful half of the screen: it collects the state and hands plain values to the composable
 * below, which stays testable and previewable on its own.
 */
@Composable
fun FavoritesRoute(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    FavoritesScreen(
        favorites = favorites,
        onRemove = viewModel::onRemove,
        // Composed per row, which is what ties a row's request to a row being on screen. The
        // LaunchedEffect is the cancellation: Compose tears it down when the row leaves, and the
        // two requests underneath go with it.
        rowFor = { ref ->
            LaunchedEffect(ref.id) { viewModel.loadRow(ref) }
            viewModel.rowState(ref.id).collectAsStateWithLifecycle().value
        },
        modifier = modifier,
    )
}

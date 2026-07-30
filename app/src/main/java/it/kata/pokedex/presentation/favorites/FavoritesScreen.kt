package it.kata.pokedex.presentation.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.kata.pokedex.R
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.presentation.common.PokemonRowState
import it.kata.pokedex.presentation.common.components.EmptyState
import it.kata.pokedex.presentation.common.components.PokemonRow
import it.kata.pokedex.presentation.common.components.RowDivider
import it.kata.pokedex.presentation.common.previewLoaded
import it.kata.pokedex.presentation.common.previewRefs
import it.kata.pokedex.presentation.theme.PokedexTheme

/**
 * The saved entries, in Pokedex order, drawn with the same row as the list.
 *
 * Every row here is a favorite by definition, so its heart is filled and the only thing it can do is
 * remove. The rows arrive as pointers and fill in one at a time exactly like in the list: what was
 * saved is which entry, not what it looked like.
 *
 * [favorites] being null means the database has not answered yet, and nothing is drawn for that one
 * frame. Showing the empty state instead would tell someone with twenty favorites that they have
 * none, and then take it back.
 */
@Composable
fun FavoritesScreen(
    favorites: List<PokemonRef>?,
    onRemove: (Int) -> Unit,
    rowFor: @Composable (PokemonRef) -> PokemonRowState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FavoritesHeader()

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                favorites == null -> Unit

                favorites.isEmpty() -> EmptyState(
                    title = stringResource(R.string.favorites_empty_title),
                    body = stringResource(R.string.favorites_empty_body),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = favorites.size, key = { favorites[it].id }) { index ->
                        val ref = favorites[index]

                        PokemonRow(
                            ref = ref,
                            state = rowFor(ref),
                            isFavorite = true,
                            onFavoriteToggle = { onRemove(ref.id) },
                        )
                        RowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.title_favorites),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun FavoritesLoadedPreview() {
    PokedexTheme {
        FavoritesScreen(
            favorites = previewRefs,
            onRemove = {},
            rowFor = { previewLoaded(it) },
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun FavoritesRowsLoadingPreview() {
    PokedexTheme {
        FavoritesScreen(
            favorites = previewRefs,
            onRemove = {},
            rowFor = { PokemonRowState.Loading },
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun FavoritesEmptyPreview() {
    PokedexTheme {
        FavoritesScreen(
            favorites = emptyList(),
            onRemove = {},
            rowFor = { PokemonRowState.Loading },
        )
    }
}

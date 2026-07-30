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

/** Every row here is saved by definition, so its heart is filled and can only remove. */
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

package it.kata.pokedex.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.presentation.list.components.ListAppendError
import it.kata.pokedex.presentation.list.components.ListAppendLoading
import it.kata.pokedex.presentation.list.components.ListErrorState
import it.kata.pokedex.presentation.list.components.PokedexHeader
import it.kata.pokedex.presentation.list.components.PokemonRow
import it.kata.pokedex.presentation.list.components.PokemonRowPlaceholder
import it.kata.pokedex.presentation.theme.PokedexTheme
import java.io.IOException

/** How many skeleton rows to draw while the first page is on its way. */
private const val PLACEHOLDER_ROWS = 8

/**
 * The list screen. Nothing more than an adapter: it unpacks [LazyPagingItems] into plain values and
 * hands them to the layout below.
 *
 * The split is what makes the layout previewable. Feeding a preview a `LazyPagingItems` is a race
 * against the coroutine that collects the flow, so it renders the skeleton about half the time;
 * plain values render the same way every time.
 */
@Composable
fun PokemonListScreen(
    pokemon: LazyPagingItems<Pokemon>,
    modifier: Modifier = Modifier,
) {
    PokemonListContent(
        refresh = pokemon.loadState.refresh,
        append = pokemon.loadState.append,
        itemCount = pokemon.itemCount,
        keyOf = pokemon.itemKey { it.id },
        itemAt = { pokemon[it] },
        onRetry = pokemon::retry,
        modifier = modifier,
    )
}

/**
 * The layout, with no idea that Paging exists.
 *
 * The two failures are told apart on purpose. Losing the first page leaves an empty screen, so it
 * gets the full error state; losing a later page leaves a usable list, so the retry goes quietly at
 * the bottom.
 */
@Composable
private fun PokemonListContent(
    refresh: LoadState,
    append: LoadState,
    itemCount: Int,
    keyOf: (Int) -> Any,
    itemAt: (Int) -> Pokemon?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PokedexHeader()

        Box(modifier = Modifier.fillMaxSize()) {
            when (refresh) {
                is LoadState.Loading -> LoadingRows()

                is LoadState.Error -> ListErrorState(onRetry = onRetry)

                is LoadState.NotLoading -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = itemCount, key = keyOf) { index ->
                        itemAt(index)?.let { pokemon ->
                            PokemonRow(pokemon = pokemon)
                            RowDivider()
                        }
                    }

                    when (append) {
                        is LoadState.Loading -> item { ListAppendLoading() }
                        is LoadState.Error -> item { ListAppendError(onRetry = onRetry) }
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRows() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(PLACEHOLDER_ROWS) {
            PokemonRowPlaceholder()
            RowDivider()
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListLoadedPreview() {
    PokedexTheme {
        PokemonListContent(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            itemCount = previewPokemon.size,
            keyOf = { previewPokemon[it].id },
            itemAt = { previewPokemon[it] },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListLoadingPreview() {
    PokedexTheme {
        PokemonListContent(
            refresh = LoadState.Loading,
            append = LoadState.NotLoading(endOfPaginationReached = false),
            itemCount = 0,
            keyOf = { it },
            itemAt = { null },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListErrorPreview() {
    PokedexTheme {
        PokemonListContent(
            refresh = LoadState.Error(IOException("preview")),
            append = LoadState.NotLoading(endOfPaginationReached = false),
            itemCount = 0,
            keyOf = { it },
            itemAt = { null },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListAppendingPreview() {
    PokedexTheme {
        PokemonListContent(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Loading,
            itemCount = previewPokemon.size,
            keyOf = { previewPokemon[it].id },
            itemAt = { previewPokemon[it] },
            onRetry = {},
        )
    }
}

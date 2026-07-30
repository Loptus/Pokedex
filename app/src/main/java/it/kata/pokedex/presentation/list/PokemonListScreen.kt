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
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.presentation.list.components.ListAppendError
import it.kata.pokedex.presentation.list.components.ListAppendLoading
import it.kata.pokedex.presentation.list.components.ListEmptyState
import it.kata.pokedex.presentation.list.components.ListErrorState
import it.kata.pokedex.presentation.list.components.PokedexHeader
import it.kata.pokedex.presentation.list.components.PokedexSearchField
import it.kata.pokedex.presentation.list.components.PokemonRow
import it.kata.pokedex.presentation.list.components.PokemonRowPlaceholder
import it.kata.pokedex.presentation.theme.PokedexTheme
import java.io.IOException

/** How many skeleton rows to draw while the first page is on its way. */
private const val PLACEHOLDER_ROWS = 8

/**
 * The list screen. Nothing more than an adapter: it unpacks [LazyPagingItems] into plain values and
 * hands them to the layout below, which is what makes that layout previewable and testable.
 *
 * [PokemonListContent] takes one immutable snapshot rather than a count plus an accessor, and that
 * matters: narrowing the search shrinks the list, and if the number of rows came from one snapshot
 * while the keys were read from a newer one, Compose would rebuild its key map over indices that no
 * longer exist. Taking both from the same list makes the mismatch impossible to express.
 */
@Composable
fun PokemonListScreen(
    pokemon: LazyPagingItems<PokemonRef>,
    query: String,
    onQueryChange: (String) -> Unit,
    rowFor: @Composable (PokemonRef) -> PokemonRowState,
    modifier: Modifier = Modifier,
) {
    PokemonListContent(
        rows = pokemon.itemSnapshotList.items,
        // Reading through LazyPagingItems is what tells Paging how far down the list the user has
        // got, and is therefore what loads the next page. The bounds check covers the frame where
        // the rows above are still one snapshot behind the live list.
        onRowReached = { index -> if (index < pokemon.itemCount) pokemon[index] },
        refresh = pokemon.loadState.refresh,
        append = pokemon.loadState.append,
        query = query,
        onQueryChange = onQueryChange,
        rowFor = rowFor,
        onRetry = pokemon::retry,
        modifier = modifier,
    )
}

/**
 * The layout, with no idea that Paging exists.
 *
 * Header and search field stay above every state: an empty result has to leave the user somewhere
 * to type, and an error has to leave them a way to change the query rather than only retry.
 *
 * The two failures are told apart on purpose. Losing the first page leaves an empty screen, so it
 * gets the full error state; losing a later page leaves a usable list, so the retry goes quietly at
 * the bottom.
 */
@Composable
private fun PokemonListContent(
    rows: List<PokemonRef>,
    onRowReached: (Int) -> Unit,
    refresh: LoadState,
    append: LoadState,
    query: String,
    onQueryChange: (String) -> Unit,
    rowFor: @Composable (PokemonRef) -> PokemonRowState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PokedexHeader()
        PokedexSearchField(query = query, onQueryChange = onQueryChange)

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                refresh is LoadState.Loading -> LoadingRows()

                refresh is LoadState.Error -> ListErrorState(onRetry = onRetry)

                rows.isEmpty() -> ListEmptyState()

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = rows.size, key = { rows[it].id }) { index ->
                        onRowReached(index)

                        val ref = rows[index]
                        PokemonRow(ref = ref, state = rowFor(ref))
                        RowDivider()
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
            rows = previewRefs,
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            query = "",
            onQueryChange = {},
            rowFor = { previewLoaded(it) },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListRowsLoadingPreview() {
    PokedexTheme {
        PokemonListContent(
            rows = previewRefs,
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            query = "",
            onQueryChange = {},
            rowFor = { PokemonRowState.Loading },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListLoadingPreview() {
    PokedexTheme {
        PokemonListContent(
            rows = emptyList(),
            onRowReached = {},
            refresh = LoadState.Loading,
            append = LoadState.NotLoading(endOfPaginationReached = false),
            query = "",
            onQueryChange = {},
            rowFor = { PokemonRowState.Loading },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListEmptyPreview() {
    PokedexTheme {
        PokemonListContent(
            rows = emptyList(),
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            query = "zzz",
            onQueryChange = {},
            rowFor = { PokemonRowState.Loading },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListErrorPreview() {
    PokedexTheme {
        PokemonListContent(
            rows = emptyList(),
            onRowReached = {},
            refresh = LoadState.Error(IOException("preview")),
            append = LoadState.NotLoading(endOfPaginationReached = false),
            query = "",
            onQueryChange = {},
            rowFor = { PokemonRowState.Loading },
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListAppendingPreview() {
    PokedexTheme {
        PokemonListContent(
            rows = previewRefs,
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Loading,
            query = "",
            onQueryChange = {},
            rowFor = { previewLoaded(it) },
            onRetry = {},
        )
    }
}

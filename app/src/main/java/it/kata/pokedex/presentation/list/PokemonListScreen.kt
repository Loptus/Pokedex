package it.kata.pokedex.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import it.kata.pokedex.R
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType
import it.kata.pokedex.presentation.common.PokemonRowState
import it.kata.pokedex.presentation.common.components.EmptyState
import it.kata.pokedex.presentation.common.components.PokemonRow
import it.kata.pokedex.presentation.common.components.PokemonRowPlaceholder
import it.kata.pokedex.presentation.common.components.RowDivider
import it.kata.pokedex.presentation.common.previewLoaded
import it.kata.pokedex.presentation.list.components.ListAppendError
import it.kata.pokedex.presentation.list.components.ListAppendLoading
import it.kata.pokedex.presentation.list.components.ListErrorState
import it.kata.pokedex.presentation.list.components.PokedexHeader
import it.kata.pokedex.presentation.list.components.PokedexSearchField
import it.kata.pokedex.presentation.list.components.TypeFilterRow
import it.kata.pokedex.presentation.theme.PokedexTheme
import java.io.IOException

/** How many skeleton rows to draw while the first page is on its way. */
private const val PLACEHOLDER_ROWS = 8

/**
 * [PokemonListContent] takes one immutable snapshot rather than a count plus an accessor: narrowing
 * the search shrinks the list, and a count from one snapshot with keys from a newer one had Compose
 * rebuilding its key map over indices that no longer exist.
 */
@Composable
fun PokemonListScreen(
    pokemon: LazyPagingItems<PokemonListItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedTypes: Set<PokemonType>,
    onTypeToggle: (PokemonType) -> Unit,
    onFavoriteToggle: (PokemonRef) -> Unit,
    rowFor: @Composable (PokemonRef) -> PokemonRowState,
    modifier: Modifier = Modifier,
) {
    PokemonListContent(
        rows = pokemon.itemSnapshotList.items,
        // Reading through LazyPagingItems is what tells Paging how far the user has got, and
        // therefore what loads the next page. The bounds check covers the frame where the rows
        // above are still one snapshot behind.
        onRowReached = { index -> if (index < pokemon.itemCount) pokemon[index] },
        refresh = pokemon.loadState.refresh,
        append = pokemon.loadState.append,
        query = query,
        onQueryChange = onQueryChange,
        selectedTypes = selectedTypes,
        onTypeToggle = onTypeToggle,
        onFavoriteToggle = onFavoriteToggle,
        rowFor = rowFor,
        onRetry = pokemon::retry,
        modifier = modifier,
    )
}

/**
 * Header and search field stay above every state, so an empty result or an error still leaves the
 * user somewhere to type.
 */
@Composable
private fun PokemonListContent(
    rows: List<PokemonListItem>,
    onRowReached: (Int) -> Unit,
    refresh: LoadState,
    append: LoadState,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedTypes: Set<PokemonType>,
    onTypeToggle: (PokemonType) -> Unit,
    onFavoriteToggle: (PokemonRef) -> Unit,
    rowFor: @Composable (PokemonRef) -> PokemonRowState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PokedexHeader()
        PokedexSearchField(query = query, onQueryChange = onQueryChange)
        TypeFilterRow(selectedTypes = selectedTypes, onTypeToggle = onTypeToggle)

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                refresh is LoadState.Loading -> LoadingRows()

                refresh is LoadState.Error -> ListErrorState(onRetry = onRetry)

                rows.isEmpty() -> EmptyState(
                    title = stringResource(R.string.list_empty_title),
                    body = stringResource(R.string.list_empty_body),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = rows.size, key = { rows[it].ref.id }) { index ->
                        onRowReached(index)

                        val item = rows[index]
                        PokemonRow(
                            ref = item.ref,
                            state = rowFor(item.ref),
                            isFavorite = item.isFavorite,
                            onFavoriteToggle = { onFavoriteToggle(item.ref) },
                        )
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

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListLoadedPreview() {
    PokedexTheme {
        PokemonListContent(
            rows = previewItems,
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            query = "",
            onQueryChange = {},
            selectedTypes = emptySet(),
            onTypeToggle = {},
            onFavoriteToggle = {},
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
            rows = previewItems,
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            query = "",
            onQueryChange = {},
            selectedTypes = emptySet(),
            onTypeToggle = {},
            onFavoriteToggle = {},
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
            selectedTypes = emptySet(),
            onTypeToggle = {},
            onFavoriteToggle = {},
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
            selectedTypes = setOf(PokemonType.DRAGON),
            onTypeToggle = {},
            onFavoriteToggle = {},
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
            selectedTypes = emptySet(),
            onTypeToggle = {},
            onFavoriteToggle = {},
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
            rows = previewItems,
            onRowReached = {},
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Loading,
            query = "",
            onQueryChange = {},
            selectedTypes = emptySet(),
            onTypeToggle = {},
            onFavoriteToggle = {},
            rowFor = { previewLoaded(it) },
            onRetry = {},
        )
    }
}

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
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.presentation.list.components.ListAppendError
import it.kata.pokedex.presentation.list.components.ListAppendLoading
import it.kata.pokedex.presentation.list.components.ListErrorState
import it.kata.pokedex.presentation.list.components.PokedexHeader
import it.kata.pokedex.presentation.list.components.PokemonRow
import it.kata.pokedex.presentation.list.components.PokemonRowPlaceholder
import it.kata.pokedex.presentation.theme.PokedexTheme
import kotlinx.coroutines.flow.flowOf

/** How many skeleton rows to draw while the first page is on its way. */
private const val PLACEHOLDER_ROWS = 8

/**
 * The list screen, stateless: it renders what it is given.
 *
 * The two failures are told apart on purpose. Losing the first page leaves an empty screen, so it
 * gets the full error state; losing a later page leaves a usable list, so the retry goes quietly at
 * the bottom.
 */
@Composable
fun PokemonListScreen(
    pokemon: LazyPagingItems<Pokemon>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PokedexHeader()

        Box(modifier = Modifier.fillMaxSize()) {
            when (pokemon.loadState.refresh) {
                is LoadState.Loading -> LoadingRows()
                is LoadState.Error -> ListErrorState(onRetry = pokemon::retry)
                is LoadState.NotLoading -> LoadedRows(pokemon)
            }
        }
    }
}

@Composable
private fun LoadedRows(pokemon: LazyPagingItems<Pokemon>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = pokemon.itemCount,
            key = pokemon.itemKey { it.id },
        ) { index ->
            val item = pokemon[index]
            if (item != null) {
                PokemonRow(pokemon = item)
                RowDivider()
            }
        }

        when (pokemon.loadState.append) {
            is LoadState.Loading -> item { ListAppendLoading() }
            is LoadState.Error -> item { ListAppendError(onRetry = pokemon::retry) }
            is LoadState.NotLoading -> Unit
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
private fun PokemonListScreenPreview() {
    PokedexTheme {
        PokemonListScreen(
            pokemon = flowOf(PagingData.from(previewPokemon)).collectAsLazyPagingItems(),
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PokemonListScreenLoadingPreview() {
    PokedexTheme { LoadingRows() }
}

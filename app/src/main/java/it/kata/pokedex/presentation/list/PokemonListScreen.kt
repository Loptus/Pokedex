package it.kata.pokedex.presentation.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.presentation.list.components.PokedexHeader
import it.kata.pokedex.presentation.list.components.PokemonRow
import it.kata.pokedex.presentation.theme.PokedexTheme
import kotlinx.coroutines.flow.flowOf

/**
 * The list screen, stateless: it renders what it is given.
 *
 * The description is not part of [Pokemon] because it comes from a different endpoint, so the
 * screen asks for it by id through [descriptionFor].
 */
@Composable
fun PokemonListScreen(
    pokemon: LazyPagingItems<Pokemon>,
    descriptionFor: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PokedexHeader()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                count = pokemon.itemCount,
                key = pokemon.itemKey { it.id },
            ) { index ->
                val item = pokemon[index]
                if (item != null) {
                    PokemonRow(
                        pokemon = item,
                        description = descriptionFor(item.id),
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PokemonListScreenPreview() {
    PokedexTheme {
        PokemonListScreen(
            pokemon = flowOf(PagingData.from(staticPokemon)).collectAsLazyPagingItems(),
            descriptionFor = { staticDescriptions.getValue(it) },
        )
    }
}

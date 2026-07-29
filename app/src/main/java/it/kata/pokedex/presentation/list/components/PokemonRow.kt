package it.kata.pokedex.presentation.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import it.kata.pokedex.R
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.presentation.common.displayName
import it.kata.pokedex.presentation.list.sampleDescriptions
import it.kata.pokedex.presentation.list.samplePokemon
import it.kata.pokedex.presentation.theme.PokedexTheme

/**
 * One row of the list, straight from the mockup: artwork, name, type chips, two lines of
 * description. Stateless: it renders what it is given.
 */
@Composable
fun PokemonRow(
    pokemon: Pokemon,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(pokemon.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.cd_pokemon_artwork, pokemon.displayName),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = pokemon.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pokemon.types.forEach { TypeChip(it) }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowPreview() {
    PokedexTheme {
        PokemonRow(
            pokemon = samplePokemon[0],
            description = sampleDescriptions.getValue(samplePokemon[0].id),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowLongDescriptionPreview() {
    PokedexTheme {
        PokemonRow(
            pokemon = samplePokemon[3],
            description = sampleDescriptions.getValue(samplePokemon[3].id),
        )
    }
}

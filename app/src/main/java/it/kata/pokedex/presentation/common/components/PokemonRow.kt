package it.kata.pokedex.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import it.kata.pokedex.R
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.presentation.common.PokemonRowState
import it.kata.pokedex.presentation.common.displayName
import it.kata.pokedex.presentation.common.previewLoaded
import it.kata.pokedex.presentation.common.previewRefs
import it.kata.pokedex.presentation.theme.PokedexTheme

/**
 * One row of the list, straight from the mockup: artwork, name, type chips, two lines of
 * description. Stateless: it renders what it is given.
 *
 * The name comes from [ref] and is there from the first frame, because the index already knows it.
 * Everything else arrives with [state], once this row's own request comes back.
 *
 * The heart is there from the first frame too, and works there: what gets saved is the pointer,
 * which the row has before its contents arrive. Waiting for the description to land would be a
 * button disabled for no reason the user can see.
 */
@Composable
fun PokemonRow(
    ref: PokemonRef,
    state: PokemonRowState,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pokemon = (state as? PokemonRowState.Loaded)?.pokemon

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(pokemon?.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.cd_pokemon_artwork, ref.displayName),
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
                text = ref.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            when {
                pokemon != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pokemon.types.forEach { TypeChip(it) }
                    }
                    Text(
                        text = pokemon.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Nothing to draw for a failed row: it keeps its name, and trying again is a
                // matter of scrolling away and back.
                state is PokemonRowState.Failed -> Unit

                else -> RowContentsPlaceholder()
            }
        }

        IconButton(onClick = onFavoriteToggle) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite,
                ),
                contentDescription = stringResource(
                    if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                    ref.displayName,
                ),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowLoadedPreview() {
    PokedexTheme {
        PokemonRow(
            ref = previewRefs[0],
            state = previewLoaded(previewRefs[0]),
            isFavorite = false,
            onFavoriteToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowFavoritePreview() {
    PokedexTheme {
        PokemonRow(
            ref = previewRefs[0],
            state = previewLoaded(previewRefs[0]),
            isFavorite = true,
            onFavoriteToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowSingleTypePreview() {
    PokedexTheme {
        PokemonRow(
            ref = previewRefs[2],
            state = previewLoaded(previewRefs[2]),
            isFavorite = false,
            onFavoriteToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowLoadingPreview() {
    PokedexTheme {
        PokemonRow(
            ref = previewRefs[1],
            state = PokemonRowState.Loading,
            isFavorite = false,
            onFavoriteToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonRowFailedPreview() {
    PokedexTheme {
        PokemonRow(
            ref = previewRefs[1],
            state = PokemonRowState.Failed,
            isFavorite = false,
            onFavoriteToggle = {},
        )
    }
}

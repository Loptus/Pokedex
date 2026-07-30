package it.kata.pokedex.presentation.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.kata.pokedex.R
import it.kata.pokedex.domain.model.PokemonType
import it.kata.pokedex.presentation.common.label
import it.kata.pokedex.presentation.theme.PokedexTheme
import it.kata.pokedex.presentation.theme.color
import it.kata.pokedex.presentation.theme.contentColorOn
import androidx.compose.ui.res.stringResource

/**
 * A row of chips, one per type, multiple selection.
 *
 * A deliberate departure from the mockup, which has a single field for both name and type. Chips
 * show which types exist instead of asking the user to guess them, they can be combined, and they
 * keep the query unambiguous for the layers underneath.
 *
 * Selected chips take the colour of their type, with the label switched to black or white by
 * luminance so every one of the eighteen stays readable.
 */
@Composable
fun TypeFilterRow(
    selectedTypes: Set<PokemonType>,
    onTypeToggle: (PokemonType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.cd_type_filter)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
    ) {
        items(PokemonType.entries, key = { it.apiName }) { type ->
            FilterChip(
                selected = type in selectedTypes,
                onClick = { onTypeToggle(type) },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = type.color,
                    selectedLabelColor = contentColorOn(type.color),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TypeFilterRowPreview() {
    PokedexTheme {
        TypeFilterRow(selectedTypes = emptySet(), onTypeToggle = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TypeFilterRowSelectedPreview() {
    PokedexTheme {
        TypeFilterRow(
            selectedTypes = setOf(PokemonType.NORMAL, PokemonType.FIRE, PokemonType.ELECTRIC),
            onTypeToggle = {},
        )
    }
}

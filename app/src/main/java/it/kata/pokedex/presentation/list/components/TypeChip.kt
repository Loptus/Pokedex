package it.kata.pokedex.presentation.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.kata.pokedex.domain.model.PokemonType
import it.kata.pokedex.presentation.common.label
import it.kata.pokedex.presentation.theme.PokedexTheme
import it.kata.pokedex.presentation.theme.color
import it.kata.pokedex.presentation.theme.contentColorOn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun TypeChip(
    type: PokemonType,
    modifier: Modifier = Modifier,
) {
    val background = type.color
    Text(
        text = type.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = contentColorOn(background),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun TypeChipsPreview() {
    PokedexTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            PokemonType.entries.take(6).forEach { TypeChip(it) }
        }
    }
}

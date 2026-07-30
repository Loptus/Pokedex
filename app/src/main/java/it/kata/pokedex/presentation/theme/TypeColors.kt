package it.kata.pokedex.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import it.kata.pokedex.domain.model.PokemonType

private val TypeColors = mapOf(
    PokemonType.NORMAL to Color(0xFFA8A77A),
    PokemonType.FIRE to Color(0xFFEE8130),
    PokemonType.WATER to Color(0xFF6390F0),
    PokemonType.GRASS to Color(0xFF7AC74C),
    PokemonType.ELECTRIC to Color(0xFFF7D02C),
    PokemonType.ICE to Color(0xFF96D9D6),
    PokemonType.FIGHTING to Color(0xFFC22E28),
    PokemonType.POISON to Color(0xFFA33EA1),
    PokemonType.GROUND to Color(0xFFE2BF65),
    PokemonType.FLYING to Color(0xFFA98FF3),
    PokemonType.PSYCHIC to Color(0xFFF95587),
    PokemonType.BUG to Color(0xFFA6B91A),
    PokemonType.ROCK to Color(0xFFB6A136),
    PokemonType.GHOST to Color(0xFF735797),
    PokemonType.DRAGON to Color(0xFF6F35FC),
    PokemonType.DARK to Color(0xFF705746),
    PokemonType.STEEL to Color(0xFFB7B7CE),
    PokemonType.FAIRY to Color(0xFFD685AD),
)

/**
 * Luminance above which black text beats white text on the same background.
 *
 * It comes out of the WCAG contrast formula: black and white tie when
 * `(L + 0.05) / 0.05 == 1.05 / (L + 0.05)`, that is at `L = 0.179`. Picking the winner at this
 * pivot guarantees at least 4.5:1 whatever the background, so the eighteen type colours stay
 * readable without hand tuning eighteen pairs.
 */
private const val CONTRAST_PIVOT = 0.179f

val PokemonType.color: Color get() = TypeColors.getValue(this)

fun contentColorOn(background: Color): Color =
    if (background.luminance() > CONTRAST_PIVOT) Color.Black else Color.White

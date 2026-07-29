package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.Pokemon

/**
 * What the list screen needs to render itself.
 *
 * Descriptions sit beside the list rather than inside [Pokemon] because they come from a different
 * endpoint: the model stays the model, and the screen looks a description up by id.
 */
data class PokemonListUiState(
    val pokemon: List<Pokemon> = emptyList(),
    val descriptions: Map<Int, String> = emptyMap(),
)

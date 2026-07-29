package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.Pokemon

/**
 * What the list screen needs on top of the paged items themselves.
 *
 * Descriptions sit here rather than inside [Pokemon] because they come from a different endpoint:
 * the model stays the model, and the screen looks a description up by id.
 */
data class PokemonListUiState(
    val descriptions: Map<Int, String> = emptyMap(),
)

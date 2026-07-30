package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.PokemonType

/**
 * What the list screen needs on top of the paged items themselves.
 *
 * Which entries are favorites is deliberately not here: it belongs to the entries, and it reaches
 * the screen already decided, on each [PokemonListItem].
 */
data class PokemonListUiState(
    val query: String = "",
    val selectedTypes: Set<PokemonType> = emptySet(),
)

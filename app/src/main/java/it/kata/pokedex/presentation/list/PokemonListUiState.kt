package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.PokemonType

/**
 * What the list screen needs on top of the paged items themselves.
 */
data class PokemonListUiState(
    val query: String = "",
    val selectedTypes: Set<PokemonType> = emptySet(),
)

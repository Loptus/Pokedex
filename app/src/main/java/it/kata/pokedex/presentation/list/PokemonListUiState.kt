package it.kata.pokedex.presentation.list

/**
 * What the list screen needs on top of the paged items themselves.
 */
data class PokemonListUiState(
    val query: String = "",
)

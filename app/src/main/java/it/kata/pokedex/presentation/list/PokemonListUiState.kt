package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.PokemonType

data class PokemonListUiState(
    val query: String = "",
    val selectedTypes: Set<PokemonType> = emptySet(),
)

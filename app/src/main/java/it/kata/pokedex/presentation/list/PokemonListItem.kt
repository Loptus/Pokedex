package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.PokemonRef

/**
 * The reason this type exists: [isFavorite] is decided by the ViewModel, so the screen renders a
 * flag instead of matching an id against a set, which would be a decision taken in a composable.
 */
data class PokemonListItem(
    val ref: PokemonRef,
    val isFavorite: Boolean,
)

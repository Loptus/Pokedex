package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.PokemonRef

/**
 * One entry of the list as the screen renders it: the pointer, plus everything about it that the
 * ViewModel has already decided.
 *
 * [isFavorite] is a decided value and not a set to look into, and that is the whole reason this
 * type exists: matching an id against the saved favorites is a rule, and a composable that applied
 * it would be making a decision instead of drawing one.
 *
 * It rides with the paged item rather than with [PokemonRowState] because the two answer different
 * questions: whether an entry is saved is known from the index alone, while the rest of the row has
 * to be fetched.
 */
data class PokemonListItem(
    val ref: PokemonRef,
    val isFavorite: Boolean,
)

package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.Pokemon

/**
 * How much of a row is known.
 *
 * A row starts as a name from the index and fills in once its own request comes back, which is why
 * these three states exist at all: the list is on screen long before its contents are.
 */
sealed interface PokemonRowState {

    data object Loading : PokemonRowState

    data class Loaded(val pokemon: Pokemon) : PokemonRowState

    /**
     * The request failed. The row keeps its name and drops the rest, and the failure is not
     * remembered, so scrolling away and back tries again without needing a retry button per row.
     */
    data object Failed : PokemonRowState
}

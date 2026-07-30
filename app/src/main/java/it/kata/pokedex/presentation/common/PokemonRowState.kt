package it.kata.pokedex.presentation.common

import it.kata.pokedex.domain.model.Pokemon

sealed interface PokemonRowState {

    data object Loading : PokemonRowState

    data class Loaded(val pokemon: Pokemon) : PokemonRowState

    /** Not remembered as failed, so scrolling away and back tries again: see [PokemonRowLoader]. */
    data object Failed : PokemonRowState
}

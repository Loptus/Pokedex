package it.kata.pokedex.domain.usecase

import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import javax.inject.Inject

/**
 * Everything one row of the list shows, fetched for one Pokemon.
 *
 * Separate from the paging use case because the two have different costs: a page of pointers is
 * free, a loaded row is two requests. Keeping them apart is what lets the list only pay for the rows
 * that reach the screen.
 */
class GetPokemonUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {

    suspend operator fun invoke(ref: PokemonRef): AppResult<Pokemon> = repository.pokemon(ref)
}

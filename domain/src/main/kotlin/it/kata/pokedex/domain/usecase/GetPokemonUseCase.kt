package it.kata.pokedex.domain.usecase

import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import javax.inject.Inject

class GetPokemonUseCase @Inject constructor(
    private val repository: PokemonRepository,
) {

    suspend operator fun invoke(ref: PokemonRef): AppResult<Pokemon> = repository.pokemon(ref)
}

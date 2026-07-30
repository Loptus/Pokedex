package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    suspend operator fun invoke(ref: PokemonRef) = repository.toggle(ref)
}

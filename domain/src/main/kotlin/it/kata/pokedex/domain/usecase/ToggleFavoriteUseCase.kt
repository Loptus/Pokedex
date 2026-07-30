package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Adds or removes a favorite, from a single tap on the heart.
 *
 * One action instead of an add and a remove: the caller taps a heart and does not decide which of
 * the two it is, the stored state does.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    suspend operator fun invoke(ref: PokemonRef) = repository.toggle(ref)
}

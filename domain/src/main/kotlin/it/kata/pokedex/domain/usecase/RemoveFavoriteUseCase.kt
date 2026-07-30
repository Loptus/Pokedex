package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Removes a saved entry, from the page that lists them.
 *
 * Takes an id rather than a whole pointer: to stop being a favorite an entry only has to be named.
 */
class RemoveFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    suspend operator fun invoke(id: Int) = repository.remove(id)
}

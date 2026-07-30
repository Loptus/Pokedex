package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Which entries are favorites, as a set, which is the shape the list wants: it asks about the rows
 * it happens to be showing, and asking a set is the cheap way round.
 */
class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    operator fun invoke(): Flow<Set<Int>> = repository.favoriteIds()
}

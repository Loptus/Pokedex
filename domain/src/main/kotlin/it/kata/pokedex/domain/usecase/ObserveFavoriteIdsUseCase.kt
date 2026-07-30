package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    operator fun invoke(): Flow<Set<Int>> = repository.favoriteIds()
}

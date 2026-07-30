package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.repository.FavoriteRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    suspend operator fun invoke(id: Int) = repository.remove(id)
}

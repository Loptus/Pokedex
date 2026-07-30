package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    operator fun invoke(): Flow<List<PokemonRef>> = repository.favorites()
}

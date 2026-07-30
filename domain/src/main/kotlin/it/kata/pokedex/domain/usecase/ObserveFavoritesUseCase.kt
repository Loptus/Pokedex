package it.kata.pokedex.domain.usecase

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The saved entries, as pointers, in the order the page shows them.
 *
 * Pointers and not whole Pokemon: what was saved is which entry, and its contents are fetched a row
 * at a time exactly like in the list.
 */
class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {

    operator fun invoke(): Flow<List<PokemonRef>> = repository.favorites()
}

package it.kata.pokedex.utils

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * One list is the source of truth and the ids are derived from it, so the fake cannot drift into a
 * state the real database could never be in.
 */
class FakeFavoriteRepository : FavoriteRepository {

    val saved = MutableStateFlow<List<PokemonRef>>(emptyList())
    val toggled = mutableListOf<Int>()
    val removed = mutableListOf<Int>()

    /** Set to keep a write pending, so a cancellation can land in the middle of it. */
    var hold: CompletableDeferred<Unit>? = null

    override fun favoriteIds(): Flow<Set<Int>> = saved.map { refs -> refs.mapTo(mutableSetOf()) { it.id } }

    override fun favorites(): Flow<List<PokemonRef>> = saved

    override suspend fun toggle(ref: PokemonRef) {
        hold?.await()
        toggled += ref.id
        saved.update { current ->
            if (current.any { it.id == ref.id }) current.filterNot { it.id == ref.id } else current + ref
        }
    }

    override suspend fun remove(id: Int) {
        hold?.await()
        removed += id
        saved.update { current -> current.filterNot { it.id == id } }
    }
}

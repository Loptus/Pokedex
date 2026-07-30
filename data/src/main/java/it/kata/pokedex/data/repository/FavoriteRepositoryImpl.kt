package it.kata.pokedex.data.repository

import it.kata.pokedex.data.local.FavoriteDao
import it.kata.pokedex.data.local.mapper.toEntity
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Barely more than the DAO, and that is the point: the interface is what the domain depends on, and
 * Room is what happens to be behind it today.
 *
 * No injected dispatcher here, unlike [PokemonRepositoryImpl]. Room already runs suspend queries and
 * Flow queries on its own executor, so a `withContext` around them would move work that has already
 * moved.
 */
class FavoriteRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
) : FavoriteRepository {

    /**
     * `distinctUntilChanged` because Room re-runs the query on every write to the table, and a write
     * that leaves the set as it was would otherwise walk the whole list screen for nothing.
     */
    override fun favoriteIds(): Flow<Set<Int>> = dao.observeIds()
        .map { it.toSet() }
        .distinctUntilChanged()

    override suspend fun toggle(ref: PokemonRef) = dao.toggle(ref.toEntity())
}

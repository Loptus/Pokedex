package it.kata.pokedex.data.repository

import it.kata.pokedex.data.local.FavoriteDao
import it.kata.pokedex.data.local.FavoritePokemonEntity
import it.kata.pokedex.data.local.mapper.toDomain
import it.kata.pokedex.data.local.mapper.toEntity
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * No injected dispatcher here, unlike [PokemonRepositoryImpl]: Room already runs suspend and Flow
 * queries on its own executor.
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

    override fun favorites(): Flow<List<PokemonRef>> = dao.observeAll()
        .map { saved -> saved.map(FavoritePokemonEntity::toDomain) }

    override suspend fun toggle(ref: PokemonRef) = dao.toggle(ref.toEntity())

    override suspend fun remove(id: Int) {
        dao.deleteById(id)
    }
}

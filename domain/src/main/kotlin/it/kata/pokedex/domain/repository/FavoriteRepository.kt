package it.kata.pokedex.domain.repository

import it.kata.pokedex.domain.model.PokemonRef
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {

    fun favoriteIds(): Flow<Set<Int>>

    fun favorites(): Flow<List<PokemonRef>>

    /** Saves [ref] if it is not a favorite yet, and drops it if it already is. */
    suspend fun toggle(ref: PokemonRef)

    suspend fun remove(id: Int)
}

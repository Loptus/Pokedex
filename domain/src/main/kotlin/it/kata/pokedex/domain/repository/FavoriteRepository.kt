package it.kata.pokedex.domain.repository

import it.kata.pokedex.domain.model.PokemonRef
import kotlinx.coroutines.flow.Flow

/**
 * Favorites, kept apart from [PokemonRepository] rather than added to it: one is a list served by
 * the network, the other a handful of rows owned by the device, and they have no reason to change
 * together.
 *
 * What is stored is a [PokemonRef], the pointer, not a whole [it.kata.pokedex.domain.model.Pokemon]:
 * a favorite is a choice about which entry, and the contents of that entry are the API's business.
 * The price is that a favorite still has to be fetched to be drawn.
 */
interface FavoriteRepository {

    /**
     * The ids of every favorite, re-emitted on every change, because whoever draws a heart has to
     * find out when it changes elsewhere.
     */
    fun favoriteIds(): Flow<Set<Int>>

    /** Saves [ref] if it is not a favorite yet, and drops it if it already is. */
    suspend fun toggle(ref: PokemonRef)
}

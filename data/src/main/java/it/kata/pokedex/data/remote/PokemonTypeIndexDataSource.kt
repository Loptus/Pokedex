package it.kata.pokedex.data.remote

import it.kata.pokedex.domain.model.PokemonType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which Pokemon carry which type, fetched once per type and then answered from memory.
 *
 * The types of a Pokemon only exist in its detail, and details are fetched per visible row, so the
 * list cannot be filtered by walking what is on screen. `GET /type/{name}` answers the other way
 * round, with the membership of a whole type at once: between 2.7 and 4.5 KB gzipped, and only for
 * the types the user actually taps.
 */
@Singleton
class PokemonTypeIndexDataSource @Inject constructor(
    private val api: PokeApi,
) {

    private val mutex = Mutex()
    private val namesByType = mutableMapOf<PokemonType, Set<String>>()

    /**
     * Names carrying any of [types], which is the union on purpose: a chip filter that returns
     * nothing when Fire and Water are both on reads as broken rather than as empty.
     */
    suspend fun namesOfAnyOf(types: Set<PokemonType>): Set<String> =
        types.flatMapTo(mutableSetOf()) { namesOf(it) }

    /**
     * The fetch happens while holding the lock, so rows arriving during it wait instead of each
     * firing a request of their own. A failure leaves the type unset and the next attempt retries.
     *
     * Several selected types are therefore fetched one after the other. With a handful of taps that
     * is a few kilobytes either way, and it is not worth a lock per type to overlap them.
     */
    private suspend fun namesOf(type: PokemonType): Set<String> = mutex.withLock {
        namesByType[type] ?: api.getType(type.apiName)
            .pokemon.orEmpty()
            .mapNotNull { it.pokemon?.name?.takeIf(String::isNotBlank) }
            .toSet()
            .also { namesByType[type] = it }
    }
}

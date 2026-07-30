package it.kata.pokedex.data.remote

import it.kata.pokedex.domain.model.PokemonType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The types of a Pokemon only exist in its detail, and details arrive per visible row, so the list
 * cannot be filtered by what is on screen. `GET /type/{name}` answers the other way round, a whole
 * type at a time, and only for the types the user taps.
 */
@Singleton
class PokemonTypeIndexDataSource @Inject constructor(
    private val api: PokeApi,
) {

    private val mutex = Mutex()
    private val namesByType = mutableMapOf<PokemonType, Set<String>>()

    suspend fun namesOfAnyOf(types: Set<PokemonType>): Set<String> =
        types.flatMapTo(mutableSetOf()) { namesOf(it) }

    /**
     * The fetch happens while holding the lock, so rows arriving during it wait instead of each
     * firing a request of their own. Selected types are therefore fetched one after the other: a
     * lock per type is not worth the few kilobytes it would overlap.
     */
    private suspend fun namesOf(type: PokemonType): Set<String> = mutex.withLock {
        namesByType[type] ?: api.getType(type.apiName)
            .pokemon.orEmpty()
            .mapNotNull { it.pokemon?.name?.takeIf(String::isNotBlank) }
            .toSet()
            .also { namesByType[type] = it }
    }
}

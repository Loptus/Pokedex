package it.kata.pokedex.data.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.core.resultOf
import it.kata.pokedex.data.remote.PokeApi
import it.kata.pokedex.data.remote.PokemonNameIndexDataSource
import it.kata.pokedex.data.remote.PokemonPagingSource
import it.kata.pokedex.data.remote.mapper.toDescription
import it.kata.pokedex.data.remote.mapper.toDomain
import it.kata.pokedex.di.IoDispatcher
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Builds a page of the list out of the name index plus two endpoints per entry.
 *
 * The names all come from memory, so a page is a slice of them and browsing and searching are the
 * same code path: a blank query simply matches everything. What actually costs something is the
 * forty calls that turn twenty names into twenty rows, and that is where the effort goes: they run
 * in parallel rather than one after the other, and the OkHttp disk cache means a page already
 * visited costs nothing on the way back.
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApi,
    private val nameIndex: PokemonNameIndexDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override fun pokemonPagingSource(query: String): PagingSource<Int, Pokemon> =
        PokemonPagingSource { offset, limit -> getPage(query, offset, limit) }

    /**
     * Not part of [PokemonRepository]: the domain only ever asks for a paging source, so fetching
     * a single window stays an implementation detail. Visible for its own tests.
     */
    suspend fun getPage(query: String, offset: Int, limit: Int): AppResult<PokemonPage> =
        withContext(dispatcher) {
            resultOf {
                val matches = nameIndex.namesMatching(query)

                PokemonPage(
                    items = hydrate(matches.drop(offset).take(limit)),
                    hasMore = offset + limit < matches.size,
                )
            }
        }

    /** The expensive half, and the reason it runs in parallel: two calls for each of twenty names. */
    private suspend fun hydrate(names: List<String>): List<Pokemon> = coroutineScope {
        names.map { name -> async { loadPokemon(name) } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun loadPokemon(name: String): Pokemon? {
        val detail = api.getPokemonDetail(name)
        val id = detail.id ?: return null
        return detail.toDomain(description = loadDescription(id))
    }

    /**
     * A missing description is worth an empty line, not a failed page: the row still has artwork,
     * name and types. Cancellation is rethrown so a scrolled away page really does stop working.
     */
    private suspend fun loadDescription(id: Int): String =
        try {
            api.getPokemonSpecies(id).toDescription()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ""
        }
}

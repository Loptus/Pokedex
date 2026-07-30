package it.kata.pokedex.data.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.core.resultOf
import it.kata.pokedex.data.remote.PokeApi
import it.kata.pokedex.data.remote.PokemonNameIndexDataSource
import it.kata.pokedex.data.remote.PokemonTypeIndexDataSource
import it.kata.pokedex.data.remote.PokemonPagingSource
import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.mapper.toDescription
import it.kata.pokedex.data.remote.mapper.toDomain
import it.kata.pokedex.di.IoDispatcher
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.model.PokemonQuery
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Serves the list in two very different sizes.
 *
 * A page is free: the indexes are already in memory, so a page is a slice of them, and browsing,
 * searching and filtering are the same code path with an empty query meaning everything. Nothing is
 * fetched.
 *
 * A row costs two requests, about 12 KB gzipped, and is fetched only when that row is on screen.
 * Cancelling the caller cancels them, which is what makes scrolling quickly past a row cheap.
 *
 * The two requests are sequential, and deliberately so: the address of the description is inside the
 * detail. Running them together would mean assembling that address from the id instead of reading
 * it, and the ids do not line up, so one round trip is the price of being right.
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApi,
    private val nameIndex: PokemonNameIndexDataSource,
    private val typeIndex: PokemonTypeIndexDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override fun pokemonPagingSource(query: PokemonQuery): PagingSource<Int, PokemonRef> =
        PokemonPagingSource { offset, limit -> getPage(query, offset, limit) }

    override suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon> = withContext(dispatcher) {
        resultOf {
            val detail = api.getPokemonDetail(ref.detailUrl)

            detail.toDomain(description = descriptionOf(detail))
                ?: throw IOException("${ref.name} came back without an id or a name")
        }
    }

    /**
     * A missing description is worth an empty line, not a blank row: artwork, name and types are
     * already there and are most of what the row is for.
     */
    private suspend fun descriptionOf(detail: PokemonDetailDto): String {
        val url = detail.species?.url?.takeIf { it.isNotBlank() } ?: return ""

        return try {
            api.getPokemonSpecies(url).toDescription()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Not part of [PokemonRepository]: the domain only ever asks for a paging source, so fetching
     * a single window stays an implementation detail. Visible for its own tests.
     */
    suspend fun getPage(query: PokemonQuery, offset: Int, limit: Int): AppResult<PokemonPage> =
        withContext(dispatcher) {
            resultOf {
                val matches = matching(query)

                PokemonPage(
                    items = matches.drop(offset).take(limit),
                    hasMore = offset + limit < matches.size,
                )
            }
        }

    /**
     * Both filters are applied to the pointers, before anything is fetched, so narrowing the list
     * stays free. The name comes from the index; the types come from the type index and combine
     * with the name in AND, while the types themselves are a union.
     */
    private suspend fun matching(query: PokemonQuery): List<PokemonRef> {
        val byName = nameIndex.matching(query.name)
        if (query.types.isEmpty()) return byName

        val ofSelectedTypes = typeIndex.namesOfAnyOf(query.types)
        return byName.filter { it.name in ofSelectedTypes }
    }
}

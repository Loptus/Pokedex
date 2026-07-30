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
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Serves the list in two very different sizes.
 *
 * A page is free: the index is already in memory, so a page is a slice of it, and browsing and
 * searching are the same code path with a blank query meaning everything. Nothing is fetched.
 *
 * A row costs two requests, about 12 KB gzipped, and is fetched only when that row is on screen.
 * They go out in parallel because the id is already known from the index, and cancelling the caller
 * cancels them both, which is what makes scrolling quickly past a row cheap.
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApi,
    private val nameIndex: PokemonNameIndexDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override fun pokemonPagingSource(query: String): PagingSource<Int, PokemonRef> =
        PokemonPagingSource { offset, limit -> getPage(query, offset, limit) }

    override suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon> = withContext(dispatcher) {
        resultOf {
            coroutineScope {
                val detail = async { api.getPokemonDetail(ref.name) }
                val description = async { api.getPokemonSpecies(ref.id).toDescription() }

                detail.await().toDomain(description = description.await())
                    ?: throw IOException("${ref.name} came back without an id or a name")
            }
        }
    }

    /**
     * Not part of [PokemonRepository]: the domain only ever asks for a paging source, so fetching
     * a single window stays an implementation detail. Visible for its own tests.
     */
    suspend fun getPage(query: String, offset: Int, limit: Int): AppResult<PokemonPage> =
        withContext(dispatcher) {
            resultOf {
                val matches = nameIndex.matching(query)

                PokemonPage(
                    items = matches.drop(offset).take(limit),
                    hasMore = offset + limit < matches.size,
                )
            }
        }
}

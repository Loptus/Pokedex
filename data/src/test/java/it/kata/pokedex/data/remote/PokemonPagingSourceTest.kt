package it.kata.pokedex.data.remote

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PokemonPagingSourceTest {

    @Test
    fun `the first page has no previous and points at the next offset`() = runTest {
        val page = load(key = null, loadPage = pageOf(20, hasMore = true))

        assertEquals(20, page.data.size)
        assertNull(page.prevKey)
        assertEquals(20, page.nextKey)
    }

    @Test
    fun `a later page walks back and forward by the page size`() = runTest {
        val page = load(key = 40, loadPage = pageOf(20, hasMore = true))

        assertEquals(20, page.prevKey)
        assertEquals(60, page.nextKey)
    }

    @Test
    fun `stops when the api says there is nothing after this page`() = runTest {
        val page = load(key = 40, loadPage = pageOf(7, hasMore = false))

        assertEquals(7, page.data.size)
        assertNull(page.nextKey)
    }

    /**
     * Entries that cannot be parsed get dropped, so a short page is not the end of the list. The
     * next offset has to advance by the window that was asked for, not by what survived, otherwise
     * the following page would repeat entries.
     */
    @Test
    fun `a short page still advances by a full window`() = runTest {
        val page = load(key = 0, loadPage = pageOf(18, hasMore = true))

        assertEquals(18, page.data.size)
        assertEquals(20, page.nextKey)
    }

    @Test
    fun `a failure becomes a paging error rather than a crash`() = runTest {
        val cause = IOException("no network")
        val source = PokemonPagingSource { _, _ -> AppResult.Failure(cause) }

        val result = source.load(refreshParams(key = null))

        assertIs<PagingSource.LoadResult.Error<Int, Pokemon>>(result)
        assertEquals(cause, result.throwable)
    }

    private fun pageOf(
        size: Int,
        hasMore: Boolean,
    ): suspend (Int, Int) -> AppResult<PokemonPage> = { offset, _ ->
        AppResult.Success(
            PokemonPage(
                items = List(size) { index ->
                    Pokemon(
                        id = offset + index,
                        name = "pokemon-${offset + index}",
                        imageUrl = null,
                        types = emptyList(),
                        description = "",
                    )
                },
                hasMore = hasMore,
            ),
        )
    }

    private suspend fun load(
        key: Int?,
        loadPage: suspend (Int, Int) -> AppResult<PokemonPage>,
    ): PagingSource.LoadResult.Page<Int, Pokemon> {
        val result = PokemonPagingSource(loadPage).load(refreshParams(key))
        return assertIs(result)
    }

    private fun refreshParams(key: Int?) = PagingSource.LoadParams.Refresh(
        key = key,
        loadSize = 20,
        placeholdersEnabled = false,
    )
}

package it.kata.pokedex.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonPage

/**
 * Pages the list by offset into the in memory index. Pointers only: a page costs no requests,
 * and each row fetches its own contents once it is on screen.
 *
 * It takes a function rather than the repository itself: all it needs is a way to ask for a window
 * of the list, and staying ignorant of who answers keeps it trivial to test.
 */
class PokemonPagingSource(
    private val loadPage: suspend (offset: Int, limit: Int) -> AppResult<PokemonPage>,
) : PagingSource<Int, PokemonRef>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PokemonRef> {
        val offset = params.key ?: 0

        return when (val result = loadPage(offset, params.loadSize)) {
            is AppResult.Success -> LoadResult.Page(
                data = result.value.items,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                // Advances by the page size asked for, not by how many entries survived being
                // read, otherwise a dropped entry would make the next page repeat one.
                nextKey = if (result.value.hasMore) offset + params.loadSize else null,
            )

            is AppResult.Failure -> LoadResult.Error(result.cause)
        }
    }

    /**
     * Where to restart after an invalidation: the offset of the page the user was looking at, so a
     * refresh does not throw them back to the top.
     */
    override fun getRefreshKey(state: PagingState<Int, PokemonRef>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor) ?: return@let null
            page.prevKey?.plus(state.config.pageSize) ?: page.nextKey?.minus(state.config.pageSize)
        }
}

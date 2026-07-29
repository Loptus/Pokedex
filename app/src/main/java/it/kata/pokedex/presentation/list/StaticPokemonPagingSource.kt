package it.kata.pokedex.presentation.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import it.kata.pokedex.domain.model.Pokemon

/**
 * Pages through an in memory list.
 *
 * The key is the offset of the page, which is the same shape the PokeAPI uses
 * (`?limit=20&offset=40`). Swapping this source for the network one later is a change of where the
 * items come from, not of how paging works.
 */
class StaticPokemonPagingSource(
    private val pokemon: List<Pokemon>,
) : PagingSource<Int, Pokemon>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
        val offset = params.key ?: 0
        val page = pokemon.drop(offset).take(params.loadSize)
        val nextOffset = offset + page.size

        return LoadResult.Page(
            data = page,
            prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
            nextKey = if (nextOffset >= pokemon.size) null else nextOffset,
        )
    }

    /**
     * Where to restart from after the list is invalidated: the offset of the page the user was
     * looking at, so a refresh does not throw them back to the top.
     */
    override fun getRefreshKey(state: PagingState<Int, Pokemon>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor) ?: return@let null
            page.prevKey?.plus(state.config.pageSize) ?: page.nextKey?.minus(state.config.pageSize)
        }
}

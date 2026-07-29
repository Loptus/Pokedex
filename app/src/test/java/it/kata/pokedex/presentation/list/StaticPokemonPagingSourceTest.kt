package it.kata.pokedex.presentation.list

import androidx.paging.PagingSource
import it.kata.pokedex.domain.model.Pokemon
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StaticPokemonPagingSourceTest {

    private val pokemon = List(45) { index ->
        Pokemon(id = index + 1, name = "pokemon-${index + 1}", imageUrl = null, types = emptyList())
    }

    @Test
    fun `first page starts at the top and points to the next offset`() = runTest {
        val page = load(key = null)

        assertEquals(20, page.data.size)
        assertEquals(1, page.data.first().id)
        assertNull(page.prevKey)
        assertEquals(20, page.nextKey)
    }

    @Test
    fun `a later page starts at its own offset`() = runTest {
        val page = load(key = 20)

        assertEquals(20, page.data.size)
        assertEquals(21, page.data.first().id)
        assertEquals(0, page.prevKey)
        assertEquals(40, page.nextKey)
    }

    @Test
    fun `the last page is short and stops the paging`() = runTest {
        val page = load(key = 40)

        assertEquals(5, page.data.size)
        assertNull(page.nextKey)
    }

    @Test
    fun `an offset past the end returns nothing and stops the paging`() = runTest {
        val page = load(key = 45)

        assertEquals(emptyList(), page.data)
        assertNull(page.nextKey)
    }

    @Test
    fun `an empty source stops immediately`() = runTest {
        val page = load(key = null, source = StaticPokemonPagingSource(emptyList()))

        assertEquals(emptyList(), page.data)
        assertNull(page.prevKey)
        assertNull(page.nextKey)
    }

    private suspend fun load(
        key: Int?,
        source: StaticPokemonPagingSource = StaticPokemonPagingSource(pokemon),
    ): PagingSource.LoadResult.Page<Int, Pokemon> {
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = key,
                loadSize = 20,
                placeholdersEnabled = false,
            ),
        )
        return result as PagingSource.LoadResult.Page
    }
}

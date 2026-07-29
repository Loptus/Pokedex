package it.kata.pokedex.presentation.list

import androidx.paging.testing.asSnapshot
import it.kata.pokedex.utils.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Guards `initialLoadSize`. Paging defaults it to three times the page size, which here would
     * hand over all forty five entries at once and never append anything.
     *
     * Forty rather than twenty because Paging prefetches the following page as soon as the first
     * one is consumed: twenty loaded, twenty prefetched. The point of the assertion is that it is
     * well short of the whole list.
     */
    @Test
    fun `loads a page at a time instead of the whole list`() = runTest {
        val loaded = PokemonListViewModel().pokemon.asSnapshot()

        assertEquals(40, loaded.size)
    }

    @Test
    fun `appends the remaining pages as the list is scrolled to the end`() = runTest {
        val loaded = PokemonListViewModel().pokemon.asSnapshot {
            scrollTo(index = staticPokemon.lastIndex)
        }

        assertEquals(staticPokemon, loaded)
    }
}

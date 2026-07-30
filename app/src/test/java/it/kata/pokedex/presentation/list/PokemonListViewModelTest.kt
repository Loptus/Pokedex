package it.kata.pokedex.presentation.list

import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.data.remote.PokemonPagingSource
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.repository.PokemonRepository
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import it.kata.pokedex.utils.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = CountingRepository(total = 45)

    private fun viewModel() = PokemonListViewModel(GetPokemonPagingUseCase(repository))

    /**
     * Guards `initialLoadSize`. Paging defaults it to three times the page size, which here would
     * ask the API for sixty entries at once and, at two extra calls each, turn the first screen
     * into well over a hundred requests.
     *
     * Forty rather than twenty because Paging prefetches the following page as soon as the first
     * one is consumed. The point is that it is a couple of pages, not the whole list.
     */
    @Test
    fun `asks for one page at a time`() = runTest {
        viewModel().pokemon.asSnapshot()

        assertEquals(listOf(20, 20), repository.requestedLimits)
    }

    @Test
    fun `appends the remaining pages as the list is scrolled to the end`() = runTest {
        val loaded = viewModel().pokemon.asSnapshot { scrollTo(index = 44) }

        assertEquals(45, loaded.size)
        assertEquals(listOf(0, 20, 40), repository.requestedOffsets)
    }

    /**
     * Wraps the real [PokemonPagingSource] so the test covers the whole pipeline, and records the
     * windows it was asked for, which is what tells a page at a time from one big load.
     */
    private class CountingRepository(private val total: Int) : PokemonRepository {

        val requestedLimits = mutableListOf<Int>()
        val requestedOffsets = mutableListOf<Int>()

        override fun pokemonPagingSource(): PagingSource<Int, Pokemon> =
            PokemonPagingSource(::loadPage)

        private suspend fun loadPage(offset: Int, limit: Int): AppResult<PokemonPage> {
            requestedLimits += limit
            requestedOffsets += offset

            val items = (offset until minOf(offset + limit, total)).map { index ->
                Pokemon(
                    id = index,
                    name = "pokemon-$index",
                    imageUrl = null,
                    types = emptyList(),
                    description = "",
                )
            }
            return AppResult.Success(PokemonPage(items = items, hasMore = offset + limit < total))
        }
    }
}

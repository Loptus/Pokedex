package it.kata.pokedex.presentation.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.repository.PokemonRepository
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import it.kata.pokedex.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = RecordingRepository(total = 45)

    private fun viewModel() = PokemonListViewModel(GetPokemonPagingUseCase(repository))

    /**
     * Guards `initialLoadSize`. Paging defaults it to three times the page size, which would ask
     * the API for sixty entries at once and, at two extra calls each, turn the first screen into
     * well over a hundred requests.
     *
     * Two windows rather than one because Paging prefetches the following page as soon as the first
     * is consumed. The point is that it asks for pages, not for the whole list.
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

    @Test
    fun `the field follows every keystroke, with no waiting`() = runTest {
        val viewModel = viewModel()

        viewModel.onQueryChange("c")
        viewModel.onQueryChange("ch")

        assertEquals("ch", viewModel.uiState.value.query)
    }

    /**
     * The reason for the debounce: typing "char" must not fire a search for "c", "ch" and "cha" on
     * the way, each of which would cost a page of requests.
     */
    @Test
    fun `waits for the typing to settle before searching`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.pokemon.collect { } }
        runCurrent()

        viewModel.onQueryChange("c")
        advanceTimeBy(100)
        viewModel.onQueryChange("ch")
        advanceTimeBy(100)
        viewModel.onQueryChange("char")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(listOf("", "char"), repository.requestedQueries)
    }

    /** Clearing the field is not typing, so it should not sit on the timer. */
    @Test
    fun `clearing the query searches straight away`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.pokemon.collect { } }
        runCurrent()

        viewModel.onQueryChange("char")
        advanceTimeBy(400)
        viewModel.onQueryChange("")
        runCurrent()

        assertEquals(listOf("", "char", ""), repository.requestedQueries)
    }

    /**
     * A paging source of its own rather than the real one from the data module: this test is about
     * the windows and the queries the ViewModel and the use case ask for, and it has no business
     * reaching across into how the data layer fetches them.
     */
    private class RecordingRepository(private val total: Int) : PokemonRepository {

        val requestedLimits = mutableListOf<Int>()
        val requestedOffsets = mutableListOf<Int>()
        val requestedQueries = mutableListOf<String>()

        override fun pokemonPagingSource(query: String): PagingSource<Int, Pokemon> {
            requestedQueries += query
            return object : PagingSource<Int, Pokemon>() {

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
                    val offset = params.key ?: 0
                    requestedLimits += params.loadSize
                    requestedOffsets += offset

                    val items = (offset until minOf(offset + params.loadSize, total)).map { index ->
                        Pokemon(
                            id = index,
                            name = "pokemon-$index",
                            imageUrl = null,
                            types = emptyList(),
                            description = "",
                        )
                    }
                    val nextOffset = offset + params.loadSize

                    return LoadResult.Page(
                        data = items,
                        prevKey = null,
                        nextKey = if (nextOffset < total) nextOffset else null,
                    )
                }

                override fun getRefreshKey(state: PagingState<Int, Pokemon>): Int? = null
            }
        }
    }
}

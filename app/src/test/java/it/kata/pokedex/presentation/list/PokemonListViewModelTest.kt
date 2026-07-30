package it.kata.pokedex.presentation.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonQuery
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType
import it.kata.pokedex.domain.repository.PokemonRepository
import it.kata.pokedex.domain.usecase.GetPokemonPagingUseCase
import it.kata.pokedex.domain.usecase.GetPokemonUseCase
import it.kata.pokedex.utils.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = RecordingRepository(total = 45)

    private fun viewModel() = PokemonListViewModel(
        getPokemonPaging = GetPokemonPagingUseCase(repository),
        getPokemon = GetPokemonUseCase(repository),
    )

    /**
     * Guards `initialLoadSize`. Paging defaults it to three times the page size, which would hand
     * the screen sixty rows to fill on the first load instead of twenty.
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

    /** Paging the pointers must not drag their contents along with it. */
    @Test
    fun `paging does not fetch any row contents`() = runTest {
        viewModel().pokemon.asSnapshot { scrollTo(index = 44) }

        assertEquals(emptyList(), repository.requestedRows)
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
     * the way.
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

        assertEquals(listOf("", "char"), repository.requestedQueries.map { it.name })
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

        assertEquals(listOf("", "char", ""), repository.requestedQueries.map { it.name })
    }

    @Test
    fun `toggling a type adds it, and toggling it again takes it away`() = runTest {
        val viewModel = viewModel()

        viewModel.onTypeToggle(PokemonType.FIRE)
        viewModel.onTypeToggle(PokemonType.WATER)
        assertEquals(setOf(PokemonType.FIRE, PokemonType.WATER), viewModel.uiState.value.selectedTypes)

        viewModel.onTypeToggle(PokemonType.FIRE)
        assertEquals(setOf(PokemonType.WATER), viewModel.uiState.value.selectedTypes)
    }

    /**
     * Tapping a chip is one deliberate action, so it must not sit on the typing timer. This is why
     * the two halves of the query are debounced separately.
     */
    @Test
    fun `toggling a type filters straight away, with no debounce`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.pokemon.collect { } }
        runCurrent()

        viewModel.onTypeToggle(PokemonType.FIRE)
        runCurrent()

        assertEquals(
            listOf(emptySet(), setOf(PokemonType.FIRE)),
            repository.requestedQueries.map { it.types },
        )
    }

    /** The name is still debounced even while a type is on, and the two travel together. */
    @Test
    fun `a type and a name reach the repository as one query`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.pokemon.collect { } }
        runCurrent()

        viewModel.onTypeToggle(PokemonType.FIRE)
        viewModel.onQueryChange("char")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(
            PokemonQuery(name = "char", types = setOf(PokemonType.FIRE)),
            repository.requestedQueries.last(),
        )
    }

    @Test
    fun `a row starts out loading and fills in on its own`() = runTest {
        val viewModel = viewModel()
        val ref = PokemonRef(id = 7, name = "squirtle", detailUrl = "url/7")

        assertEquals(PokemonRowState.Loading, viewModel.rowState(id = 7).value)

        launch { viewModel.loadRow(ref) }
        advanceUntilIdle()

        val state = viewModel.rowState(id = 7).value
        assertIs<PokemonRowState.Loaded>(state)
        assertEquals("squirtle", state.pokemon.name)
    }

    /**
     * A row is composed again every time it comes back into view. Without the cache that would be
     * two requests each time.
     */
    @Test
    fun `fetches a row once however many times it comes back`() = runTest {
        val viewModel = viewModel()
        val ref = PokemonRef(id = 7, name = "squirtle", detailUrl = "url/7")

        repeat(3) {
            launch { viewModel.loadRow(ref) }
            advanceUntilIdle()
        }

        assertEquals(listOf(7), repository.requestedRows)
    }

    /**
     * The point of running the load in the caller's scope: when the row leaves the screen Compose
     * cancels it, and the row must not be left claiming to be loaded.
     */
    @Test
    fun `a cancelled row keeps nothing and is fetched again next time`() = runTest {
        repository.hold = CompletableDeferred()
        val viewModel = viewModel()
        val ref = PokemonRef(id = 7, name = "squirtle", detailUrl = "url/7")

        val scrolledPast = launch { viewModel.loadRow(ref) }
        runCurrent()
        scrolledPast.cancel()
        advanceUntilIdle()

        assertEquals(PokemonRowState.Loading, viewModel.rowState(id = 7).value)

        repository.hold = null
        launch { viewModel.loadRow(ref) }
        advanceUntilIdle()

        assertIs<PokemonRowState.Loaded>(viewModel.rowState(id = 7).value)
        assertEquals(listOf(7, 7), repository.requestedRows)
    }

    /** A failed row is not remembered as failed, so coming back into view tries again. */
    @Test
    fun `a failed row is retried when it comes back`() = runTest {
        repository.failingRowIds = setOf(7)
        val viewModel = viewModel()
        val ref = PokemonRef(id = 7, name = "squirtle", detailUrl = "url/7")

        launch { viewModel.loadRow(ref) }
        advanceUntilIdle()
        assertEquals(PokemonRowState.Failed, viewModel.rowState(id = 7).value)

        repository.failingRowIds = emptySet()
        launch { viewModel.loadRow(ref) }
        advanceUntilIdle()

        assertIs<PokemonRowState.Loaded>(viewModel.rowState(id = 7).value)
    }

    /**
     * A paging source of its own rather than the real one from the data module: this test is about
     * what the ViewModel and the use cases ask for, not about how the data layer fetches it.
     */
    private class RecordingRepository(private val total: Int) : PokemonRepository {

        val requestedLimits = mutableListOf<Int>()
        val requestedOffsets = mutableListOf<Int>()
        val requestedQueries = mutableListOf<PokemonQuery>()
        val requestedRows = mutableListOf<Int>()

        var failingRowIds: Set<Int> = emptySet()

        /** Set to keep a row's request pending, so a cancellation can land in the middle of it. */
        var hold: CompletableDeferred<Unit>? = null

        override suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon> {
            requestedRows += ref.id
            hold?.await()

            return if (ref.id in failingRowIds) {
                AppResult.Failure(IOException("no network"))
            } else {
                AppResult.Success(
                    Pokemon(
                        id = ref.id,
                        name = ref.name,
                        imageUrl = null,
                        types = emptyList(),
                        description = "Description of ${ref.name}",
                    ),
                )
            }
        }

        override fun pokemonPagingSource(query: PokemonQuery): PagingSource<Int, PokemonRef> {
            requestedQueries += query
            return object : PagingSource<Int, PokemonRef>() {

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PokemonRef> {
                    val offset = params.key ?: 0
                    requestedLimits += params.loadSize
                    requestedOffsets += offset

                    val items = (offset until minOf(offset + params.loadSize, total)).map { index ->
                        PokemonRef(id = index, name = "pokemon-$index", detailUrl = "url/$index")
                    }
                    val nextOffset = offset + params.loadSize

                    return LoadResult.Page(
                        data = items,
                        prevKey = null,
                        nextKey = if (nextOffset < total) nextOffset else null,
                    )
                }

                override fun getRefreshKey(state: PagingState<Int, PokemonRef>): Int? = null
            }
        }
    }
}

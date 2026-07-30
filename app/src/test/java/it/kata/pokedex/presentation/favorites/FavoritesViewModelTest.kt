package it.kata.pokedex.presentation.favorites

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonQuery
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import it.kata.pokedex.domain.usecase.GetPokemonUseCase
import it.kata.pokedex.domain.usecase.ObserveFavoritesUseCase
import it.kata.pokedex.domain.usecase.RemoveFavoriteUseCase
import it.kata.pokedex.presentation.common.PokemonRowLoader
import it.kata.pokedex.presentation.common.PokemonRowState
import it.kata.pokedex.utils.FakeFavoriteRepository
import it.kata.pokedex.utils.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val favorites = FakeFavoriteRepository()
    private val repository = StubPokemonRepository()

    private val bulbasaur = PokemonRef(id = 1, name = "bulbasaur", detailUrl = "url/1")
    private val charmander = PokemonRef(id = 4, name = "charmander", detailUrl = "url/4")

    private fun viewModel() = FavoritesViewModel(
        observeFavorites = ObserveFavoritesUseCase(favorites),
        rowLoader = PokemonRowLoader(GetPokemonUseCase(repository)),
        removeFavorite = RemoveFavoriteUseCase(favorites),
    )

    @Test
    fun `the saved entries reach the screen`() = runTest {
        favorites.saved.value = listOf(bulbasaur, charmander)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.favorites.collect { } }
        runCurrent()

        assertEquals(listOf(bulbasaur, charmander), viewModel.favorites.value)
    }

    @Test
    fun `removing takes the entry out of the list`() = runTest {
        favorites.saved.value = listOf(bulbasaur, charmander)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.favorites.collect { } }
        runCurrent()

        viewModel.onRemove(bulbasaur.id)
        advanceUntilIdle()

        assertEquals(listOf(1), favorites.removed)
        assertEquals(listOf(charmander), viewModel.favorites.value)
    }

    /**
     * The removal runs in the ViewModel's scope, not in the row's, and this is where it matters
     * most: removing a row is precisely what takes that row's composition away.
     */
    @Test
    fun `removing outlives the row that asked for it`() = runTest {
        favorites.hold = CompletableDeferred()
        favorites.saved.value = listOf(bulbasaur)
        val viewModel = viewModel()

        val row = launch { viewModel.onRemove(bulbasaur.id) }
        runCurrent()
        row.cancel()
        favorites.hold?.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(1), favorites.removed)
    }

    /** The saved entries arrive as pointers, so this page fetches its rows just like the list. */
    @Test
    fun `a row starts out loading and fills in on its own`() = runTest {
        val viewModel = viewModel()

        assertEquals(PokemonRowState.Loading, viewModel.rowState(bulbasaur.id).value)

        launch { viewModel.loadRow(bulbasaur) }
        advanceUntilIdle()

        val state = viewModel.rowState(bulbasaur.id).value
        assertIs<PokemonRowState.Loaded>(state)
        assertEquals("bulbasaur", state.pokemon.name)
        assertEquals(listOf(1), repository.requestedRows)
    }

    /** Only answers for single rows: this screen never pages, so paging is left unimplemented. */
    private class StubPokemonRepository : PokemonRepository {

        val requestedRows = mutableListOf<Int>()

        override suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon> {
            requestedRows += ref.id

            return AppResult.Success(
                Pokemon(
                    id = ref.id,
                    name = ref.name,
                    imageUrl = null,
                    types = emptyList(),
                    description = "Description of ${ref.name}",
                ),
            )
        }

        override fun pokemonPagingSource(query: PokemonQuery): PagingSource<Int, PokemonRef> =
            error("the favorites page does not page")
    }
}

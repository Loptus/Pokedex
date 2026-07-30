package it.kata.pokedex.presentation.list

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.presentation.common.PokemonRowState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for a crash that only happens once a LazyColumn is laid out: with the row count
 * and the row keys read from different snapshots, narrowing the search threw
 * `IndexOutOfBoundsException`. Typing "zera" was enough.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
// Robolectric has no image for SDK 37 yet, and the app targets it. Nothing here depends on the
// platform version, so pinning to the newest one Robolectric has is enough.
@Config(sdk = [36])
class PokemonListScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val query = MutableStateFlow("")
    private val saved = MutableStateFlow<Set<Int>>(emptySet())

    private val paged = combine(
        query.flatMapLatest { current ->
            Pager(
                config = PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { FilteredRefs(current) },
            ).flow
        },
        saved,
    ) { refs, favoriteIds ->
        refs.map { ref -> PokemonListItem(ref = ref, isFavorite = ref.id in favoriteIds) }
    }

    @Test
    fun `survives the search narrowing the list`() {
        showList()
        compose.onNodeWithText("Bulbasaur").assertIsDisplayed()

        query.value = "zera"
        compose.waitForIdle()

        compose.onNodeWithText("Zeraora").assertIsDisplayed()
    }

    @Test
    fun `survives the search emptying the list`() {
        showList()

        query.value = "zzz"
        compose.waitForIdle()

        compose.onNodeWithText("No Pokémon found").assertIsDisplayed()
    }

    /**
     * Note what this does not assert: where the list ends up.
     *
     * LazyColumn remembers its position by key, so after the list grows back the user is left
     * wherever the row they were looking at now sits, not at the top. That is Compose working as
     * documented rather than a bug, and pinning it down here would only make the test brittle, but
     * it is arguably the wrong thing for a search and worth deciding separately.
     */
    @Test
    fun `survives the search widening the list again`() {
        showList()

        query.value = "zera"
        compose.waitForIdle()
        query.value = ""
        compose.waitForIdle()

        compose.onNodeWithText("No Pokémon found").assertDoesNotExist()
    }

    /** Every row here is still loading, which is the case the heart must keep working in. */
    @Test
    fun `the heart works before the row's contents arrive`() {
        val toggled = mutableListOf<PokemonRef>()
        showList(onFavoriteToggle = { toggled += it })

        compose.onNodeWithContentDescription("Add Bulbasaur to favorites").performClick()

        assertEquals(listOf(1), toggled.map { it.id })
    }

    @Test
    fun `a saved row offers to remove it instead`() {
        saved.value = setOf(1)
        showList()

        compose.onNodeWithContentDescription("Remove Bulbasaur from favorites").assertIsDisplayed()
    }

    private fun showList(onFavoriteToggle: (PokemonRef) -> Unit = {}) {
        compose.setContent {
            val items = remember { paged }.collectAsLazyPagingItems()

            PokemonListScreen(
                pokemon = items,
                query = "",
                onQueryChange = {},
                selectedTypes = emptySet(),
                onTypeToggle = {},
                onFavoriteToggle = onFavoriteToggle,
                rowFor = { PokemonRowState.Loading },
            )
        }
        compose.waitForIdle()
    }

    private class FilteredRefs(private val query: String) : PagingSource<Int, PokemonRef>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PokemonRef> {
            val matches = allRefs.filter { it.name.contains(query, ignoreCase = true) }
            val offset = params.key ?: 0
            val nextOffset = offset + params.loadSize

            return LoadResult.Page(
                data = matches.drop(offset).take(params.loadSize),
                prevKey = null,
                nextKey = if (nextOffset < matches.size) nextOffset else null,
            )
        }

        override fun getRefreshKey(state: PagingState<Int, PokemonRef>): Int? = null

        private companion object {
            /** Eighteen entries, exactly one of which matches "zera". */
            val allRefs = listOf(
                "bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard",
                "squirtle", "wartortle", "blastoise", "caterpie", "metapod", "butterfree",
                "weedle", "kakuna", "beedrill", "pidgey", "pidgeotto", "zeraora",
            ).mapIndexed { index, name -> PokemonRef(id = index + 1, name = name, detailUrl = "url/${index + 1}") }
        }
    }
}

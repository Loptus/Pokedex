package it.kata.pokedex.data.repository

import it.kata.pokedex.core.AppResult
import it.kata.pokedex.data.remote.FakePokeApi
import it.kata.pokedex.data.remote.PokemonNameIndexDataSource
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.model.PokemonType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PokemonRepositoryImplTest {

    private val api = FakePokeApi()

    /**
     * The injected dispatcher has to share the scheduler `runTest` created, otherwise the two
     * clocks never agree and nothing ever completes.
     */
    private fun TestScope.repository() = PokemonRepositoryImpl(
        api = api,
        nameIndex = PokemonNameIndexDataSource(api),
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `builds a page out of the list, the details and the species`() = runTest {
        api.allNames = listOf("bulbasaur", "charizard")

        val page = repository().getPage(query = "", offset = 0, limit = 20).valueOrFail()

        assertEquals(listOf("bulbasaur", "charizard"), page.items.map { it.name })
        assertEquals(listOf(PokemonType.GRASS), page.items.first().types)
        assertEquals("Description of bulbasaur.", page.items.first().description)
    }

    /**
     * The whole point of the parallel load: twenty entries must not cost twenty round trips one
     * after the other.
     */
    @Test
    fun `loads the details of a page in parallel`() = runTest {
        api.allNames = List(20) { "pokemon-$it" }

        repository().getPage(query = "", offset = 0, limit = 20).valueOrFail()

        assertTrue(
            api.maxConcurrentDetailCalls > 1,
            "details were fetched one at a time (max concurrency ${api.maxConcurrentDetailCalls})",
        )
    }

    @Test
    fun `reports hasMore straight from the api instead of guessing from the size`() = runTest {
        api.allNames = listOf("bulbasaur")

        assertEquals(false, repository().getPage("", offset = 0, limit = 20).valueOrFail().hasMore)
    }

    /** One row without its description is better than a page that refuses to load. */
    @Test
    fun `keeps the entry when only its description fails`() = runTest {
        api.allNames = listOf("bulbasaur")
        api.failingSpeciesIds = setOf(1)

        val page = repository().getPage("", offset = 0, limit = 20).valueOrFail()

        assertEquals(1, page.items.size)
        assertEquals("", page.items.first().description)
    }

    @Test
    fun `drops an entry whose detail cannot be parsed and keeps the others`() = runTest {
        api.allNames = listOf("bulbasaur", "broken", "charizard")

        val page = repository().getPage("", offset = 0, limit = 20).valueOrFail()

        assertEquals(listOf("bulbasaur", "charizard"), page.items.map { it.name })
    }

    @Test
    fun `turns a failing list call into a failure instead of throwing`() = runTest {
        api.failListCall = true

        val result = repository().getPage("", offset = 0, limit = 20)

        assertIs<AppResult.Failure>(result)
        assertIs<IOException>(result.cause)
    }

    @Test
    fun `passes the window it was asked for straight through`() = runTest {
        api.allNames = emptyList()

        repository().getPage("", offset = 40, limit = 20)

        assertEquals(40, api.lastOffset)
        assertEquals(20, api.lastLimit)
    }

    @Test
    fun `a query narrows the page down to the matching names`() = runTest {
        api.allNames = listOf("bulbasaur", "charmander", "charmeleon", "charizard")

        val page = repository().getPage("char", offset = 0, limit = 20).valueOrFail()

        assertEquals(listOf("charmander", "charmeleon", "charizard"), page.items.map { it.name })
        assertEquals(false, page.hasMore)
    }

    /** A search pages through the matches held in memory, so it has to slice them. */
    @Test
    fun `a search pages through its matches`() = runTest {
        api.allNames = List(30) { "char-$it" }
        val repository = repository()

        val firstPage = repository.getPage("char", offset = 0, limit = 20).valueOrFail()
        val secondPage = repository.getPage("char", offset = 20, limit = 20).valueOrFail()

        assertEquals(20, firstPage.items.size)
        assertEquals(true, firstPage.hasMore)
        assertEquals(10, secondPage.items.size)
        assertEquals(false, secondPage.hasMore)
    }

    @Test
    fun `a query that matches nothing gives an empty page, not a failure`() = runTest {
        api.allNames = listOf("bulbasaur", "charizard")

        val page = repository().getPage("zzz", offset = 0, limit = 20).valueOrFail()

        assertEquals(emptyList(), page.items)
        assertEquals(false, page.hasMore)
    }

    private fun AppResult<PokemonPage>.valueOrFail(): PokemonPage {
        assertIs<AppResult.Success<PokemonPage>>(this)
        return value
    }
}

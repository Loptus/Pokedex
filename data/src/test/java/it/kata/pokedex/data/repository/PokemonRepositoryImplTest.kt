package it.kata.pokedex.data.repository

import it.kata.pokedex.core.AppResult
import it.kata.pokedex.data.remote.FakePokeApi
import it.kata.pokedex.data.remote.PokemonNameIndexDataSource
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun `a page is a slice of the index`() = runTest {
        api.allNames = listOf("bulbasaur", "charmander", "charizard")

        val page = repository().getPage(query = "", offset = 1, limit = 20).valueOrFail()

        assertEquals(listOf("charmander", "charizard"), page.items.map { it.name })
        assertEquals(false, page.hasMore)
    }

    /**
     * The whole reason the list is paged as pointers: turning a page must not fetch anything. The
     * two requests a row costs are paid when that row is on screen, by [PokemonRepositoryImpl.pokemon].
     */
    @Test
    fun `a page costs no detail and no species requests`() = runTest {
        api.allNames = List(20) { "pokemon-$it" }

        repository().getPage(query = "", offset = 0, limit = 20).valueOrFail()

        assertEquals(0, api.detailCalls)
        assertEquals(0, api.speciesCalls)
    }

    @Test
    fun `fetches the index once, however many pages are asked for`() = runTest {
        api.allNames = List(60) { "pokemon-$it" }
        val repository = repository()

        repository.getPage("", offset = 0, limit = 20).valueOrFail()
        repository.getPage("", offset = 20, limit = 20).valueOrFail()
        repository.getPage("char", offset = 0, limit = 20).valueOrFail()

        assertEquals(1, api.indexCalls)
    }

    @Test
    fun `pages through the whole list a window at a time`() = runTest {
        api.allNames = List(30) { "pokemon-$it" }
        val repository = repository()

        val firstPage = repository.getPage("", offset = 0, limit = 20).valueOrFail()
        val secondPage = repository.getPage("", offset = 20, limit = 20).valueOrFail()

        assertEquals("pokemon-0", firstPage.items.first().name)
        assertEquals(true, firstPage.hasMore)
        assertEquals("pokemon-20", secondPage.items.first().name)
        assertEquals(false, secondPage.hasMore)
    }

    @Test
    fun `a query narrows the page down to the matching names`() = runTest {
        api.allNames = listOf("bulbasaur", "charmander", "charmeleon", "charizard")

        val page = repository().getPage("char", offset = 0, limit = 20).valueOrFail()

        assertEquals(listOf("charmander", "charmeleon", "charizard"), page.items.map { it.name })
    }

    @Test
    fun `a query that matches nothing gives an empty page, not a failure`() = runTest {
        api.allNames = listOf("bulbasaur", "charizard")

        val page = repository().getPage("zzz", offset = 0, limit = 20).valueOrFail()

        assertEquals(emptyList(), page.items)
        assertEquals(false, page.hasMore)
    }

    @Test
    fun `turns a failing index call into a failure instead of throwing`() = runTest {
        api.failIndexCall = true

        val result = repository().getPage("", offset = 0, limit = 20)

        assertIs<AppResult.Failure>(result)
        assertIs<IOException>(result.cause)
    }

    @Test
    fun `builds a row out of its detail and its description`() = runTest {
        api.allNames = listOf("bulbasaur")

        val pokemon = repository().pokemon(PokemonRef(id = 1, name = "bulbasaur")).valueOrFail()

        assertEquals("bulbasaur", pokemon.name)
        assertEquals(listOf(PokemonType.GRASS), pokemon.types)
        assertEquals("artwork/bulbasaur.png", pokemon.imageUrl)
        assertEquals("Description of bulbasaur.", pokemon.description)
    }

    /**
     * The id is known from the index, so the two requests a row needs do not have to queue behind
     * each other: one round trip instead of two, for every row on screen.
     */
    @Test
    fun `fetches a row's detail and description at the same time`() = runTest {
        api.allNames = listOf("bulbasaur")

        repository().pokemon(PokemonRef(id = 1, name = "bulbasaur")).valueOrFail()

        assertEquals(true, api.detailAndSpeciesOverlapped)
    }

    @Test
    fun `reports a failing row instead of throwing`() = runTest {
        api.allNames = listOf("bulbasaur")
        api.failingSpeciesIds = setOf(1)

        val result = repository().pokemon(PokemonRef(id = 1, name = "bulbasaur"))

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `reports a row whose detail cannot be read instead of returning a half built one`() = runTest {
        api.allNames = listOf("broken")

        val result = repository().pokemon(PokemonRef(id = 1, name = "broken"))

        assertIs<AppResult.Failure>(result)
    }

    private fun AppResult<PokemonPage>.valueOrFail(): PokemonPage {
        assertIs<AppResult.Success<PokemonPage>>(this)
        return value
    }

    private fun AppResult<Pokemon>.valueOrFail(): Pokemon {
        assertIs<AppResult.Success<Pokemon>>(this)
        return value
    }
}

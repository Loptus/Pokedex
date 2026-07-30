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
     * two requests a row costs are paid when that row is on screen.
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
        val ref = firstRef()

        val pokemon = repository().pokemon(ref).valueOrFail()

        assertEquals("bulbasaur", pokemon.name)
        assertEquals(listOf(PokemonType.GRASS), pokemon.types)
        assertEquals("artwork/bulbasaur.png", pokemon.imageUrl)
        assertEquals("Description of bulbasaur.", pokemon.description)
    }

    /**
     * The bug this guards against: from id 10001 onwards the entries are alternate forms, and their
     * species sits under a different, much lower id. Anything that builds the species address out of
     * the Pokemon's own id gets a 404 for every one of them, which blanked the whole tail of the
     * list. Following the url inside the detail is the only thing that works.
     */
    @Test
    fun `follows the species link of an alternate form instead of guessing it`() = runTest {
        api.entries = listOf(FakePokeApi.Entry(id = 10001, name = "deoxys-attack", speciesId = 386))
        val ref = firstRef()

        val pokemon = repository().pokemon(ref).valueOrFail()

        assertEquals("Description of deoxys-attack.", pokemon.description)
    }

    @Test
    fun `follows the detail link of an alternate form instead of guessing it`() = runTest {
        api.entries = listOf(FakePokeApi.Entry(id = 10001, name = "deoxys-attack", speciesId = 386))

        val ref = firstRef()

        assertEquals("https://pokeapi.co/api/v2/pokemon/10001/", ref.detailUrl)
        assertEquals(10001, repository().pokemon(ref).valueOrFail().id)
    }

    /** A row without its description is still worth showing: it has artwork, name and types. */
    @Test
    fun `keeps the row when only its description fails`() = runTest {
        api.allNames = listOf("bulbasaur")
        api.failingSpeciesUrls = setOf("https://pokeapi.co/api/v2/pokemon-species/1/")

        val pokemon = repository().pokemon(firstRef()).valueOrFail()

        assertEquals("bulbasaur", pokemon.name)
        assertEquals("", pokemon.description)
    }

    @Test
    fun `reports a row whose detail cannot be read instead of returning a half built one`() = runTest {
        api.allNames = listOf("broken")

        val result = repository().pokemon(firstRef())

        assertIs<AppResult.Failure>(result)
    }

    /** Refs always come from the index, so the test takes them from there too. */
    private suspend fun TestScope.firstRef(): PokemonRef =
        repository().getPage("", offset = 0, limit = 20).valueOrFail().items.first()

    private fun AppResult<PokemonPage>.valueOrFail(): PokemonPage {
        assertIs<AppResult.Success<PokemonPage>>(this)
        return value
    }

    private fun AppResult<Pokemon>.valueOrFail(): Pokemon {
        assertIs<AppResult.Success<Pokemon>>(this)
        return value
    }
}

package it.kata.pokedex.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PokemonNameIndexDataSourceTest {

    private val api = FakePokeApi().apply {
        allNames = listOf("bulbasaur", "charmander", "charmeleon", "charizard", "squirtle")
    }
    private val index = PokemonNameIndexDataSource(api)

    @Test
    fun `matches anywhere in the name, not just at the start`() = runTest {
        assertEquals(listOf("charmander", "charmeleon", "charizard"), index.namesMatching("char"))
        assertEquals(listOf("bulbasaur", "squirtle"), index.namesMatching("ur").plus(index.namesMatching("squir")))
    }

    @Test
    fun `ignores case, because the user types lowercase and the api stores lowercase`() = runTest {
        assertEquals(listOf("charizard"), index.namesMatching("CHARIZARD"))
    }

    @Test
    fun `keeps the api order, so results stay by pokedex number`() = runTest {
        assertEquals(listOf("charmander", "charmeleon", "charizard"), index.namesMatching("char"))
    }

    @Test
    fun `returns nothing when no name matches`() = runTest {
        assertEquals(emptyList(), index.namesMatching("zzz"))
    }

    /** The whole point of the index: one download, however many searches follow. */
    @Test
    fun `downloads the index only once`() = runTest {
        index.namesMatching("char")
        index.namesMatching("saur")
        index.namesMatching("turtle")

        assertEquals(1, api.indexCalls)
    }

    /**
     * Several rows can ask at the same time on a fast scroll. The lock has to make the latecomers
     * wait for the first download rather than each firing one of their own.
     */
    @Test
    fun `concurrent searches share a single download`() = runTest {
        coroutineScope {
            List(5) { async { index.namesMatching("char") } }.awaitAll()
        }

        assertEquals(1, api.indexCalls)
    }

    @Test
    fun `tries again after a failed download instead of caching the emptiness`() = runTest {
        api.failIndexCall = true
        assertFailsWith<IOException> { index.namesMatching("char") }

        api.failIndexCall = false

        assertEquals(listOf("charmander", "charmeleon", "charizard"), index.namesMatching("char"))
    }
}

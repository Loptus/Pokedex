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
        assertEquals(listOf("charmander", "charmeleon", "charizard"), index.matching("char").map { it.name })
        assertEquals(listOf("bulbasaur"), index.matching("ur").map { it.name })
    }

    @Test
    fun `ignores case, because the user types lowercase and the api stores lowercase`() = runTest {
        assertEquals(listOf("charizard"), index.matching("CHARIZARD").map { it.name })
    }

    @Test
    fun `keeps the api order, so results stay by pokedex number`() = runTest {
        assertEquals(listOf("charmander", "charmeleon", "charizard"), index.matching("char").map { it.name })
    }

    @Test
    fun `returns nothing when no name matches`() = runTest {
        assertEquals(emptyList(), index.matching("zzz"))
    }

    @Test
    fun `a blank query is what plain browsing asks for, so it matches everything`() = runTest {
        assertEquals(5, index.matching("").size)
        assertEquals(5, index.matching("   ").size)
    }

    /** The id only exists in the url, and a row needs it to fetch its description. */
    @Test
    fun `reads the id out of the url`() = runTest {
        assertEquals(listOf(2, 3, 4), index.matching("char").map { it.id })
    }

    /** The url is kept as the API published it, never rebuilt from the id. */
    @Test
    fun `keeps the detail url of each entry`() = runTest {
        assertEquals(
            "https://pokeapi.co/api/v2/pokemon/4/",
            index.matching("charizard").single().detailUrl,
        )
    }

    /** The whole point of the index: one download, however many searches follow. */
    @Test
    fun `downloads the index only once`() = runTest {
        index.matching("char")
        index.matching("saur")
        index.matching("turtle")

        assertEquals(1, api.indexCalls)
    }

    /**
     * Several rows can ask at the same time on a fast scroll. The lock has to make the latecomers
     * wait for the first download rather than each firing one of their own.
     */
    @Test
    fun `concurrent searches share a single download`() = runTest {
        coroutineScope {
            List(5) { async { index.matching("char") } }.awaitAll()
        }

        assertEquals(1, api.indexCalls)
    }

    @Test
    fun `tries again after a failed download instead of caching the emptiness`() = runTest {
        api.failIndexCall = true
        assertFailsWith<IOException> { index.matching("char") }

        api.failIndexCall = false

        assertEquals(listOf("charmander", "charmeleon", "charizard"), index.matching("char").map { it.name })
    }
}

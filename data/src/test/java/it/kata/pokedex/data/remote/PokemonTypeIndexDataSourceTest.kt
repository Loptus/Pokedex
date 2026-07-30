package it.kata.pokedex.data.remote

import it.kata.pokedex.domain.model.PokemonType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PokemonTypeIndexDataSourceTest {

    private val api = FakePokeApi().apply {
        typeMembers = mapOf(
            "fire" to setOf("charmander", "charizard"),
            "flying" to setOf("charizard", "pidgey"),
            "water" to setOf("squirtle"),
        )
    }
    private val index = PokemonTypeIndexDataSource(api)

    @Test
    fun `gives the members of a single type`() = runTest {
        assertEquals(
            setOf("charmander", "charizard"),
            index.namesOfAnyOf(setOf(PokemonType.FIRE)),
        )
    }

    /**
     * Union, not intersection: picking Fire and Water has to show both, otherwise a filter that can
     * only ever return nothing looks broken rather than empty.
     */
    @Test
    fun `several types are a union, and a shared member appears once`() = runTest {
        assertEquals(
            setOf("charmander", "charizard", "pidgey"),
            index.namesOfAnyOf(setOf(PokemonType.FIRE, PokemonType.FLYING)),
        )
    }

    @Test
    fun `no types selected asks the api for nothing`() = runTest {
        assertEquals(emptySet(), index.namesOfAnyOf(emptySet()))
        assertEquals(0, api.typeCalls)
    }

    @Test
    fun `fetches each type only once, however often it is used`() = runTest {
        index.namesOfAnyOf(setOf(PokemonType.FIRE))
        index.namesOfAnyOf(setOf(PokemonType.FIRE, PokemonType.FLYING))
        index.namesOfAnyOf(setOf(PokemonType.FLYING))

        assertEquals(2, api.typeCalls)
    }

    /** Rows arriving during the first fetch have to wait for it, not start one of their own. */
    @Test
    fun `concurrent callers share a single fetch`() = runTest {
        coroutineScope {
            List(5) { async { index.namesOfAnyOf(setOf(PokemonType.FIRE)) } }.awaitAll()
        }

        assertEquals(1, api.typeCalls)
    }

    @Test
    fun `tries again after a failure instead of caching the emptiness`() = runTest {
        api.failingTypes = setOf("fire")
        assertFailsWith<IOException> { index.namesOfAnyOf(setOf(PokemonType.FIRE)) }

        api.failingTypes = emptySet()

        assertEquals(
            setOf("charmander", "charizard"),
            index.namesOfAnyOf(setOf(PokemonType.FIRE)),
        )
    }
}

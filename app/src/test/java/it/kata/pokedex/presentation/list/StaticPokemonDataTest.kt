package it.kata.pokedex.presentation.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticPokemonDataTest {

    /**
     * The screen looks a description up by id, so a Pokemon without one renders an empty
     * paragraph. Cheap invariant to check, and the kind of gap nobody notices by scrolling.
     */
    @Test
    fun `every pokemon has a description`() {
        staticPokemon.forEach { pokemon ->
            assertTrue(pokemon.id in staticDescriptions, "${pokemon.name} has no description")
        }
    }

    @Test
    fun `ids are unique, so the list keys stay stable`() {
        assertEquals(staticPokemon.size, staticPokemon.map { it.id }.toSet().size)
    }

    @Test
    fun `there is more than one page of data to page through`() {
        assertTrue(staticPokemon.size > 20, "only ${staticPokemon.size} entries, paging never appends")
    }
}

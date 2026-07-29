package it.kata.pokedex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PokemonTypeTest {

    @Test
    fun `covers the eighteen official types`() {
        assertEquals(18, PokemonType.entries.size)
    }

    @Test
    fun `resolves every type from its own api name`() {
        PokemonType.entries.forEach { type ->
            assertEquals(type, PokemonType.fromApiName(type.apiName))
        }
    }

    @Test
    fun `ignores case and surrounding blanks`() {
        assertEquals(PokemonType.FIRE, PokemonType.fromApiName(" Fire "))
        assertEquals(PokemonType.FAIRY, PokemonType.fromApiName("FAIRY"))
    }

    @Test
    fun `returns null for a type the app does not know`() {
        assertNull(PokemonType.fromApiName("stellar"))
        assertNull(PokemonType.fromApiName(""))
    }
}

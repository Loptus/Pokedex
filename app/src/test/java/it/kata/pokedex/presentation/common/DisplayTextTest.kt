package it.kata.pokedex.presentation.common

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayTextTest {

    @Test
    fun `capitalises the type label coming from the api`() {
        assertEquals("Fire", PokemonType.FIRE.label)
        assertEquals("Fighting", PokemonType.FIGHTING.label)
    }

    @Test
    fun `capitalises the pokemon name coming from the api`() {
        assertEquals("Bulbasaur", refNamed("bulbasaur").displayName)
    }

    @Test
    fun `leaves hyphenated names alone past the first letter`() {
        assertEquals("Nidoran-f", refNamed("nidoran-f").displayName)
        assertEquals("Mr-mime", refNamed("mr-mime").displayName)
    }

    @Test
    fun `does not choke on an empty name`() {
        assertEquals("", refNamed("").displayName)
    }

    private fun refNamed(name: String) = PokemonRef(id = 1, name = name)
}

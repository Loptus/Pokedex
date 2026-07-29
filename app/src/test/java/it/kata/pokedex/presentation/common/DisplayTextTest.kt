package it.kata.pokedex.presentation.common

import it.kata.pokedex.domain.model.Pokemon
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
        assertEquals("Bulbasaur", pokemonNamed("bulbasaur").displayName)
    }

    @Test
    fun `leaves hyphenated names alone past the first letter`() {
        assertEquals("Nidoran-f", pokemonNamed("nidoran-f").displayName)
        assertEquals("Mr-mime", pokemonNamed("mr-mime").displayName)
    }

    @Test
    fun `does not choke on an empty name`() {
        assertEquals("", pokemonNamed("").displayName)
    }

    private fun pokemonNamed(name: String) = Pokemon(
        id = 1,
        name = name,
        imageUrl = null,
        types = emptyList(),
    )
}

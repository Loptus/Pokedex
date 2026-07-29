package it.kata.pokedex.presentation.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PokemonListViewModelTest {

    @Test
    fun `exposes the list as soon as it is created`() {
        val state = PokemonListViewModel().uiState.value

        assertEquals(staticPokemon, state.pokemon)
    }

    /**
     * The screen looks a description up by id, so a Pokemon without one would render an empty
     * paragraph. Cheap invariant to check, and it is the kind of gap nobody notices by scrolling.
     */
    @Test
    fun `has a description for every pokemon it shows`() {
        val state = PokemonListViewModel().uiState.value

        state.pokemon.forEach { pokemon ->
            assertTrue(
                pokemon.id in state.descriptions,
                "${pokemon.name} has no description",
            )
        }
    }
}

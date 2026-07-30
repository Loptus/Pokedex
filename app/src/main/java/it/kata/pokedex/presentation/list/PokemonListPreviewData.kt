package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonType

/**
 * Fixtures for the @Preview functions only.
 *
 * The descriptions are written here rather than copied from the real Pokedex entries: the app pulls
 * the genuine text from the API, so there is no reason to bake game text into the repo.
 */
internal val previewPokemon = listOf(
    Pokemon(
        id = 1,
        name = "bulbasaur",
        imageUrl = null,
        types = listOf(PokemonType.GRASS, PokemonType.POISON),
        description = "A seed on its back grows larger as it soaks up the sun.",
    ),
    Pokemon(
        id = 6,
        name = "charizard",
        imageUrl = null,
        types = listOf(PokemonType.FIRE, PokemonType.FLYING),
        description = "It flies high enough to look for opponents worth its time.",
    ),
    Pokemon(
        id = 25,
        name = "pikachu",
        imageUrl = null,
        types = listOf(PokemonType.ELECTRIC),
        description = "It stores energy in its cheeks and lets it go when startled.",
    ),
)

package it.kata.pokedex.presentation.list

import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonType

/**
 * Fixtures for the @Preview functions, and the temporary source of the screen until the ViewModel
 * lands.
 *
 * The descriptions are written here rather than copied from the real Pokedex entries: the app pulls
 * the genuine text from the API at runtime, so there is no reason to bake game text into the repo.
 */
val samplePokemon = listOf(
    Pokemon(
        id = 1,
        name = "bulbasaur",
        imageUrl = null,
        types = listOf(PokemonType.GRASS, PokemonType.POISON),
    ),
    Pokemon(
        id = 405,
        name = "luxray",
        imageUrl = null,
        types = listOf(PokemonType.ELECTRIC),
    ),
    Pokemon(
        id = 90,
        name = "shellder",
        imageUrl = null,
        types = listOf(PokemonType.WATER),
    ),
    Pokemon(
        id = 384,
        name = "rayquaza",
        imageUrl = null,
        types = listOf(PokemonType.DRAGON, PokemonType.FLYING),
    ),
    Pokemon(
        id = 316,
        name = "gulpin",
        imageUrl = null,
        types = listOf(PokemonType.POISON),
    ),
)

val sampleDescriptions = mapOf(
    1 to "A seed sits on its back from birth, and grows larger as this Pokemon takes in sunlight.",
    405 to "Its eyes pick out shapes behind thick walls, which makes it a tireless tracker.",
    90 to "It keeps its soft body sealed inside a shell that very few attacks can even scratch.",
    384 to "It is said to glide through the upper atmosphere, coming down to the ground only rarely.",
    316 to "Almost all of its body is stomach, and little of what it swallows survives for long.",
)

package it.kata.pokedex.presentation.common

import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType

/**
 * The descriptions are written here rather than copied from the real Pokedex entries: the genuine
 * text comes from the API at runtime, so there is no reason to bake game text into the repo.
 */
internal val previewRefs = listOf(
    PokemonRef(id = 1, name = "bulbasaur", detailUrl = ""),
    PokemonRef(id = 6, name = "charizard", detailUrl = ""),
    PokemonRef(id = 25, name = "pikachu", detailUrl = ""),
)

internal fun previewLoaded(ref: PokemonRef): PokemonRowState.Loaded =
    PokemonRowState.Loaded(previewPokemon.getValue(ref.id))

private val previewPokemon = mapOf(
    1 to Pokemon(
        id = 1,
        name = "bulbasaur",
        imageUrl = null,
        types = listOf(PokemonType.GRASS, PokemonType.POISON),
        description = "A seed on its back grows larger as it soaks up the sun.",
    ),
    6 to Pokemon(
        id = 6,
        name = "charizard",
        imageUrl = null,
        types = listOf(PokemonType.FIRE, PokemonType.FLYING),
        description = "It flies high enough to look for opponents worth its time.",
    ),
    25 to Pokemon(
        id = 25,
        name = "pikachu",
        imageUrl = null,
        types = listOf(PokemonType.ELECTRIC),
        description = "It stores energy in its cheeks and lets it go when startled.",
    ),
)

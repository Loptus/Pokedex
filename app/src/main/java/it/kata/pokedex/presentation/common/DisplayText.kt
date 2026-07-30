package it.kata.pokedex.presentation.common

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType

/**
 * Not translated on purpose: these are values coming from the API, not interface text, and a table
 * per language would drift from the vocabulary the API itself uses for searching.
 */
val PokemonType.label: String get() = apiName.capitalizeFirst()

val PokemonRef.displayName: String get() = name.capitalizeFirst()

private fun String.capitalizeFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

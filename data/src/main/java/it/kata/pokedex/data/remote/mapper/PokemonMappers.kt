package it.kata.pokedex.data.remote.mapper

import it.kata.pokedex.data.remote.dto.NamedResourceDto
import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType

private const val ENGLISH = "en"

/** The index gives urls like `https://pokeapi.co/api/v2/pokemon/25/`, and the id is the last segment. */
private val TRAILING_ID = Regex("""/(\d+)/?$""")

/** Collapses the newlines, form feeds and double spaces the API ships inside the flavour text. */
private val WHITESPACE = Regex("\\s+")

/** The id is read out of the url only to key the list row, never to build another address. */
fun NamedResourceDto.toRef(): PokemonRef? {
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val url = url?.takeIf { it.isNotBlank() } ?: return null
    val id = TRAILING_ID.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return null

    return PokemonRef(id = id, name = name, detailUrl = url)
}

/** Null instead of an exception, so one broken entry does not take the whole page down with it. */
fun PokemonDetailDto.toDomain(description: String): Pokemon? {
    val id = id ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null

    return Pokemon(
        id = id,
        name = name,
        imageUrl = artworkUrl(),
        types = types.orEmpty().mapNotNull { slot ->
            slot.type?.name?.let(PokemonType::fromApiName)
        },
        description = description,
    )
}

/**
 * The text arrives wrapped for the text boxes of the games, with literal `\n` and `\f` inside, so
 * rendering it untouched would break a line in the middle of a word.
 */
fun PokemonSpeciesDto.toDescription(): String =
    flavorTextEntries.orEmpty()
        .firstOrNull { it.language?.name == ENGLISH }
        ?.flavorText
        ?.replace(WHITESPACE, " ")
        ?.trim()
        .orEmpty()

/** Official artwork first because it is the large, clean image; the sprite is the fallback. */
private fun PokemonDetailDto.artworkUrl(): String? =
    sprites?.other?.officialArtwork?.frontDefault?.takeIf { it.isNotBlank() }
        ?: sprites?.frontDefault?.takeIf { it.isNotBlank() }

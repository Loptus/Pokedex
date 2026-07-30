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

/**
 * Turns an index entry into a pointer, or null when it is unusable.
 *
 * The id comes out of the url because that is the only place the index carries it, and having it up
 * front is what lets a row ask for its detail and its description at the same time instead of one
 * after the other.
 */
fun NamedResourceDto.toRef(): PokemonRef? {
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val id = url?.let { TRAILING_ID.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: return null

    return PokemonRef(id = id, name = name)
}

/**
 * Turns a detail payload into the domain model, or null when the entry is unusable.
 *
 * Returning null instead of throwing keeps one broken entry from taking down the whole page: the
 * caller drops it and the other nineteen still reach the screen.
 */
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
 * Picks the first English entry and cleans it up.
 *
 * The API stores the text as it appeared in the games, wrapped for their text boxes, so it arrives
 * with literal `\n` and `\f` inside. Rendering it untouched would break the line in the middle of
 * a word.
 */
fun PokemonSpeciesDto.toDescription(): String =
    flavorTextEntries.orEmpty()
        .firstOrNull { it.language?.name == ENGLISH }
        ?.flavorText
        ?.replace(WHITESPACE, " ")
        ?.trim()
        .orEmpty()

/**
 * Official artwork first because it is the large, clean image; the small sprite is the fallback.
 * Both can be missing, and the model allows that.
 */
private fun PokemonDetailDto.artworkUrl(): String? =
    sprites?.other?.officialArtwork?.frontDefault?.takeIf { it.isNotBlank() }
        ?: sprites?.frontDefault?.takeIf { it.isNotBlank() }

package it.kata.pokedex.data.remote

import it.kata.pokedex.data.remote.dto.ArtworkDto
import it.kata.pokedex.data.remote.dto.FlavorTextEntryDto
import it.kata.pokedex.data.remote.dto.NamedResourceDto
import it.kata.pokedex.data.remote.dto.OtherSpritesDto
import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonIndexDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import it.kata.pokedex.data.remote.dto.SpritesDto
import it.kata.pokedex.data.remote.dto.TypeDto
import it.kata.pokedex.data.remote.dto.TypeMemberDto
import it.kata.pokedex.data.remote.dto.TypeSlotDto
import java.io.IOException

/**
 * Stands in for the network, and answers only the urls it published, exactly like the real one.
 *
 * That is the point of it: a url this fake never handed out is a 404, so any code that assembles an
 * address instead of following one fails here too. [Entry.speciesUrl] deliberately does not have to
 * match the pokemon's own id, which is what alternate forms do.
 */
class FakePokeApi : PokeApi {

    /** An entry as the API publishes it: a detail url, and a species url the detail points at. */
    data class Entry(
        val id: Int,
        val name: String,
        val speciesId: Int = id,
        val type: String = "fire",
    ) {
        val detailUrl: String get() = "https://pokeapi.co/api/v2/pokemon/$id/"
        val speciesUrl: String get() = "https://pokeapi.co/api/v2/pokemon-species/$speciesId/"
    }

    var entries: List<Entry> = emptyList()
    var failIndexCall: Boolean = false
    var failingSpeciesUrls: Set<String> = emptySet()

    /** Members of each type, keyed by the api name of the type. */
    var typeMembers: Map<String, Set<String>> = emptyMap()
    var failingTypes: Set<String> = emptySet()

    var indexCalls: Int = 0
        private set
    var typeCalls: Int = 0
        private set
    var detailCalls: Int = 0
        private set
    var speciesCalls: Int = 0
        private set

    /** Convenience for the many tests that only care about names. */
    var allNames: List<String>
        get() = entries.map { it.name }
        set(value) {
            entries = value.mapIndexed { index, name ->
                Entry(id = index + 1, name = name, type = if (name == "bulbasaur") "grass" else "fire")
            }
        }

    override suspend fun getPokemonIndex(limit: Int): PokemonIndexDto {
        indexCalls++
        if (failIndexCall) throw IOException("no network")

        return PokemonIndexDto(
            results = entries.map { NamedResourceDto(name = it.name, url = it.detailUrl) },
        )
    }

    override suspend fun getPokemonDetail(url: String): PokemonDetailDto {
        detailCalls++
        val entry = entries.firstOrNull { it.detailUrl == url } ?: throw IOException("404 $url")

        if (entry.name == "broken") return PokemonDetailDto(id = null, name = entry.name)

        return PokemonDetailDto(
            id = entry.id,
            name = entry.name,
            types = listOf(TypeSlotDto(NamedResourceDto(name = entry.type))),
            sprites = SpritesDto(
                frontDefault = "front/${entry.name}.png",
                other = OtherSpritesDto(ArtworkDto("artwork/${entry.name}.png")),
            ),
            species = NamedResourceDto(name = entry.name, url = entry.speciesUrl),
        )
    }

    override suspend fun getType(name: String): TypeDto {
        typeCalls++
        if (name in failingTypes) throw IOException("type unavailable")

        return TypeDto(
            pokemon = typeMembers[name].orEmpty().map {
                TypeMemberDto(NamedResourceDto(name = it, url = "url/$it"))
            },
        )
    }

    override suspend fun getPokemonSpecies(url: String): PokemonSpeciesDto {
        speciesCalls++
        if (url in failingSpeciesUrls) throw IOException("species unavailable")

        val entry = entries.firstOrNull { it.speciesUrl == url } ?: throw IOException("404 $url")

        return PokemonSpeciesDto(
            flavorTextEntries = listOf(
                FlavorTextEntryDto(
                    flavorText = "Description of ${entry.name}.",
                    language = NamedResourceDto(name = "en"),
                ),
            ),
        )
    }
}

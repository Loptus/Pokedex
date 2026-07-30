package it.kata.pokedex.data.remote

import it.kata.pokedex.data.remote.dto.ArtworkDto
import it.kata.pokedex.data.remote.dto.FlavorTextEntryDto
import it.kata.pokedex.data.remote.dto.NamedResourceDto
import it.kata.pokedex.data.remote.dto.OtherSpritesDto
import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonIndexDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import it.kata.pokedex.data.remote.dto.SpritesDto
import it.kata.pokedex.data.remote.dto.TypeSlotDto
import kotlinx.coroutines.yield
import java.io.IOException

/**
 * Stands in for the network.
 *
 * A name of "broken" comes back without an id, which is how the tests exercise an entry that cannot
 * be read. The counters are there to prove the things that are otherwise invisible: [indexCalls]
 * that the index is fetched once, [detailCalls] and [speciesCalls] that turning a page fetches
 * nothing, and [detailAndSpeciesOverlapped] that a row's two requests do not queue behind each
 * other.
 */
class FakePokeApi : PokeApi {

    var allNames: List<String> = emptyList()
    var failIndexCall: Boolean = false
    var failingSpeciesIds: Set<Int> = emptySet()

    var indexCalls: Int = 0
        private set
    var detailCalls: Int = 0
        private set
    var speciesCalls: Int = 0
        private set

    private var detailInFlight = false
    private var speciesInFlight = false
    var detailAndSpeciesOverlapped: Boolean = false
        private set

    override suspend fun getPokemonIndex(limit: Int): PokemonIndexDto {
        indexCalls++
        if (failIndexCall) throw IOException("no network")

        return PokemonIndexDto(
            results = allNames.mapIndexed { index, name ->
                NamedResourceDto(name = name, url = "https://pokeapi.co/api/v2/pokemon/${index + 1}/")
            },
        )
    }

    override suspend fun getPokemonDetail(name: String): PokemonDetailDto {
        detailCalls++
        detailInFlight = true
        // Gives the sibling request a chance to start, so the overlap is observable.
        yield()
        if (speciesInFlight) detailAndSpeciesOverlapped = true
        detailInFlight = false

        if (name == "broken") return PokemonDetailDto(id = null, name = name)

        return PokemonDetailDto(
            id = idOf(name),
            name = name,
            types = listOf(TypeSlotDto(NamedResourceDto(name = typeOf(name)))),
            sprites = SpritesDto(
                frontDefault = "front/$name.png",
                other = OtherSpritesDto(ArtworkDto("artwork/$name.png")),
            ),
        )
    }

    override suspend fun getPokemonSpecies(id: Int): PokemonSpeciesDto {
        speciesCalls++
        speciesInFlight = true
        yield()
        if (detailInFlight) detailAndSpeciesOverlapped = true
        speciesInFlight = false

        if (id in failingSpeciesIds) throw IOException("species unavailable")

        return PokemonSpeciesDto(
            flavorTextEntries = listOf(
                FlavorTextEntryDto(
                    flavorText = "Description of ${nameOf(id)}.",
                    language = NamedResourceDto(name = "en"),
                ),
            ),
        )
    }

    private fun idOf(name: String) = allNames.indexOf(name) + 1

    private fun nameOf(id: Int) = allNames.getOrElse(id - 1) { "unknown" }

    private fun typeOf(name: String) = if (name == "bulbasaur") "grass" else "fire"
}

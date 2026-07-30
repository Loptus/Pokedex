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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Stands in for the network.
 *
 * A name of "broken" comes back without an id, which is how the tests exercise an entry that cannot
 * be mapped. [maxConcurrentDetailCalls] records how many detail calls were in flight at once, which
 * is the only way to tell a parallel load from a sequential one, and [indexCalls] proves the index
 * is fetched once and not once per page.
 */
class FakePokeApi : PokeApi {

    var allNames: List<String> = emptyList()
    var failIndexCall: Boolean = false
    var failingSpeciesIds: Set<Int> = emptySet()

    var indexCalls: Int = 0
        private set

    private val detailCallsInFlight = AtomicInteger()
    var maxConcurrentDetailCalls: Int = 0
        private set

    override suspend fun getPokemonIndex(limit: Int): PokemonIndexDto {
        indexCalls++
        if (failIndexCall) throw IOException("no network")

        return PokemonIndexDto(
            results = allNames.map { NamedResourceDto(name = it, url = "url/$it") },
        )
    }

    override suspend fun getPokemonDetail(name: String): PokemonDetailDto {
        val inFlight = detailCallsInFlight.incrementAndGet()
        maxConcurrentDetailCalls = maxOf(maxConcurrentDetailCalls, inFlight)
        // Gives the other coroutines a chance to start, so concurrency is observable.
        yield()
        detailCallsInFlight.decrementAndGet()

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

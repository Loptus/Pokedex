package it.kata.pokedex.data.remote

import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.dto.PokemonIndexDto
import it.kata.pokedex.data.remote.dto.PokemonSpeciesDto
import it.kata.pokedex.data.remote.dto.TypeDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Only the index and the types have a path of their own. Everything else takes a [Url] the API
 * handed back, because the ids of alternate forms do not line up with the ids of their species, so
 * an address built here would be a guess.
 */
interface PokeApi {

    @GET("pokemon")
    suspend fun getPokemonIndex(
        @Query("limit") limit: Int,
    ): PokemonIndexDto

    /** The url comes from an index entry. */
    @GET
    suspend fun getPokemonDetail(
        @Url url: String,
    ): PokemonDetailDto

    /** The url comes from the `species` object of a detail. */
    @GET
    suspend fun getPokemonSpecies(
        @Url url: String,
    ): PokemonSpeciesDto

    /**
     * Addressed by name, which we already own. By id it would hit the same mismatch: `unknown` and
     * `shadow` sit at ids 10001 and 10002.
     */
    @GET("type/{name}")
    suspend fun getType(
        @Path("name") name: String,
    ): TypeDto
}
